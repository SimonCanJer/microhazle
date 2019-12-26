package microhazle.building.api;

import microhazle.channels.abstrcation.hazelcast.IMessage;
import microhazle.channels.abstrcation.hazelcast.IRouter;
import microhazle.processors.api.AbstractProcessor;

import java.util.function.Consumer;

public interface IMounter {
    <T extends IMessage> void addProcessor(AbstractProcessor<T> p);
    <T extends IMessage> void addRequestClass(Class<T> cl);
    IRouter mountAndStart(Consumer<IRouter> ready);
    boolean isReady();

}
