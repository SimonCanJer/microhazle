package microhazle.channels.concrete.hazelcast;

import com.google.gson.*;
import microhazle.channels.abstrcation.hazelcast.IdentificatedObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class ImpesonatedDataImplTest {
    Map<String, JsonElement> mapValues= new HashMap<>();
    IdentificatedObject ident = new IdentificatedObject("id","token");
    void useConsumer(Consumer<Map<String,JsonElement>> modifier)
    {
        modifier.accept(mapValues);

    }
    JsonElement query(String key)
    {
        return mapValues.get(key);
    }
    IdentificatedObject getIdentity()
    {
        return ident;
    }
    ImpersonatedDataImpl tested = new ImpersonatedDataImpl(this::useConsumer, this::query, this::getIdentity);
    JsonElement toPut= new Gson().toJsonTree("toPut");

    private void testIdent()
    {
        Exception ex = null;
        try {
            Assert.assertEquals(tested.getImpersonation(),ident);
        } catch (Exception e) {
            ex = e;
        }
        Assert.assertNull(ex);
    }

    @Test
    public void test()
    {
        testIdent();
        testModification();
        testRetrieve();

    }
    void  testModification()
    {
        Gson gson = new Gson();

        tested.modify((map)->{map.put("key",toPut);});
        Assert.assertTrue(mapValues.containsKey("key"));
      }

      void testRetrieve()
      {
          JsonElement res=tested.get("key");
          Assert.assertNotNull(res);
          Assert.assertTrue(res instanceof JsonPrimitive);
          Assert.assertEquals(res,toPut);

      }
}