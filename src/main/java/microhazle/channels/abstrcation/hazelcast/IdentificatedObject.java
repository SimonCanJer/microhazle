package microhazle.channels.abstrcation.hazelcast;

public class IdentificatedObject {
     String  domainId;
    String domainToken;
    long    openedTime = -1;
    long expirationTime=-1;
    public String getDomainId() {
        return domainId;
    }
    public IdentificatedObject(String domainId)
    {

        this.domainId = domainId;
    }
    public IdentificatedObject(String domainId, String token)
    {

        this.domainId = domainId;
        this.domainToken=token;
    }
    public String getDomainToken() {
        return domainToken;
    }

    public long getOpenedTime() {
        return openedTime;
    }

    public long getExpirationTime() {
        return expirationTime;
    }


    public boolean  isExpired()  {
        if(expirationTime<0)
            return false;
        if(System.currentTimeMillis()<(openedTime+ expirationTime))
            return false;
        return true;

    }
    public boolean isValid()
    {
        return !isExpired() &&openedTime>0;
    }
    public void activate(long expirationTime)
    {
        openedTime = System.currentTimeMillis();
        this.expirationTime = expirationTime;

    }
}
