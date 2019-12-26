package microhazle.building.concrete;

import microhazle.building.api.IBuild;
import microhazle.building.api.IMounter;
import microhazle.channels.abstrcation.hazelcast.IMessage;
import microhazle.channels.abstrcation.hazelcast.IProducerChannel;
import microhazle.channels.abstrcation.hazelcast.IRouter;
import microhazle.channels.abstrcation.hazelcast.ITransport;
import microhazle.channels.concrete.hazelcast.HazelcastChannelProvider;
import microhazle.processors.api.AbstractProcessor;
import microhazle.processors.impl.containers.ConsumingContainer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
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
            return method.invoke(stub, method);
        }
        T getProxy()
        {
            return proxy;
        }
    }
    class Mounter implements IMounter
    {
        private Consumer<IRouter> notifyReady;
        private final String appName;
        boolean bConnected = false;
        ProxyOf<IRouter> proxy=new ProxyOf<>(IRouter.class);
        ConsumingContainer container= new ConsumingContainer(proxy.proxy);
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


        @Override
        public IRouter mountAndStart(Consumer<IRouter> onReady) {
           notifyReady = onReady;
           proxy.setStub(channelProvider.initServiceAndRouting(appName,container));
           setAnnouncedRequests.stream().forEach(this::testAvaiableChannel);
           return proxy.stub;

        }

        @Override
        public boolean isReady() {
            return satisfied.get()==setAnnouncedRequests.size();
        }

        private <T extends ITransport> void testAvaiableChannel(Class c)
        {
           proxy.stub.getChannel(c,(IProducerChannel<T> connected)->{int ready=satisfied.incrementAndGet(); if(isReady()&& notifyReady!=null) notifyReady.accept(proxy.proxy);});

        }
    }


}
