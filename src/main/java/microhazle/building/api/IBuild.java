package microhazle.building.api;

public interface IBuild {
    IBuild INSTANCE =Selector.getBuilder();
    IMounter forApplication(String name);
}
