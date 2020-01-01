package microhazle.building.api;

import microhazle.channels.abstrcation.hazelcast.IMessage;
;import java.util.function.Consumer;

public interface IClientRoutingGateway {
    <T extends IMessage> IClientProducer<T> getChannel(Class<T> channel, Consumer<IClientProducer<T>> cosnumer);
}
