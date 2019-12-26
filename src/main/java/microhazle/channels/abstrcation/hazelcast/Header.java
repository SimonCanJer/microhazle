package microhazle.channels.abstrcation.hazelcast;

import java.io.Serializable;
import java.util.UUID;

public class Header implements Serializable
{
    String destination;
    String source;

    public String getId() {
        return id;
    }

    String id = UUID.randomUUID().toString();
    Header(String s, String d)
    {
        destination  = d;
        source =d;
    }

    public String getDestination() {
        return destination;
    }

    public String getSource() {
        return source;
    }
    public void setSource(String s)
    {
        source = s;
    }
}
