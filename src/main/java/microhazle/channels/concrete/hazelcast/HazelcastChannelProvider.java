package microhazle.channels.concrete.hazelcast;

import microhazle.building.api.CustomEndPoint;
import microhazle.channels.IEndPointPopulator;
import microhazle.channels.abstrcation.hazelcast.*;
import com.hazelcast.config.*;
import com.hazelcast.core.*;
import microhazle.channels.abstrcation.hazelcast.Error;
import microhazle.channels.abstrcation.hazelcast.admin.IAdmin;
import microhazle.channels.abstrcation.hazelcast.admin.MonitoredQ;
import org.apache.log4j.Logger;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Mono;

import java.rmi.UnexpectedException;
import java.rmi.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * The class implements  IGateWayServiceProvider interface on the top Hazelcast framework.
 * The implemented mechanism  collects set of requests are consumed by local handlers(processors)
 * which are listening on the backend of
 * @see IMessageConsumer
 * interface which the request transmitters.
 * Each one  of the consumed  message classes is associated with a queue (IQueue of Hazelcast), which are configured and created
 * in the listening instance
 * @see #initServiceAndRouting(String, IMessageConsumer)
 * @see #createConsumedQueues()
 * The created queues bloking ones, and are polled by threads for incoming messages
 * At the same time the queues are subject of get operation fro message senders which use
 * the IRouter, which the class implemenbts and returns upon initialized
 * @see IRouter interface.
 * A sender has to use the IRouter interface in order to get IProducerChannel
 * (@see  IProducerChannel) interface for sending messages of
 * class T (T extends IMessage); It is implemented by obtaining an instance of an existing queue, which has been created
 * on backend fro a processor (if any). If it was not create still, then anyway a not connected instance will be returned
 * and client should white for notification about the queue created by any consumed instance.
 *
 *
 */
public class HazelcastChannelProvider implements IGateWayServiceProvider {
    ITopic<String> pubCommand;
    private static final String REPORT_START="report.start";
    private static final String REPORT_END="report.end";
    Logger logger = Logger.getLogger(this.getClass());

    private static final String REQUEST_FAMILY_MAP = "RequestFamilyMap";
    private static final String COMMON_FAMILY_ENTRY = "CommonFamilyEntries";
    private static final String REGISTERED_QS = "distributedProcessing.RegisteredQueues";
    private static final String Q_MONITOING_MAP = "distributedProcessing.Q_Monitoring";
    private IMap<String,MonitoredQ> mapMonitoredQueues;
    private HazelcastInstance mHazelcast;
    static private Config mConfig;
    private ISet<String> mSetQueues;
    private ItemListener<String> mListenQsSet= new QsRegisterListener();
    private Map<String, QueueConfig> mConfigQs = new HashMap<>();
    private Map<String, IQueue<DTOMessageTransport<? extends ITransport>>> mConsumedQueus = new HashMap<>();
    private Map<String, DestinationQ<? extends ITransport>> mDestinationQueues = new HashMap<>();
    private Map<String, Consumer> mapPendingListeners= new ConcurrentHashMap<>();
    private WrappingConsumer wrapper = new WrappingConsumer();
    IMessageConsumer mConsumer;
    private volatile BiConsumer<String, MonitoredQ> monitoredEvent;
    private List<PoolingThreads> threads = new ArrayList<>();
    private EntryListenerMonitored monitoredQListener = new EntryListenerMonitored();
    private String mStrPrivateReplyQueueID = UUID.randomUUID().toString().replace("-",".");
    private ScheduledExecutorService executor= Executors.newScheduledThreadPool(1);
    Set<SetConfig> setEndPointsConfig=new HashSet<>();
    Map<String,CustomEndPoint> mapEndPoints= new ConcurrentHashMap<String,CustomEndPoint>();
    /**
     * names of all populated end point instances
     */
    ISet<String > setAllPopulatedEPInNetwork;
    private Runnable monitoringRun = new Runnable() {
        @Override
        public void run() {
            for(Map.Entry<String, IQueue<DTOMessageTransport<? extends ITransport>>> entry:mConsumedQueus.entrySet()) {
                MonitoredQ q = mapMonitoredQueues.computeIfAbsent(entry.getKey(), (String s) -> {
                    return new MonitoredQ(mStrPrivateReplyQueueID,0);
                });
                q.setNumberInQ(entry.getValue().size());
                mapMonitoredQueues.put(entry.getKey(),q);

            }
        }
    };
    public void hold() {
        threads.stream().forEach(p->p.join());
    }

