package microhazle.channels.abstrcation.hazelcast;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.rmi.UnknownHostException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * The interface declares functionality to send messages to destination
 * @param <T>
 */
public interface IProducerChannel<T extends ITransport> {
    /**
     * Verifies , whether any consumed queue created on back end and underlaying instance
     * of implementer is connected to then q
     * @return is connected
     */
    boolean isConnected();

    /**
     * send message and listen to a resulty
     *
     * @param message a message
     * @param listener a listener to be called upon result returned
     * @param <Response>
     * @return Id of the message sent
     * @throws UnknownHostException
     */
    <Response extends IReply>  String  post(DTOMessageTransport<T> message,Consumer<DTOReply<Response>> listener) throws UnknownHostException;

    /**
     * like previous, but uses teh Mono mechsnism
      * @param message a message to send
     * @param <R> type of message (not DTO)
     * @return Mono object
     * @throws UnknownHostException
     */
    <R extends IReply> Mono<R> post(DTOMessageTransport<T> message) throws UnknownHostException;
     <R extends IReply> Future<R> send(DTOMessageTransport<T> message) throws UnknownHostException;
}
