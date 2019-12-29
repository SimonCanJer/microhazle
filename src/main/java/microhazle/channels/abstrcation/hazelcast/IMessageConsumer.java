package microhazle.channels.abstrcation.hazelcast;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * interface supplies functionality to
 * declare handled classes (needed to advertise related pear in network) and
 * handle incoming messages
 */
public interface IMessageConsumer {
    void handle(DTOMessageTransport<? extends ITransport> dto);
    Set<String> getHandledMessageClasses();

}
