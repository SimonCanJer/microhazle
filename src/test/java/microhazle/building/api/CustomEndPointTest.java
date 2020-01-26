package microhazle.building.api;

import org.junit.Test;

import static org.junit.Assert.*;

public class CustomEndPointTest {

    CustomEndPoint point= new CustomEndPoint("myservice","http");
    public
    @Test()
    void test()
    {
        getUiid();
        setUrl();
        getProtocol();
        getUrl();

    }

    public void getUiid() {
        assertNotNull(point.getUiid());
    }

    @Test
    public void setUrl() {
        point.setUrl("localhost:8080");
    }

    public void getProtocol() {
        assertEquals(point.getProtocol(),"rest");
    }

   public void getUrl() {
        assertEquals(point.getUrl(),"http://localhost:8080");
    }
}