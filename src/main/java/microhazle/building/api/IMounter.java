package microhazle.building.api;

import microhazle.building.concrete.NwPopulator;
import microhazle.channels.abstrcation.hazelcast.IMessage;
import microhazle.channels.abstrcation.hazelcast.IRouter;
import microhazle.processors.api.AbstractProcessor;

import java.io.Serializable;
import java.util.function.Consumer;

/**
 * mounts and initialized messaging and processing
 */
public interface IMounter {
    <T extends IMessage, S extends Serializable> void addProcessor(AbstractProcessor<T,S> p);
    <T extends IMessage> void addRequestClass(Class<T> cl);

    /**
     *
     * @param ready
     * @return
     */
    IClientRoutingGateway mountAndStart(Consumer<IClientRoutingGateway> ready);
    boolean isReady();
    void destroy();
    void holdServer();
    IAServicePopulator endPointPopulator();

}
