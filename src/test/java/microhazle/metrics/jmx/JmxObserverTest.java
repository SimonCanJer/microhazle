package microhazle.metrics.jmx;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class JmxObserverTest {

   JmxObserver observer = new JmxObserver();
    @Test
    public void jsonSerializedGrab() {
        Throwable t=null; ;
        observer.init();
        try {
            System.out.println(observer.jsonSerializedGrab());
        }
        catch(Throwable e)
        {
            t=e;

        }
        Assert.assertNull(t);

    }
}