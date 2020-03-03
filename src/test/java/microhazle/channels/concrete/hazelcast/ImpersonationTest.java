package microhazle.channels.concrete.hazelcast;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.IMap;
import com.hazelcast.map.EntryProcessor;
import microhazle.channels.abstrcation.hazelcast.IImpresonatedData;
import microhazle.channels.abstrcation.hazelcast.IdentificatedObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ImpersonationTest {
    public static final String DKEY = "dkey";
    public static final String TEST_VALUE = "testValue";
    public static final String TEST_KEY = "key";
    Impersonation imp;
    private MapConfig config;
    String modifiedKey=null;
    Map<String,byte[]> emul = new HashMap<>();
    String getKey= null;
    byte[] getValue;

    IMap<String,byte[]> map = (IMap<String, byte[]>) Proxy.newProxyInstance(this.getClass().getClassLoader(),
            new Class[]{IMap.class}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    String name = method.getName();
                    switch(name)
                    {
                        case "putIfAbsent":
                            String key;
                            emul.put(key=(String) args[0],(byte[]) args[1]);
                            assertEquals("test putIfAbsent",key,DKEY);
                            break;
                        case "executeOnKey":

                            EntryProcessor ep= (EntryProcessor) args[1];
                            modifiedKey= (String) args[0];
                            Assert.assertEquals("the same id",modifiedKey,DKEY);
                            Assert.assertTrue(emul.containsKey(modifiedKey));
                            for(Map.Entry<String,byte[]> e: emul.entrySet())
                            {
                                emul.put(modifiedKey, (byte[]) ep.process(e));
                                break;
                            }
                            return emul.get(modifiedKey);

                        case "get":
                            getKey = (String) args[0];
                            getValue=emul.get(getKey);
                            Assert.assertEquals("the same id",getKey,DKEY);
                            return getValue;

                    }
                    return null;
                }
            });

    @Before
    public void before()
    {
        imp= new Impersonation((config)->{this.config=config;});
    }
    @Test
    public void test()
    {
        imp.connect();
        Assert.assertNotNull(config);
        String [] qName= new String[1];
        imp.init((s)->{qName[0]=s;return map;});
        assertEquals("test qname is proper",qName[0],config.getName());
        imp.impersonateFor(new IdentificatedObject("did",DKEY));
        Assert.assertEquals(emul.size(),1);
        IImpresonatedData data=verifyImpersonatedData();
        testModification(data);
        testGet(data);
    }

    private IImpresonatedData verifyImpersonatedData() {
        IImpresonatedData data=imp.getData(DKEY);
        Assert.assertNotNull(data);
        try {
            IdentificatedObject id=data.getImpersonation();
            Assert.assertEquals("checking id ",id.getDomainToken(),DKEY);
            return data;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;

    }
    private void testModification(IImpresonatedData data)
    {
        byte[] prev=emul.get(DKEY);
        JsonElement json= new Gson().toJsonTree(TEST_VALUE);
        data.modify((map)->map.put(TEST_KEY,json));
        Assert.assertNotNull("modified key not null",modifiedKey);
        Assert.assertEquals("domain key modified",modifiedKey,DKEY);
        byte[] newVal=emul.get(DKEY);
        Assert.assertNotEquals("value changed",prev,newVal);
    }
    private void testGet(IImpresonatedData data)
    {
        getKey = null;
       JsonElement el=data.get(TEST_KEY);
       assertNotNull(el);
       assertEquals(getKey,DKEY);
       assertEquals("check the modified value received",el.getAsString(),TEST_VALUE);
    }

}