    private class DestinationQ<T extends ITransport> implements IProducerChannel<T> {
        IQueue<DTOMessageTransport<T>> q;
        Consumer<IProducerChannel<T>> notifier;
        void setQ(IQueue<DTOMessageTransport<T>> q) {
            this.q = q;
            if (notifier != null) {
                logger.trace("destination queue is connected now tp channel "+q.getName());
                notifier.accept(this);
            }
        }

         @Override
         public boolean isConnected() {
             return q!=null;
         }

         @Override
        public  <R extends IReply> String post(DTOMessageTransport<T> message,Consumer<DTOReply<R>> listener) throws UnknownHostException {
             message.getHeader().setSource(mStrPrivateReplyQueueID);
            if (q != null) {
                if(listener!=null)
                    mapPendingListeners.put(message.getHeader().getId(),listener);

                q.add(message);
                if(q.size()>450)
                {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return message.getHeader().getId();
            }
            else {
                logger.error("Unknown host for message");
                throw new UnknownHostException(message.getHeader().getDestination());
            }

        }

         @Override
         public <R extends IReply> Mono<R> post(DTOMessageTransport<T> message) throws UnknownHostException {
            Subscriber<? super R> [] imported = new Subscriber[1];
            boolean [] ignore=new boolean[]{false};
            Publisher<? extends IReply> p= new Publisher<IReply>() {

                @Override
                public void subscribe(Subscriber<? super IReply> subscriber) {
                    imported[0]=subscriber;
                    imported[0].onSubscribe(new Subscription() {
                        @Override
                        public void request(long l) {

                        }

                        @Override
                        public void cancel() {
                            ignore[0]=true;

                        }
                    });
                }
            };
             Mono<? extends IReply> mono= Mono.from(p);
             post(message,( r)->{if(ignore[0]) return;if(
                     r.getData() instanceof Error) imported[0].onError((Error)r.getData());logger.trace("subscriber: onNext");imported[0].onNext((R)r.getData());});
             return (Mono<R>) mono;
         }

         @Override
         public <R extends IReply> Future<R> send(DTOMessageTransport<T> message) throws UnknownHostException {
             CompletableFuture<R> res= new CompletableFuture<>();
             post(message,(r)->{res.complete((R) r.getData());});
             return res;
         }


     }

    class QsRegisterListener implements ItemListener<String> {

         @SuppressWarnings("all")
        @Override
        public void itemAdded(ItemEvent<String> itemEvent) {
            String strClass = itemEvent.getItem();
            try {
                logger.trace("destination queue "+strClass+" became available");

                IQueue q = mHazelcast.getQueue(itemEvent.getItem());
                DestinationQ dest = mDestinationQueues.computeIfAbsent(strClass, (String k) -> new DestinationQ<>());
                logger.trace("registering dest "+strClass);
                dest.setQ(q);
            } catch (Exception e) {
                e.printStackTrace();
            }
      }

        @Override
        public void itemRemoved(ItemEvent<String> itemEvent) {

        }
    }
    static class EndOfQPooling<T extends ITransport> extends DTOMessageTransport<T>
    {

    }
     void commands(Message<String> message)
     {
         switch (message.getMessageObject())
         {
             case REPORT_START:
                 startReporting();
                 break;
             case REPORT_END:
                 endRecoring();
                 break;
         }
     }

    private void configCommandTopic()
    {
        TopicConfig topicConfig= new TopicConfig();
        topicConfig.setName("commands.topic");

    }

