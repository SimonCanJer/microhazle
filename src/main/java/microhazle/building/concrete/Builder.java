package microhazle.building.concrete;

import microhazle.building.api.*;
import microhazle.channels.abstrcation.hazelcast.*;
import microhazle.channels.concrete.hazelcast.HazelcastChannelProvider;
import microhazle.processors.api.AbstractProcessor;
import microhazle.impl.containers.ConsumingContainer;
import org.apache.log4j.Logger;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.*;
import java.rmi.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


public class Builder implements IBuild {

   Logger logger= Logger.getLogger(this.getClass());
    @Override
    public IMounter forApplication(String name) {
        return new Mounter(name);
    }

    static class ProxyOf<T> implements InvocationHandler
    {
         private T stub;
         private T proxy;


        ProxyOf(Class<T> clazz) {
           proxy= (T) Proxy.newProxyInstance(this.getClass().getClassLoader(),new Class[]{clazz},this);
        }
        void setStub(T stub)
        {
            this.stub = stub;

        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return method.invoke(stub, method,args);
        }
        T getProxy()
        {
            return proxy;
        }
    }
    static class ProxyRouter implements IRouter
    {
        public void setStub(IRouter stub) {
            this.stub = stub;
        }

        IRouter stub;

        @Override
        public <T extends ITransport> IProducerChannel<T> getChannel(Class<T> routed, Consumer<IProducerChannel<T>> readyHandler) {
            if(stub==null)
                return null;
            return stub.getChannel(routed,readyHandler);
        }

        @Override
        public void reply(DTOReply<? extends IReply> transport) {
            if(stub==null)
                return ;
            stub.reply(transport);

        }
    }
    class Mounter implements IMounter
    {
        private Consumer<IClientRoutingGateway> notifyReady;
        private final String appName;
        boolean bConnected = false;
        //ProxyOf<IRouter> proxy=new ProxyOf<>(IRouter.class);
        ProxyRouter proxy= new ProxyRouter();
        ConsumingContainer container= new ConsumingContainer(proxy);
        HazelcastChannelProvider channelProvider = new HazelcastChannelProvider();
        Set<Class> setAnnouncedRequests= new HashSet<>();
        AtomicInteger satisfied= new AtomicInteger(0);
        Mounter(String appName) {
            this.appName = appName;
        }

        @Override
        public <T extends IMessage, S extends Serializable> void addProcessor(AbstractProcessor<T,S> p) {
            logger.trace("adding processor "+p);
            container.addProcessor(p);

        }

        @Override
        public <T extends IMessage> void addRequestClass(Class<T> cl) {
            logger.trace("adding request class "+cl);
            setAnnouncedRequests.add(cl);

        }

         class ClientRoutingGateway  implements IClientRoutingGateway
        {
            @Override
            public <T extends IMessage> IClientProducer<T> getChannel(Class<T> channel, Consumer<IClientProducer<T>> consumer) {
                logger.trace("channel requested for the message class "+channel);
                logger.trace("Verify that mountAndStart called : this is the correct practice");
                Consumer<IClientProducer<T>> localConsumer= consumer;
                IProducerChannel<T>[] producer=new IProducerChannel[1];
                IClientProducer[] res= new IClientProducer[1];
                logger.trace("creating IClientProducer interface for"+channel);
                res[0]= new IClientProducer<T>() {
                    @Override
                    public boolean isConnected() {
                        return producer[0].isConnected();
                    }

                    @Override
                    public <Response extends IReply> String post(T obj, Consumer<DTOReply<Response>> listener) throws UnknownHostException {
                        DTOMessage<T> dto= new DTOMessage<T>(obj);
                        return producer[0].post(dto,listener);
                    }

                    @Override
                    public <R extends IReply> Mono<R> post(T message) throws UnknownHostException {
                        DTOMessage<T> dto= new DTOMessage<T>(message);
                        return producer[0].post(dto);
                    }

                    @Override
                    public <R extends IReply> Future<R> send(T message) throws UnknownHostException {
                        DTOMessage<T> dto= new DTOMessage<T>(message);
                        return producer[0].send(dto);
                    }
                };
                producer[0]=proxy.getChannel(channel,(r)->{if(localConsumer!=null) localConsumer.accept(res[0]);});
                logger.trace("producer channel created  for"+channel+" : "+producer[0]);
                return res[0];
            }



        }
        ClientRoutingGateway gateway= new ClientRoutingGateway();


        @Override
        public IClientRoutingGateway mountAndStart(Consumer<IClientRoutingGateway> onReady) {
           notifyReady = onReady;
           logger.trace("Gateway: mountAndStart");
           proxy.setStub(channelProvider.initServiceAndRouting(appName,container));
           setAnnouncedRequests.stream().forEach(this::testAvaiableChannel);
           return gateway;

        }

        @Override
        public boolean isReady() {
            return satisfied.get()==setAnnouncedRequests.size();
        }

        @Override
        public void destroy() {
            channelProvider.shutdown();
        }

        @Override
        public void holdServer() {
            logger.trace("Hold server request");
            channelProvider.hold();

        }

        @Override
        public IAServicePopulator endPointPopulator() {
            return new NwPopulator(channelProvider,appName);
        }

        private <T extends ITransport> void testAvaiableChannel(Class c)
        {
            logger.trace("testAvaiableChannel(Class "+c+")");
            try {
                proxy.getChannel(c, (IProducerChannel<T> connected) -> {
                    int ready = satisfied.incrementAndGet();
                    logger.trace("channel ready for "+c);
                    if (isReady() && notifyReady != null)
                    {
                        logger.trace("the whole router is ready, notifying");
                        notifyReady.accept(gateway);
                    }
                });
            }
            catch (Throwable t)
            {

            }

        }
    }


}
