package microhazle.channels;

import microhazle.building.api.CustomEndPoint;

import java.util.List;

public interface IEndPointPopulator {
    void populate(CustomEndPoint ep, String serviceName);

    void query(String endPoint, List<CustomEndPoint> collector);
}
