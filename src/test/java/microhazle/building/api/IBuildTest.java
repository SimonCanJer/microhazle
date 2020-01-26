package microhazle.building.api;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class IBuildTest {

    @Test
    public void forApplication() {
        IMounter mounter= IBuild.INSTANCE.forApplication("services");
        Assert.assertNotNull(mounter);

    }
}