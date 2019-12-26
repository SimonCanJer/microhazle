package microhazle.channels.abstrcation.hazelcast;

import java.util.function.Consumer;

public interface IRouter {
    <T extends ITransport> IProducerChannel<T> getChannel(Class<T> router, Consumer<IProducerChannel<T>> readyHandler);
    void reply(DTOReply<? extends IReply> transport);
 }
