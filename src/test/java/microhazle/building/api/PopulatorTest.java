package microhazle.building.api;


import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class PopulatorTest {
   static  List<CustomEndPoint> updated;
   static final String REGISTER_INVOKE="register-invoke";
    static final String REGISTER_INVALIDATE="register-invalidate";
    static void listen(  List<CustomEndPoint> update)
    {
        updated=update;

    }
    IMounter mounter;
    @Test
    public void test()
    {
        IMounter mounter= IBuild.INSTANCE.forApplication("services");
        CustomEndPoint ep= new CustomEndPoint(REGISTER_INVOKE,"http");
        CustomEndPoint ep1= new CustomEndPoint(REGISTER_INVALIDATE,"http");
        Pattern p= Pattern.compile("10.\\d{1,3}.\\d{1,3}.\\d{1,3}");
        mounter.endPointPopulator().populateNameOnPort(ep, Arrays.asList(p),null,8090);
        mounter.endPointPopulator().populateNameOnPort(ep1, Arrays.asList(p),null,8091);
        String s=ep.toString();
        IClientRoutingGateway  provider=mounter.mountAndStart(null);
        List<CustomEndPoint> collector= new ArrayList<>();
        updated= new ArrayList<>();
        mounter.endPointPopulator().queryEndPoint(REGISTER_INVOKE,collector,PopulatorTest::listen);
        Assert.assertEquals(1,collector.size());
        Assert.assertEquals(collector.get(0).getUiid(),ep.getUiid());
        Assert.assertEquals(collector.get(0).name,ep.name);
        mounter.endPointPopulator().revokePopulated(REGISTER_INVOKE);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertEquals(0,updated.size());

        collector.clear();
        updated.clear();
        updated.add(ep1);
        mounter.endPointPopulator().queryEndPoint(REGISTER_INVALIDATE,collector,PopulatorTest::listen);
        Assert.assertEquals(collector.size(),1);
        collector.clear();
        mounter.endPointPopulator().complaignInvalid(ep1);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(updated.size(),0);


        mounter.destroy();
    }
}
