package microhazle.channels.abstrcation.hazelcast;

import java.rmi.UnknownHostException;
import java.util.function.Consumer;

public interface IProducerChannel<T extends ITransport> {
    boolean isConnected();
    <Response extends IReply>  String  post(DTOMessageTransport<T> message,Consumer<DTOReply<Response>> listener) throws UnknownHostException;

}