    private void createCommandTopic()
    {
       pubCommand=mHazelcast.getTopic("commands.topic");
       pubCommand.addMessageListener(this::commands);

    }
    private void configMonitoredQueues()
    {
        MapConfig cfg = new MapConfig();
        cfg.setName(Q_MONITOING_MAP);
        cfg.setTimeToLiveSeconds(20);
        mConfig.addMapConfig(cfg);

    }
    private static final String SET_POPULATED_END_POINTS ="populated.endPoints.names";
    void configPopulatedEndPoints()

    {
        SetConfig config= new SetConfig();
        config.setName(SET_POPULATED_END_POINTS);
        config.setMaxSize(300);
        mConfig.addSetConfig(config);
    }

    class EntryListenerMonitored implements EntryListener<String, MonitoredQ>{

        @Override
        public void entryAdded(EntryEvent<String, MonitoredQ> e) {
            notifyAdded(e);
        }

        private void notifyAdded(EntryEvent<String, MonitoredQ> e) {
            if(monitoredEvent!=null)
            {
                try
                {
                    monitoredEvent.accept(e.getKey(),e.getValue());

                }
                catch(Throwable t)
                {

                }
            }
        }

        @Override
        public void entryEvicted(EntryEvent<String, MonitoredQ> entryEvent) {

        }

        @Override
        public void entryRemoved(EntryEvent<String, MonitoredQ> entryEvent) {

        }

        @Override
        public void entryUpdated(EntryEvent<String, MonitoredQ> entryEvent) {
            notifyAdded(entryEvent);

        }

        @Override
        public void mapCleared(MapEvent mapEvent) {

        }

        @Override
        public void mapEvicted(MapEvent mapEvent) {

        }
    }


    private void activateMonitor()
    {
        mapMonitoredQueues=mHazelcast.getMap(Q_MONITOING_MAP);
        mapMonitoredQueues.addEntryListener(monitoredQListener,true);

    }



    private IRouter mRouter = new IRouter() {

        @SuppressWarnings("all")
        @Override
        public <T extends ITransport> IProducerChannel<T> getChannel(Class<T> router, Consumer<IProducerChannel<T>> readyEvent) {
            logger.trace("low level router: getChannel "+router);
            DestinationQ<T> dq = (DestinationQ<T>) mDestinationQueues.computeIfAbsent(router.getName(), (String s) -> {
                return new DestinationQ<T>();
            });
            dq.notifier=readyEvent;
            if (dq.q == null && mSetQueues.contains(router.getName())) {
                try {
                    logger.trace(" discovered that destination Queue is available in grid, setting reference");
                    dq.setQ(mHazelcast.getQueue(router.getName()));
                } catch (Exception e) {
                    e.printStackTrace();

                }
            }
            else
            {
                if(dq.q!=null)
                {
                    if(readyEvent!=null)
                    {
                        logger.trace("destination queue is got previously, just notify about");
                        readyEvent.accept(dq);
                    }
                }
            }
            return dq;

        }

        @Override
        public void reply(DTOReply<? extends IReply> transport) {
            try {
                IQueue<DTOReply<? extends IReply>> q = mHazelcast.getQueue(transport.getHeader().getSource());
                System.out.println("sending reply");
                q.add(transport);

            }
            catch(Exception e)
            {
                e.printStackTrace();
            }

        }
    };


    @Override
    public IRouter initServiceAndRouting(String service, IMessageConsumer consumer) {
        if (service == null) {

            throw new RuntimeException(new UnexpectedException("Service is null"));
        }
        mConsumer = consumer;
        initHazelcastConfig(service);
        for (String name : new String[]{REQUEST_FAMILY_MAP, COMMON_FAMILY_ENTRY})
            configMap(name);
        configPrivateReplyQueue();
        configQsRegistrationSet();
        configMonitoredQueues();
        configCommandTopic();
        consumer.getHandledMessageClasses().stream().forEach(this::queueConfg);
        restart();

        return mRouter;
    }

