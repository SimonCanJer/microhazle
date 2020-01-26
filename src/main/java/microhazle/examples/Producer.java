package microhazle.examples;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import microhazle.building.api.IBuild;
import microhazle.building.api.IClientProducer;
import microhazle.building.api.IClientRoutingGateway;
import microhazle.building.api.IMounter;
import microhazle.channels.abstrcation.hazelcast.*;
import microhazle.channels.abstrcation.hazelcast.Error;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.rmi.UnknownHostException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Producer {


    static public void main(String[] args)
    {
         Pattern pattern = Pattern.compile("\\d{1,3}.\\d{1,3}.\\d{1,3}.\\d{1,3}");
         Pattern patternNW= Pattern.compile("10.\\d{1,3}.\\d{1,3}.\\d{1,3}");
        try {
           // ServerSocket ss = new ServerSocket(9092);

            Enumeration<NetworkInterface> interfaces=NetworkInterface.getNetworkInterfaces();
            while(interfaces.hasMoreElements())
            {
                NetworkInterface ni= interfaces.nextElement();
                Enumeration<NetworkInterface> subs=ni.getSubInterfaces();;

                if(ni.isLoopback())
                    continue;
                while(subs.hasMoreElements())
                {
                    Enumeration<InetAddress> addresses=subs.nextElement().getInetAddresses();
                    while(addresses.hasMoreElements())
                    {
                        System.out.println(addresses.nextElement());
                    }
                }

                List<InterfaceAddress> la=ni.getInterfaceAddresses();
                Enumeration<InetAddress> ea=ni.getInetAddresses();
                while(ea.hasMoreElements())
                {
                    InetAddress ia =ea.nextElement();
                    String s=ia.getHostAddress();
                    Matcher m=patternNW.matcher(s);

                    if(m.matches()/*s.startsWith("10")||s.startsWith("192")*/)
                    {
                        System.out.println(  ni.getInetAddresses());
                        System.out.println(ni.getDisplayName());
                        System.out.println(s);

                    }
                }
           }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Matcher m= pattern.matcher("10.100.0.100");
        boolean b=m.matches();
        Scanner sc= new Scanner(System.in);
        final IClientRoutingGateway[] router= new IClientRoutingGateway[1];
        IClientProducer<Capitalize>[] prod= new IClientProducer[1];
        IMounter mounter = IBuild.INSTANCE.forApplication("test_app_1");
        mounter.addRequestClass(Capitalize.class);
        router[0] = mounter.mountAndStart((r)->{router[0]=r;
          r.getChannel(Capitalize.class,(p)->prod[0]=p);
          System.out.println("join");

        });

        String[] arr= new String[]{"old","new","used"};
        for(int i=0;i<300;i++) {
            int [] ref= new int[]{i};
            Arrays.stream(arr).forEach(s -> {
                try {


                    prod[0].post(new Capitalize(s+ref[0]), Producer::print);

                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            });
        }
       while(true)
       {
           String s= sc.next();
           if(s.length()==0 || s.equals("end!!!"))
               break;
           try {
               prod[0].post(new Capitalize(s),Producer::print);
            } catch (UnknownHostException e) {
               e.printStackTrace();
           }

       }
      mounter.destroy();


    }

    private static <Response extends IReply> void print(DTOReply<Response> dto) {
       if(dto.getData() instanceof Capitalized )
           System.out.println(((Capitalized)dto.getData()).getData());
       else
       {
           System.out.println(((Error)dto.getData()).getError().toString());
       }
    }
}
