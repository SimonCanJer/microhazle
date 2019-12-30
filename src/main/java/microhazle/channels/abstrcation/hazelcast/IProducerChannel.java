package microhazle.channels.abstrcation.hazelcast;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.rmi.UnknownHostException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public interface IProducerChannel<T extends ITransport> {
    boolean isConnected();
    <Response extends IReply>  String  post(DTOMessageTransport<T> message,Consumer<DTOReply<Response>> listener) throws UnknownHostException;
     <R extends IReply> Mono<R> post(DTOMessageTransport<T> message) throws UnknownHostException;
     <R extends IReply> Future<R> send(DTOMessageTransport<T> message) throws UnknownHostException;
}