    private void initHazelcastConfig(String service) {
        if(mConfig!=null)
            return ;
        try {
            if(mConfig!=null)
                mConfig = new ClasspathXmlConfig("hazelcast.xml");

        } catch (Exception e) {

           throw new RuntimeException("Failed initializing Hazelcast");

        }
        mConfig= new Config();
        mConfig.setInstanceName(UUID.randomUUID().toString());
        mConfig.getGroupConfig().setName(service);
        mConfig.getGroupConfig().setPassword("service");
        mConfig.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(true).setMulticastGroup("224.3.2.1");
    }

    @Override
    public void shutdown() {


        try {
            removeEndPointsPopulated();
            for (PoolingThreads pt : threads) {
                pt.end();
            }
            mHazelcast.shutdown();
        }
        catch(Exception e)
        {

        }
    }

    @Override
    public Set<String> getConsumedQueueNames() {
        return Collections.unmodifiableSet(mConsumedQueus.keySet());
    }
    String endPointInfoName(String name)
    {
        return "populatedEndPoints_"+name;
    }
    void queryEndPointAndAct(CustomEndPoint ep, BiFunction<CustomEndPoint,CustomEndPoint,Boolean> selector,BiConsumer<byte[],ISet<byte[]>> act)
    {
        String name= endPointInfoName(ep.getName());
        ISet<byte[]> set= mHazelcast.getSet(name);

        if(set==null)
            return ;
        for(byte[] record: set)
        {
            CustomEndPoint recorded=CustomEndPoint.gsonUnmarshal(record);
            if(selector!=null)
            {
                if(selector.apply(ep,recorded))
                {
                    if(act!=null)
                    {
                        act.accept(record,set);
                    }
                }
            }
        }
    }

    private Map<String,Runnable> mapPendingForPopulated= new ConcurrentHashMap<>();
    @Override
    public IEndPointPopulator endPointPopulator() {
        return new IEndPointPopulator() {



            @Override
            public void populate(CustomEndPoint ep, String serviceName) {
                initHazelcastConfig(serviceName);
              SetConfig cfg= new SetConfig();
              cfg.setName(endPointInfoName(ep.getName()));
              cfg.setMaxSize(100);
              setEndPointsConfig.add(cfg);
              mapEndPoints.put(endPointInfoName(ep.getName()),ep);
              mConfig.addSetConfig(cfg);
           }

            @Override
            public void revoke(String serviceName) {
                removePopulatedEndPoint(serviceName);
            }

            HashMap<String, ItemListener<byte[]>> itemListeners = new HashMap<>();


            @Override
            public void query(String endPoint, List<CustomEndPoint> collector, Consumer<List<CustomEndPoint>> listener) {
                String sName = endPointInfoName(endPoint);
                if(!setAllPopulatedEPInNetwork.contains(sName))
                {
                    mapPendingForPopulated.put(sName, new Runnable(){
                        @Override
                        public void run() {
                            collectAndListenPopulated(collector,listener,sName);

                        }
                    });
                    return ;

                }
                collectAndListenPopulated(collector, listener, sName);
            }

            @Override
            public void invalidate(CustomEndPoint ep) {
                ep.invalidate();
                queryEndPointAndAct(ep,(cep,rep)->{return cep.getUrl().equals(rep.getUrl())&&cep.getTimeStamp()>rep.getTimeStamp();},(record,set)->{set.remove(record);});
            }

            private void collectAndListenPopulated(List<CustomEndPoint> collector, Consumer<List<CustomEndPoint>> listener, String sName) {
                collectPopulated(collector, sName);
                if(listener!=null) {
                    synchronized (itemListeners)
                    {
                        boolean reg=false;
                        if(!itemListeners.containsKey(sName))
                            reg=true;
                        NameItemListener handler= (NameItemListener) itemListeners.computeIfAbsent(sName,(k)->{return new NameItemListener(listener,sName);});
                        ISet set= mHazelcast.getSet(sName);
                        if(set!=null)
                            set.addItemListener(handler,true);

                    }
                }
            }
            private void collectPopulated(List<CustomEndPoint> collector, String sName) {
                if(!setAllPopulatedEPInNetwork.contains(sName))
                {
                    return ;
                }
                ISet<byte[]> populated =mHazelcast.getSet(sName);
                for(byte[] bytes:populated)
                {
                    CustomEndPoint ep=CustomEndPoint.gsonUnmarshal(bytes);
                    collector.add(ep);
                }
            }

            class NameItemListener implements ItemListener<byte[]>
            {
                Consumer<List<CustomEndPoint>> listener;
                String name;
                NameItemListener(Consumer<List<CustomEndPoint>> consumer,String name)
                {
                    listener=consumer;
                    this.name=name;

                }
                void refresh()
                {
                    ArrayList<CustomEndPoint> collector = new ArrayList<>();
                    collectPopulated(collector,name);
                    try
                    {
                        listener.accept(collector);

                    }
                    catch(Throwable th)
                    {

                    }

                }
                @Override
                public void itemAdded(ItemEvent<byte[]> itemEvent) {
                    refresh();
                }

                @Override
                public void itemRemoved(ItemEvent<byte[]> itemEvent) {
                    refresh();
                }

            }
            private void removePopulatedEndPoint(String point)
            {
                String name= endPointInfoName(point);
                ISet<byte[]> set= mHazelcast.getSet(name);
                CustomEndPoint ep = mapEndPoints.get(name);
                if(ep==null)
                    return ;
                byte[] remove=null;

                if(set!=null)
                {
                    for(byte[] bytes:set)
                    {
                        CustomEndPoint epc= CustomEndPoint.gsonUnmarshal(bytes);
                        if(epc.getUiid().equals(ep.getUiid()))
                        {
                            remove=bytes;
                            break;
                        }

                    }
                    mapEndPoints.remove(name);
                    if(remove!=null)
                    {
                        set.remove(remove);
                    }

                }
            }

        };
    }




