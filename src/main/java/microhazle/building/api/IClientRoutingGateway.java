package microhazle.building.api;

import microhazle.building.concrete.NwPopulator;
import microhazle.channels.IEndPointPopulator;
import microhazle.channels.abstrcation.hazelcast.IMessage;
;import java.util.function.Consumer;

/**
 * just client friendly wrapper for
 * @see microhazle.channels.abstrcation.hazelcast.IRouter
 */
public interface IClientRoutingGateway {

    <T extends IMessage> IClientProducer<T> getChannel(Class<T> channel, Consumer<IClientProducer<T>> cosnumer);

}
