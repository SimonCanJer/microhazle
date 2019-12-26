package microhazle.channels.abstrcation.hazelcast.admin;

import java.util.function.BiConsumer;

public interface IAdmin {
    void postReportingRequest();
    void postStopReportingRequest();
    void setQueueListener(BiConsumer<String, MonitoredQ> lsistener);

}