    private void configQsRegistrationSet()
    {
        SetConfig cfg = new SetConfig();
        cfg.setName(REGISTERED_QS);
        cfg.setMaxSize(200);
        mConfig.addSetConfig(cfg);
    }

    private void configPrivateReplyQueue() {
        QueueConfig queueConfig = new QueueConfig();
         queueConfig.setMaxSize(500);
        queueConfig.setName(mStrPrivateReplyQueueID);
        mConfig.addQueueConfig(queueConfig);
        logger.trace("private reply queue configured wiith id ="+mStrPrivateReplyQueueID);

    }

    /**
     * creates executive pool of consumers
     */
    private void initConsumerPoolExecutors() {
        if (mConsumedQueus.size() > 0) {
            int processors = Runtime.getRuntime().availableProcessors();
            int perp = processors / mConsumedQueus.size();
            if (perp == 0)
                perp = 1;
            int perproc = perp;
            mConsumedQueus.entrySet().forEach(e -> createExecPool(e, perproc));
        }
    }

    private void createListensAvalableQueues() {

        if (mSetQueues == null)
            mSetQueues = mHazelcast.getSet(REGISTERED_QS);
        mSetQueues.addItemListener(mListenQsSet, true);
        logger.trace("createListensAvalableQueues() : set of names of destination queues is created");

    }

    @SuppressWarnings("all")
    class WrappingConsumer implements IMessageConsumer
    {

        @Override
        public void handle(DTOMessageTransport<? extends ITransport> dto) {
            if(dto instanceof DTOReply)
            {
                if(mConsumer!=null)
                {
                    Consumer cons= mapPendingListeners.get(dto.getHeader().getId());
                    if(cons!=null)
                    {
                        mapPendingListeners.remove(dto.getHeader().getId());
                        cons.accept((DTOReply<? extends IReply>) dto);
                        return ;
                    }

                }

            }
            mConsumer.handle(dto);

        }


        @Override
        public Set<String> getHandledMessageClasses() {
            return mConsumer.getHandledMessageClasses();
        }
    }

    private void createExecPool(Map.Entry<String, IQueue<DTOMessageTransport<? extends ITransport>>> e, int perp) {
        threads.add(new PoolingThreads(wrapper, e.getKey(), e.getValue(), perp));
    }



