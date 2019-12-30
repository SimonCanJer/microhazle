package microhazle.channels.concrete.hazelcast;

import com.hazelcast.map.listener.MapListener;
import microhazle.channels.abstrcation.hazelcast.*;
import com.hazelcast.config.*;
import com.hazelcast.core.*;
import microhazle.channels.abstrcation.hazelcast.Error;
import microhazle.channels.abstrcation.hazelcast.admin.IAdmin;
import microhazle.channels.abstrcation.hazelcast.admin.MonitoredQ;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.rmi.UnexpectedException;
import java.rmi.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class HazelcastChannelProvider implements IGateWayServiceProvider {
    ITopic<String> pubCommand;
    static final String REPORT_START="report.start";
    static final String REPORT_END="report.end";

    private static final String REQUEST_FAMILY_MAP = "RequestFamilyMap";
    private static final String COMMON_FAMILY_ENTRY = "CommonFamilyEntries";
    private static final String REGISTERED_QS = "distributedProcessing.RegisteredQueues";
    private static final String Q_MONITOING_MAP = "distributedProcessing.Q_Monitoring";
    private IMap<String,MonitoredQ> mapMonitoredQueues;
    private Exception mThrown = null;
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
    volatile BiConsumer<String, MonitoredQ> monitoredEvent;
    private List<PoolingThreads> threads = new ArrayList<>();

    private String mStrPrivateReplyQueueID = UUID.randomUUID().toString().replace("-",".");
    ScheduledExecutorService executor= Executors.newScheduledThreadPool(1);
     private class DestinationQ<T extends ITransport> implements IProducerChannel<T> {
        IQueue<DTOMessageTransport<T>> q;
        Consumer<IProducerChannel<T>> notifier;

        void setQ(IQueue<DTOMessageTransport<T>> q) {
            this.q = q;
            if (notifier != null) {
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
            else
                throw new UnknownHostException(message.getHeader().getDestination());

        }

         @Override
         public <R extends IReply> Mono<R> post(DTOMessageTransport<T> message) throws UnknownHostException {
            Subscriber<? super R> [] imported = new Subscriber[1];
            boolean [] ignore=new boolean[]{false};
            Publisher<IReply> p= new Publisher<IReply>() {

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
             Mono<R> mono=(Mono<R> )Mono.from(p);
             post(message,( r)->{if(ignore[0]) return;if(
                     r.getData() instanceof Error) imported[0].onError((Error)r.getData());imported[0].onNext((R)r.getData());});
             return mono;
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

                IQueue q = mHazelcast.getQueue(itemEvent.getItem());
                DestinationQ dest = mDestinationQueues.computeIfAbsent(strClass, (String k) -> new DestinationQ<>());
                dest.setQ(q);
            } catch (Exception e) {
                e.printStackTrace();
            }
      }

        @Override
        public void itemRemoved(ItemEvent<String> itemEvent) {

        }
    }
    private static class EndOfQPooling<T extends ITransport> extends DTOMessageTransport<T>
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

    void configCommandTopic()
    {
        TopicConfig topicConfig= new TopicConfig();
        topicConfig.setName("commands.topic");

    }

    void createCommandTopic()
    {
       pubCommand=mHazelcast.getTopic("commands.topic");
       pubCommand.addMessageListener(this::commands);

    }
    void configMonitoredQueues()
    {
        MapConfig cfg = new MapConfig();
        cfg.setName(Q_MONITOING_MAP);
        cfg.setTimeToLiveSeconds(20);
        mConfig.addMapConfig(cfg);

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
    EntryListenerMonitored monitoredQListener = new EntryListenerMonitored();

    void activateMonitor()
    {
        mapMonitoredQueues=mHazelcast.getMap(Q_MONITOING_MAP);
        mapMonitoredQueues.addEntryListener(monitoredQListener,true);

    }



    private IRouter mRouter = new IRouter() {

        @SuppressWarnings("all")
        @Override
        public <T extends ITransport> IProducerChannel<T> getChannel(Class<T> router, Consumer<IProducerChannel<T>> readyEvent) {
            DestinationQ<T> dq = (DestinationQ<T>) mDestinationQueues.computeIfAbsent(router.getName(), (String s) -> {
                return new DestinationQ<T>();
            });
            dq.notifier=readyEvent;
            if (dq.q == null && mSetQueues.contains(router.getName())) {
                try {
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
        try {
            mConfig = new ClasspathXmlConfig("hazelcast.xml");

        } catch (Exception e) {
            mThrown = e;
            try {
                mConfig = new ClasspathXmlConfig("hazelcast.xml");

            } catch (Exception e1) {
                mThrown = e1;
            }

        }
        mConfig= new Config();
        mConfig.setInstanceName(UUID.randomUUID().toString());
        mConfig.getGroupConfig().setName(service);
        mConfig.getGroupConfig().setPassword("service");
        mConfig.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(true).setMulticastGroup("224.3.2.1");
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

    @Override
    public void shutdown() {

        for(PoolingThreads pt:threads)
        {
            pt.end();
        }
        mHazelcast.shutdown();

    }

    @Override
    public Set<String> getConsumedQueueNames() {
        return Collections.unmodifiableSet(mConsumedQueus.keySet());
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

    }

    static class PoolingThreads {
        IQueue<DTOMessageTransport<? extends ITransport>> mQ;
        IMessageConsumer consumer = null;
        final private Thread[] mThreads;
        CountDownLatch latch;
        PoolingThreads(IMessageConsumer consumer, String strName, IQueue<DTOMessageTransport<? extends ITransport>> q, int threads) {
            this.consumer = consumer;
            latch= new CountDownLatch(threads);
            mQ = q;
            this.mThreads = new Thread[threads];
            for (int i = 0; i < threads; i++) {
                mThreads[i] = new Thread(this::execute);
                mThreads[i].start();
            }
        }

        private void execute() {
            while (true) {
                try {
                    DTOMessageTransport<? extends ITransport> take=mQ.take();
                    if(take==null)
                        continue;
                    System.out.println(take);
                    if(take instanceof EndOfQPooling) {
                        System.out.println("breaking");
                        break;
                    }
                    consumer.handle(take);
                } catch (Throwable e) {
                    e.printStackTrace();
                    break;
                }
            }
            latch.countDown();
        }

        void end() {
            for (Thread t: mThreads) {
                mQ.add(new EndOfQPooling<>());
            }
           try {
                latch.await(10000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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



    private void createConsumesQueues() {
        mConfigQs.entrySet().forEach(this::createQueueByConfig);
        createReplyQ();


    }

    private void createQueueByConfig(Map.Entry<String, QueueConfig> qc) {
        mConsumedQueus.put(qc.getKey(), mHazelcast.getQueue(qc.getKey()));
        mSetQueues.add(qc.getKey());

    }
    private void createReplyQ()
    {
        mConsumedQueus.put(mStrPrivateReplyQueueID,mHazelcast.getQueue(mStrPrivateReplyQueueID));
        mSetQueues.add(mStrPrivateReplyQueueID);

    }

    private void restart() {
        mHazelcast = Hazelcast.newHazelcastInstance(mConfig);

        createListensAvalableQueues();
        createConsumesQueues();
         activateMonitor();
        createCommandTopic();
        initConsumerPoolExecutors();

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
    Runnable monitoringRun = new Runnable() {
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
    void startReporting()
    {
        if(mConsumedQueus.size()>0)
        {
            executor.schedule(monitoringRun,2,TimeUnit.SECONDS);
        }
    }
    void endRecoring()
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
