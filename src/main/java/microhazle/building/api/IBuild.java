package microhazle.building.api;

/**
 * The route class opening interaction to api for
 * messaging
 */
public interface IBuild {

    IBuild INSTANCE =Selector.getBuilder();

    /**
     *   initializes hazelcast  messaging for an application by name
     * @param name the application name
     * @return mounter interface
     * @see {IMounter}
     */
    IMounter forApplication(String name);
}
