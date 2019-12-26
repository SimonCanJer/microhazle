package microhazle.channels.abstrcation.hazelcast;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public interface IGateWayServiceProvider {
    IRouter initServiceAndRouting(String service, IMessageConsumer consumer);
    void shutdown();
    Set<String> getConsumedQueueNames();
}
