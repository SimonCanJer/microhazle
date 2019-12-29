package microhazle.channels.abstrcation.hazelcast;

import reactor.core.publisher.Flux;

import java.rmi.UnknownHostException;
import java.util.function.Consumer;

public interface IProducerChannel<T extends ITransport> {
    boolean isConnected();
    <Response extends IReply>  String  post(DTOMessageTransport<T> message,Consumer<DTOReply<Response>> listener) throws UnknownHostException;
     <R extends IReply> Flux<R> post(DTOMessageTransport<T> message) throws UnknownHostException;
}
