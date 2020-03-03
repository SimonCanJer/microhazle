package microhazle.channels.concrete.hazelcast;

import com.google.gson.JsonElement;
import com.hazelcast.core.IFunction;
import microhazle.channels.abstrcation.hazelcast.IImpresonatedData;
import microhazle.channels.abstrcation.hazelcast.IdentificatedObject;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;


  class ImpersonatedDataImpl implements IImpresonatedData {

    private final Consumer<Consumer<Map<String, JsonElement>>> invoker;
    private final Function<String, JsonElement> getter;
    private final Callable<IdentificatedObject> getPersonal;

    ImpersonatedDataImpl(Consumer<Consumer<Map<String,JsonElement>>> invokeModif, Function<String,JsonElement> getter, Callable<IdentificatedObject> getPeronal)
    {

        invoker = invokeModif;
        this.getter= getter;
        this.getPersonal=getPeronal;


    }
    @Override
    public void modify(Consumer<Map<String, JsonElement>> modifier) {
        invoker.accept(modifier);
    }

    @Override
    public JsonElement get(String s) {
        return getter.apply(s);
    }


    @Override
    public IdentificatedObject getImpersonation() throws Exception {
        return getPersonal.call();
    }
}
