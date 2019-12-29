package microhazle.channels.abstrcation.hazelcast;

import java.io.Serializable;
import java.util.UUID;

/**
 * The class defines header part of a message transport object and
 * contains matadata of the transported message
 *
 */
public class Header implements Serializable
{
    String destination;
    String source;
    String id = UUID.randomUUID().toString();

    public String getId() {
        return id;
    }


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
