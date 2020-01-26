package microhazle.building.api;

import microhazle.channels.abstrcation.hazelcast.IGateWayServiceProvider;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class PopulatorTest {

    @Test
    public void test()
    {
        IMounter mounter= IBuild.INSTANCE.forApplication("services");
        CustomEndPoint ep= new CustomEndPoint("myservice","http");
        Pattern p= Pattern.compile("10.\\d{1,3}.\\d{1,3}.\\d{1,3}");
        mounter.endPointPopulator().populateNameOnPort(ep, Arrays.asList(p),null,8090);
        IClientRoutingGateway  provider=mounter.mountAndStart(null);
        List<CustomEndPoint> collector= new ArrayList<>();
        mounter.endPointPopulator().queryEndPoint("myservice",collector );
        Assert.assertEquals(1,collector.size());
        Assert.assertEquals(collector.get(0).getUiid(),ep.getUiid());
        Assert.assertEquals(collector.get(0).name,ep.name);
        mounter.destroy();
    }
}
