package microhazle.building.api;

import microhazle.building.api.CustomEndPoint;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public interface IAServicePopulator {

    void populateNameOnPort(CustomEndPoint point, Collection<Pattern> pattern, String namePattern, int port);

    void queryEndPoint(String myservice, List<CustomEndPoint> collector);

}