    private void createConsumedQueues() {
        logger.trace("creating consumed queues: configured for consume: "+mConfigQs.size()+ " queues");
        mConfigQs.entrySet().forEach(this::createQueueByConfig);
        createReplyQ();


    }

    private void createQueueByConfig(Map.Entry<String, QueueConfig> qc) {
        mConsumedQueus.put(qc.getKey(), mHazelcast.getQueue(qc.getKey()));
        mSetQueues.add(qc.getKey());
        logger.trace("##Consumed queue created : "+qc.getKey());

    }
    private void createReplyQ()
    {
        mConsumedQueus.put(mStrPrivateReplyQueueID,mHazelcast.getQueue(mStrPrivateReplyQueueID));
        mSetQueues.add(mStrPrivateReplyQueueID);
        logger.trace("##Reply queue created : "+mStrPrivateReplyQueueID);

    }

    private void restart() {
        mHazelcast = Hazelcast.newHazelcastInstance(mConfig);
        createListensAvalableQueues();
        createConsumedQueues();
        populateEndPoints();
        activateMonitor();
        createCommandTopic();
        initConsumerPoolExecutors();
        IQueue q= mHazelcast.getQueue("11s");
        Runtime.getRuntime().addShutdownHook(new Thread(){
            public void run()
            {
                shutdown();
            }
        });

    }


    private void removeEndPointsPopulated() {

        for(Map.Entry<String,CustomEndPoint> e: mapEndPoints.entrySet())
        {

            ISet<byte[]> reg= mHazelcast.getSet(e.getKey());
            Set<byte[]> remove= new HashSet<>();
            for(byte[] bytes: reg) {
                CustomEndPoint ep = CustomEndPoint.gsonUnmarshal(bytes);
                if(ep.getUiid().equals(e.getValue().getUiid())) {

                    reg.remove(remove);
                    break;
                }
            }

        }

    }

    private void populateEndPoints() {
        setAllPopulatedEPInNetwork = mHazelcast.getSet(SET_POPULATED_END_POINTS);
        setAllPopulatedEPInNetwork.addItemListener(new ItemListener<String>() {
            void notifyPending(String s)
            {
                Runnable r= mapPendingForPopulated.get(s);
                if(r!=null)
                    r.run();
            }
            @Override
            public void itemAdded(ItemEvent<String> itemEvent) {

                notifyPending(itemEvent.getItem());
            }

            @Override
            public void itemRemoved(ItemEvent<String> itemEvent) {
                notifyPending(itemEvent.getItem());

            }
        },true);
        for(SetConfig cfg:setEndPointsConfig)
        {

            ISet<byte[]> info= mHazelcast.getSet(cfg.getName());
            CustomEndPoint cep=mapEndPoints.get(cfg.getName());
            queryEndPointAndAct(cep,(qep,rep)->{return qep.getUrl().equals(rep.getUrl());},(r,s)->{s.remove(r);});
            info.add(CustomEndPoint.gsonMarshal(cep));

            setAllPopulatedEPInNetwork.add(cfg.getName());

        }
    }


    private void configMap(String name) {
        MapConfig cfg = new MapConfig();
        cfg.setName(name);
        mConfig.addMapConfig(cfg);
    }

    private void queueConfg(String qClass) {
        QueueConfig queueConfig = new QueueConfig();
        queueConfig.setName(qClass);
        queueConfig.setMaxSize(500);
        mConfigQs.put(qClass, queueConfig);
        mConfig.addQueueConfig(queueConfig);

    }

    private void startReporting()
    {
        if(mConsumedQueus.size()>0)
        {
            executor.schedule(monitoringRun,2,TimeUnit.SECONDS);
        }
    }
    private void endRecoring()
    {
        executor.shutdown();
    }
    class Admin implements IAdmin{

        @Override
        public void postReportingRequest() {
            pubCommand.publish(REPORT_START);


        }

        @Override
        public void postStopReportingRequest() {
            pubCommand.publish(REPORT_END);

        }

        @Override
        public void setQueueListener(BiConsumer<String, MonitoredQ> listener) {
            monitoredEvent=listener;

        }
    }


}
