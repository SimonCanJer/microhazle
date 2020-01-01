package microhazle.channels.abstrcation.hazelcast;

import org.junit.Test;

import javax.xml.ws.Response;
import java.io.Serializable;

import static org.junit.Assert.*;

public class DTOMessageTest {

    static DTOMessage<Call> secondMessage;
    static String[] src=new   String[] {"src1","src2"};


    static class Call implements IMessage
    {
        public String getMethod() {
            return method;
        }

        public Serializable[] getArgs() {
            return args;
        }

        String method;
        Serializable[] args;

    }

   static DTOMessage<Call> message = new DTOMessage<Call>(new Call());
    @Test
    public void getData() {

        testMessage();
    }

    static void testMessage() {
        assertNotNull(message.header);
        assertEquals(message.stack.size(),0);
        assertEquals(message.getHeader().destination,Call.class.getName());
        message.getHeader().setSource(src[0]);
        progress();
    }


    static public void progress() {
        testProgress();
    }

     static void testProgress() {
        DTOMessage<Call> next = new DTOMessage<Call>(new Call(),message);
        assertNotNull(next.header);
        assertEquals(next.stack.size(),1);
        assertEquals(next.getHeader().destination,Call.class.getName());
        next.getHeader().setSource(src[1]);
        secondMessage=next;
    }
    static void run()
    {
        testMessage();
        testProgress();

    }
}