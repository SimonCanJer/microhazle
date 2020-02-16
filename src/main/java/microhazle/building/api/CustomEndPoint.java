package microhazle.building.api;

import com.google.gson.Gson;
import java.nio.charset.Charset;
import java.util.UUID;

public class CustomEndPoint  {
    private  String protocolPrefix;

    public String getUiid() {
        return uiid;
    }

    String   uiid= UUID.randomUUID().toString();

    public String getProtocolPrefix() {
        return protocolPrefix;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    long     timeStamp;
    public void invalidate()
    {
        timeStamp=System.currentTimeMillis();
    }


    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    public CustomEndPoint()
    {
        protocolPrefix="http";
        timeStamp=System.currentTimeMillis();

    }
    public CustomEndPoint(String name,String protocolPrefix)
    {
        this();
        this.name=name;
        this.protocolPrefix=protocolPrefix;

    }
    public void setUrl(String url) {
        this.url = protocolPrefix+"://"+url;
    }

    public void setName(String name) {
        this.name = name;
    }

    String protocol = "rest";
    String url;
    String name;

    public void setDetails(String details) {
        this.details = details;
    }

    String details;

    public String getProtocol() {
        return protocol;
    }

    public String getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }

    public String getDetails() {
        return details;
    }
    public static String toGson(CustomEndPoint endPoint)
    {
        Gson gson = new Gson();
        return gson.toJson(endPoint);
    }

    public static byte[] gsonMarshal(CustomEndPoint endPoint)
    {
        return  toGson(endPoint).getBytes(Charset.forName("UTF-8"));
    }
    public static  CustomEndPoint gsonUnmarshal(byte[] bytes)
    {

        Gson gson = new Gson();
        String s= new String(bytes,Charset.forName("UTF-8"));
        return gson.fromJson(s,CustomEndPoint.class);
    }

    @Override
    public String toString() {
        return toGson(this);
    }
}
