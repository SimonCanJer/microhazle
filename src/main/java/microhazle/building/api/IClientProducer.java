package microhazle.building.api;

import microhazle.channels.abstrcation.hazelcast.*;
import reactor.core.publisher.Mono;

import java.rmi.UnknownHostException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * just whapper for the
 * @see IClientProducer interface
 * The interface is exported as API because of hides behind the scene generation of a transport object
 * @param <T>
 */
public interface IClientProducer<T extends IMessage> {
    boolean isConnected();
    <Response extends IReply>  String  post(T obj, Consumer<DTOReply<Response>> listener) throws UnknownHostException;
    <R extends IReply> Mono<R> post(T message) throws UnknownHostException;
    <R extends IReply> Future<R> send(T message) throws UnknownHostException;
}
