package microhazle.channels.abstrcation.hazelcast;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public interface IMessageConsumer {
    void handle(DTOMessageTransport<? extends ITransport> dto);
    Set<String> getHandledMessageClasses();

}
