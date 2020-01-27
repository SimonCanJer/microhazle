package microhazle.channels;

import microhazle.building.api.CustomEndPoint;

import java.util.List;
import java.util.function.Consumer;

public interface IEndPointPopulator {
    void populate(CustomEndPoint ep, String serviceName);
    void revoke(String serviceName);

    void query(String endPoint, List<CustomEndPoint> collector, Consumer<List<CustomEndPoint>> listener);
}
