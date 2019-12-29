package microhazle.channels.abstrcation.hazelcast;

import java.util.function.Consumer;

/**
 * The interface define functionality to  obtain a channel to post request
 * and reply
 * @see IProducerChannel
 */
public interface IRouter {
    /**
     *
     * @param routed class of message to route
     * @param readyHandler  get called when channel is ready for transport
     * @param <T> type of request class
     * @return   channel
     */
    <T extends ITransport> IProducerChannel<T> getChannel(Class<T> routed, Consumer<IProducerChannel<T>> readyHandler);

    /**
     * sends reply to a message
     * @param transport
     */
    void reply(DTOReply<? extends IReply> transport);

 }
