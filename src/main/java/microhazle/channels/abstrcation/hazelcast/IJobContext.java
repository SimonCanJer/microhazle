package microhazle.channels.abstrcation.hazelcast;

import java.io.Serializable;

public interface IJobContext<S extends Serializable,P extends ITransport> {

    P getInitialData();
    S getState();
    <T extends ITransport> T getReactedRequest();
    void setState(S s);


}
