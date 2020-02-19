package microhazle.channels.abstrcation.hazelcast;

import java.io.Serializable;
import java.util.function.Consumer;
import java.util.function.Function;

public interface IJobContext<P extends ITransport,S extends Serializable> {

    P getInitialData();

    S getState();
    <T extends ITransport> T getReactedRequest();
    void mutateState(Function<S,S> mutator,S triggered, Runnable trigger);


}
