package microhazle.building.api;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class SelectorTest {

    @Test
    public void getBuilder() {
        Exception error=null;
        try {
            IBuild build= Selector.getBuilder();

        }
        catch(Exception e)
        {
            error =e;

        }
        Assert.assertNull(error);
    }
}