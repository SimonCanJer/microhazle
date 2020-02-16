package microhazle.building.api;

import microhazle.building.api.CustomEndPoint;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public interface IAServicePopulator {

    void populateNameOnPort(CustomEndPoint point, Collection<Pattern> pattern, String namePattern, int port);
    void revokePopulated(String name);

    void queryEndPoint(String myservice, List<CustomEndPoint> collector, Consumer<List<CustomEndPoint>> consumer);
    void complaignInvalid(CustomEndPoint ep);

}
