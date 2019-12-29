package microhazle.channels.abstrcation.hazelcast;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 *  Main interface providing functionality
 *  of IPC
 */
public interface IGateWayServiceProvider {
    /**
     * initializes all the communication center
     * @param service    name of populated service
     * @param consumer   consumer interface
     * @return IRouter interface to send messages
     * @see IRouter
     * @see IMessageConsumer
     */
    IRouter initServiceAndRouting(String service, IMessageConsumer consumer);

    /**
     * shutdown services
     */
    void shutdown();
    Set<String> getConsumedQueueNames();
}
