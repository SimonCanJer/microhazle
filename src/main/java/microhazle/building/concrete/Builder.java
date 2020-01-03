package microhazle.building.concrete;

import microhazle.building.api.IBuild;
import microhazle.building.api.IClientProducer;
import microhazle.building.api.IClientRoutingGateway;
import microhazle.building.api.IMounter;
import microhazle.channels.abstrcation.hazelcast.*;
import microhazle.channels.concrete.hazelcast.HazelcastChannelProvider;
import microhazle.processors.api.AbstractProcessor;
import microhazle.processors.impl.containers.ConsumingContainer;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.*;
import java.rmi.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


public class Builder implements IBuild {


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
        public <T extends IMessage> void addProcessor(AbstractProcessor<T> p) {
            container.addProcessor(p);

        }

        @Override
        public <T extends IMessage> void addRequestClass(Class<T> cl) {
            setAnnouncedRequests.add(cl);

        }

         class ClientRoutingGateway  implements IClientRoutingGateway
        {
            @Override
            public <T extends IMessage> IClientProducer<T> getChannel(Class<T> channel, Consumer<IClientProducer<T>> consumer) {
                Consumer<IClientProducer<T>> localConsumer= consumer;
                IProducerChannel<T>[] producer=new IProducerChannel[1];
                IClientProducer[] res= new IClientProducer[1];

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
                return res[0];
            }

        }
        ClientRoutingGateway gateway= new ClientRoutingGateway();


        @Override
        public IClientRoutingGateway mountAndStart(Consumer<IClientRoutingGateway> onReady) {
           notifyReady = onReady;
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
            channelProvider.hold();

        }

        private <T extends ITransport> void testAvaiableChannel(Class c)
        {
            try {
                proxy.getChannel(c, (IProducerChannel<T> connected) -> {
                    int ready = satisfied.incrementAndGet();
                    if (isReady() && notifyReady != null) notifyReady.accept(gateway);
                });
            }
            catch (Throwable t)
            {

            }

        }
    }


}
