package microhazle.channels;

import microhazle.building.api.CustomEndPoint;

import java.util.List;
import java.util.function.Consumer;

/**
 * This interface is dedicated to populate REST  services data accorss
 * services using this instance of hazelcast and this API
 * @Aurthor S.Cantor
 */
public interface IEndPointPopulator {
    /**
     * populates the endpoint is related set, visible on another services
     * @param ep end point
     * @param serviceName network name of the service
     */
    void populate(CustomEndPoint ep, String serviceName);

    /**
     * revoke service populated/
     * Note , it is applicable to  address of an end point, which is populated by
     * current JVM instance (as IP of Tomcat for example)
     * @param serviceName name of populated service
     */
    void revoke(String serviceName);

    /**
     * query populated instances of service with this name and listen when set of
     * the services are changed
     * @param endPoint which end point
     * @param collector -  collection to get the end point description
     * @param listener -   callback to accept change notifications upon occured.
     * @see CustomEndPoint
     */
    void query(String endPoint, List<CustomEndPoint> collector, Consumer<List<CustomEndPoint>> listener);

    /**
     * informs about the end point is invalid
     * @param ep the end point
     */
    void invalidate(CustomEndPoint ep);

}
