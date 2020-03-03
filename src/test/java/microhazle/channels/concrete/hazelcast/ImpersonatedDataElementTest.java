package microhazle.channels.concrete.hazelcast;

import com.google.gson.JsonObject;
import microhazle.channels.abstrcation.hazelcast.IdentificatedObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ImpersonatedDataElementTest {

    ImpersonatedDataElement obj;
    IdentificatedObject ident ;
    @Before
    public void init()
    {
        ident=new IdentificatedObject("did","token");
        obj=new ImpersonatedDataElement(ident);
    }
    @Test
    public void test()
    {
        Assert.assertFalse(ident.isExpired());
        Assert.assertFalse(ident.isValid());
        Assert.assertNotNull(obj.getPersonalOf());
        Assert.assertFalse(obj.getPersonalOf().isValid());
        ident.activate(1000);
        Assert.assertTrue(obj.getPersonalOf().isValid());
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertFalse(obj.getPersonalOf().isValid());
        ident.activate(10000);
        Assert.assertFalse(obj.getPersonalOf().isExpired());
        JsonObject el= new JsonObject();

        el.addProperty("key1", "value1");
        obj.setValue("myValues",el);
        Assert.assertTrue(obj.getValue("myValues") instanceof JsonObject);
        String s=((JsonObject)obj.getValue("myValues")).get("key1").getAsString();
        System.out.println("retrieved value "+s);
        Assert.assertEquals(s,"value1");
        byte[] serial= ImpersonatedDataElement.serialize(obj);
        ImpersonatedDataElement des = ImpersonatedDataElement.deserialize(serial);
        IdentificatedObject identDes= des.getPersonalOf();
        Assert.assertNotNull(identDes);
        Assert.assertFalse(obj.getPersonalOf().isExpired());
        Assert.assertEquals(identDes.getOpenedTime(),ident.getOpenedTime());
        Assert.assertEquals(identDes.getExpirationTime(),ident.getExpirationTime());
        Assert.assertEquals(identDes.getDomainId(),ident.getDomainId());
        Assert.assertEquals(identDes.getDomainToken(),ident.getDomainToken());
        Assert.assertTrue(des.getValue("myValues") instanceof JsonObject);
        Assert.assertEquals(((JsonObject)des.getValue("myValues")).get("key1").getAsString(),"value1");
    }



}