package microhazle.channels.concrete.hazelcast;

import microhazle.channels.abstrcation.hazelcast.*;
import com.sun.jmx.snmp.internal.SnmpIncomingResponse;
import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Flux;

import java.io.Serializable;
import java.net.URLClassLoader;
import java.rmi.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HazelcastChannelProviderTest {

    HazelcastChannelProvider hazelcast = new HazelcastChannelProvider();
    static class MessageRequest implements IMessage
    {
        public String getMethod() {
            return method;
        }

        public Serializable[] getData() {
            return data;
        }

        final private String method;
        final private Serializable[] data;

        public MessageRequest(String method, Serializable[] data) {
            this.method = method;
            this.data = data;
        }
    }

    static class  Response implements IReply {
        private final Serializable data;

        public Response(Serializable data) {
            this.data = data;

        }

        public Serializable getData() {
            return data;
        }
    }
    Set<String> consumed=null;
    String methodMessage=null;
    String replyMessage;
    IMessageConsumer consumer = new IMessageConsumer() {
        @Override
        public void handle(DTOMessageTransport<? extends ITransport> dto) {

            if(dto instanceof  DTOMessage)
            {
                DTOMessage<MessageRequest> m= (DTOMessage<MessageRequest>) dto;
                methodMessage=m.getData().getMethod();
                router.reply(new DTOReply<>(new Response(replyMessage=strExpectedReply),m ));
                return ;

            }
      }


        @Override
        public Set<String> getHandledMessageClasses() {
             consumed=new HashSet<>();
             consumed.addAll(Arrays.asList(new String[]{MessageRequest.class.getName()}));
             return consumed;

        }
    };
    IProducerChannel<MessageRequest> channelRep;


    void connectedProducer(IProducerChannel<? extends ITransport> transport)
    {
        channelRep= (IProducerChannel<MessageRequest>) transport;

    }
    IRouter router;
    void  listenReply(DTOReply<? extends IReply> dto)
    {
        if(dto instanceof  DTOReply) {
            DTOReply<Response> r = (DTOReply<Response>) dto;
            Assert.assertEquals(r.getData().getData(), replyMessage);
            synchronized (this) {
                notify();
            }
            return;
        }

    }
    String strExpectedReply="replyMethod";

    @Test
    public void initServiceAndRouting() {

        String home= System.getProperty("java.home");
        URLClassLoader cl= (URLClassLoader) this.getClass().getClassLoader();

        router= hazelcast.initServiceAndRouting("test",consumer);
        Assert.assertNotNull(hazelcast.mConsumer);
        IProducerChannel<MessageRequest> channel = router.getChannel(MessageRequest.class,this::connectedProducer);
        Assert.assertNotNull(channel);
        int count =0;
        while(channelRep==null && count++<10)
        {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Assert.assertNotNull(channelRep);
        Assert.assertTrue(channel.isConnected());
        Assert.assertNotNull(consumed);
        Assert.assertEquals(hazelcast.getConsumedQueueNames().size(),2);

        String method="callMe";
        try {
            channel.post(new DTOMessage<MessageRequest>(new MessageRequest(method, new Integer[]{0,0})),this::listenReply);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        try {
            synchronized (this)
            {
                wait(10000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertNotNull(replyMessage);
        Assert.assertEquals(methodMessage,method);
        Assert.assertEquals(replyMessage, strExpectedReply);

        try {
            Flux<Response> f= channel.post(new DTOMessage<MessageRequest>(new MessageRequest(method, new Integer[]{0,0})));
            f.subscribe((r)-> Assert.assertEquals(r.getData(), replyMessage));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        try {
            synchronized (this)
            {
                wait(5000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        hazelcast.shutdown();
    }


}