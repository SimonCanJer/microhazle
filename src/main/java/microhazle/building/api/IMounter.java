package microhazle.building.api;

import microhazle.channels.abstrcation.hazelcast.IMessage;
import microhazle.channels.abstrcation.hazelcast.IRouter;
import microhazle.processors.api.AbstractProcessor;

import java.util.function.Consumer;

/**
 * mounts and initialized messaging and processing
 */
public interface IMounter {
    <T extends IMessage> void addProcessor(AbstractProcessor<T> p);
    <T extends IMessage> void addRequestClass(Class<T> cl);

    /**
     *
     * @param ready
     * @return
     */
    IRouter mountAndStart(Consumer<IRouter> ready);
    boolean isReady();

}