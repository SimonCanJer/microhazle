package microhazle.building.concrete;

import com.sun.org.apache.xerces.internal.impl.xpath.regex.Match;
import microhazle.building.api.CustomEndPoint;
import microhazle.building.api.IAServicePopulator;
import microhazle.channels.abstrcation.hazelcast.IGateWayServiceProvider;
import microhazle.channels.concrete.hazelcast.HazelcastChannelProvider;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NwPopulator  implements IAServicePopulator {

    private final String serviceName;
    Pattern patternDef1= Pattern.compile("192.\\d{1,3}.\\d{1,3}.\\d{1,3}");
    Pattern patternDef2= Pattern.compile("10.\\d{1,3}.\\d{1,3}.\\d{1,3}");
    List<Pattern> patterns = Arrays.asList(new Pattern[]{patternDef1,patternDef2}) ;
    IGateWayServiceProvider producer;
    NwPopulator(IGateWayServiceProvider producer,String appName)
    {
        this.producer=producer;
        this.serviceName=appName;
    }


    @Override
    public void populateNameOnPort(CustomEndPoint point, Collection<Pattern> filter, String namePattern, int port) {
        try {
            Enumeration<NetworkInterface> interfaces=NetworkInterface.getNetworkInterfaces();
            if(filter==null)
            {
                filter=patterns;
            }
            while(interfaces.hasMoreElements())
            {
                NetworkInterface ni= interfaces.nextElement();
                Enumeration<InetAddress> adds= ni.getInetAddresses();
                while(adds.hasMoreElements())
                {
                    InetAddress ia = adds.nextElement();
                    boolean add=false;
                    String s= ia.getHostAddress();
                    Iterator<Pattern> it= filter.iterator();
                    String strAddr= null;
                    while(it.hasNext() &&!add)
                    {
                        Matcher m= it.next().matcher(s);
                        add=m.matches();
                        strAddr= s+":"+port;
                    }
                    if(add)
                    {
                        point.setUrl(strAddr);
                        producer.endPointPopulator().populate(point,serviceName);
                    }


                }
            }

        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void revokePopulated(String name) {
        producer.endPointPopulator().revoke(name);
    }

    @Override
    public void queryEndPoint(String endPoint, List<CustomEndPoint> collector, Consumer<List<CustomEndPoint>> listener) {
         producer.endPointPopulator().query(endPoint,collector, listener);
    }
}
