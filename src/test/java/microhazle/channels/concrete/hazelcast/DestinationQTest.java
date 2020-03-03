package microhazle.channels.concrete.hazelcast;

import com.hazelcast.core.IQueue;
import microhazle.channels.abstrcation.hazelcast.*;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import reactor.core.publisher.Mono;

import java.rmi.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;



@RunWith(MockitoJUnitRunner.class)
public class DestinationQTest {

    Logger logger = Logger.getLogger("tester");
    DestinationQ<ToSend> dest;
    Map<String, Consumer> pending = new HashMap<>();
    private IProducerChannel<? extends ITransport> readyChannel;

    public DestinationQTest() {
        MockitoAnnotations.initMocks(this);
    }

    void registrar(String s, Consumer<DTOReply<? extends IReply>> listener) {
        pending.put(s,listener);

    }

   void notifier(IProducerChannel<? extends ITransport> ready) {
        readyChannel = ready;
    }

    @Before
    public void init() {
        dest = new DestinationQ<ToSend>(this::registrar, "MyQ", logger);
        dest.notifier = this::notifier;
    }

    @Mock
    IQueue<DTOMessage<? extends IMessage>> mQ ;


    @Test
    public void test() {
        isNotConnected();
        postFailed();
        setQ();
        isConnected();
        postSuccess();
        monoTest();


    }

    <T extends IReply> void setQ() {
        IQueue q = mQ;
        dest.setQ(q);
    }


    private void isNotConnected() {
        Assert.assertFalse(dest.isConnected());

    }

    void isConnected() {
        Assert.assertTrue(dest.isConnected());
        Assert.assertNotNull(readyChannel);
    }

    static class ToSend implements IMessage {

    }


    <T extends ITransport> void postSuccess() {
        Exception occured = doPost();
        Assert.assertNull(occured);
        Assert.assertTrue(pending.containsKey(request.getHeader().getId()));
        Mockito.verify(mQ).add(request);
    }
    <T extends ITransport> void postFailed() {
        Exception occured = doPost();
        Assert.assertNotNull(occured);
    }
    DTOMessage request;
    private Exception doPost() {
        Exception occured = null;
        request = new DTOMessage<ToSend>(new ToSend());
        try {
            dest.post(request, (reply) -> {
            });

        } catch (UnknownHostException e) {
            occured = e;

        }
        return occured;
    }
   static class Reply implements IReply{

   }
    DTOMessage mess;
    IReply reply;
    Object sync=new Object();
    void monoTest()
    {
        try {
            Mockito.when(mQ.take()).thenReturn(mess=new DTOMessage(new ToSend()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Mono<IReply> res = null;
        Exception ex=null;
        try {
           res=dest.post(mess);
        } catch (UnknownHostException e) {
           ex=e;
        }
        res.subscribe((r)->{reply=r;},(e)->{});
        Assert.assertNull(ex);
        Assert.assertNotNull(res);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DTOMessage<? extends IMessage> sent=mQ.take();
                    Consumer c= pending.get(sent.getHeader().getId());
                    DTOReply res= new DTOReply(new Reply(),sent);
                    c.accept(res);
                    synchronized (sync)
                    {
                        sync.notifyAll();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }).start();
        synchronized (sync)
        {
            try {
                sync.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Assert.assertNotNull(reply);

    }


}