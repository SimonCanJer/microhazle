package microhazle.channels.concrete.hazelcast;

import com.google.gson.JsonElement;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.IMap;
import com.hazelcast.map.EntryBackupProcessor;
import com.hazelcast.map.EntryProcessor;
import microhazle.channels.abstrcation.hazelcast.IImpresonatedData;
import microhazle.channels.abstrcation.hazelcast.IImpresonation;
import microhazle.channels.abstrcation.hazelcast.IdentificatedObject;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

public class Impersonation implements IImpresonation {

    static final String Q="Queue_"+Impersonation.class.getName();
    private final Consumer<MapConfig> configReg;
    IMap<String, byte[]> impersonated;
    MapConfig  mapConfig=new MapConfig();
    {
        mapConfig.setBackupCount(1);
        mapConfig.setName(Q);
        mapConfig.setAsyncBackupCount(0);
       // mapConfig.setInMemoryFormat(InMemoryFormat.NATIVE);
    }
    void connect()
    {
        configReg.accept(mapConfig);
    }
    Impersonation(Consumer<MapConfig> registrar)
    {
        this.configReg = registrar;

    }
    void init(Function<String, IMap<String, byte[]>> creator)
    {
        impersonated= creator.apply(Q);
    }

    @Override
    public IImpresonatedData impersonateFor(IdentificatedObject io) {

        ImpersonatedDataElement el;
        impersonated.putIfAbsent(io.getDomainToken(),ImpersonatedDataElement.serialize(el=new ImpersonatedDataElement(io)));
        ImpersonatedDataElement data = ImpersonatedDataElement.deserialize(impersonated.get(io.getDomainToken()));
        return getImpresonatedData(data);
    }

    private IImpresonatedData getImpresonatedData(ImpersonatedDataElement data) {
        return new ImpersonatedDataImpl(new Consumer<Consumer<Map<String, JsonElement>>>()
        {

            @Override
            public void accept(Consumer<Map<String, JsonElement>> processor) {
                impersonated.executeOnKey(data.personalOf.getDomainToken(), new EntryProcessor() {
                    @Override
                    public Object process(Map.Entry entry) {
                        byte[] v= (byte[]) entry.getValue();
                        ImpersonatedDataElement el =ImpersonatedDataElement.deserialize(v);
                        processor.accept(el.values);
                        v= ImpersonatedDataElement.serialize(el);
                        entry.setValue(v);
                        return v;
                    }

                    @Override
                    public EntryBackupProcessor getBackupProcessor() {
                        return null;
                    }
                });
            }
        }, new Function<String,JsonElement>(){
            @Override
            public JsonElement apply(String s) {
                byte[] ser= impersonated.get(data.personalOf.getDomainToken());
                if(ser==null)
                    return null;
                return ImpersonatedDataElement.deserialize(ser).values.get(s);
            }
        }, new Callable<IdentificatedObject>(){
            @Override
            public IdentificatedObject call() throws Exception {
                return data.personalOf;
            }
        });
    }

    @Override
    public IImpresonatedData getData(String token) {
        ImpersonatedDataElement el = getImpersonatedDataElement(token);
        return  this.getImpresonatedData(el);
    }

    private ImpersonatedDataElement getImpersonatedDataElement(String token) {
        byte[] data = impersonated.get(token);
        return ImpersonatedDataElement.deserialize(data);
    }

    @Override
    public boolean validate(String token) {
        ImpersonatedDataElement element=  getImpersonatedDataElement(token);
        return element!=null & element.personalOf.isValid();
    }

    @Override
    public void invalidate(String token) {
        impersonated.remove(token);

    }
}
