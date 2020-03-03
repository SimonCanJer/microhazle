package microhazle.channels.abstrcation.hazelcast;



public interface IImpresonation {
    IImpresonatedData impersonateFor(IdentificatedObject io);
    IImpresonatedData getData(String token);
    boolean validate(String token);
    void invalidate(String token);


}
