package microhazle.channels.abstrcation.hazelcast;

import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;

import static org.junit.Assert.*;

public class DTOReplyTest {
   static class Response implements IReply
   {
       Serializable res;

       public Response(Serializable res) {
           this.res = res;
       }
   }
    @Before
    public void init()
    {
        if(DTOMessageTest.secondMessage==null)
        {
            DTOMessageTest.run();

        }
    }
    DTOReply<Response> reply;
   @Test
   public void testScenario()
   {
       testConstrutor();
       testReplyProgress();

   }
    private void testConstrutor()
   {
       reply = new DTOReply<Response>(new Response("res1"),DTOMessageTest.secondMessage);
       assertNull(DTOMessageTest.secondMessage.header);
       assertEquals(reply.getHeader().source,DTOMessageTest.src[1]);
       assertEquals(reply.stack.size(),1);
   }

   private void testReplyProgress()
   {
       DTOReply<Response> second= reply.continueReply(new Response("res2"));
       assertEquals(second.getHeader().source,DTOMessageTest.src[0]);
       assertEquals(second.stack.size(),0);

   }

}