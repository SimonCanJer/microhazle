package microhazle.channels.concrete.hazelcast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import microhazle.channels.abstrcation.hazelcast.IdentificatedObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

class ImpersonatedDataElement {
    IdentificatedObject personalOf;
    Map<String, JsonElement>  values= new HashMap<>();

    ImpersonatedDataElement(IdentificatedObject personalOf)
    {
        this.personalOf = personalOf;
        values=new HashMap<>();

    }


    public void setValue(String s, JsonElement el) {
        this.values.put(s,el);
    }

    public IdentificatedObject getPersonalOf() {
        return personalOf;
    }

    public JsonElement getValue(String s) {
        return values.get(s);
    }


    static ImpersonatedDataElement deserialize(byte[] array)
    {
        Gson gson= new Gson();
        String s= new String(array, StandardCharsets.UTF_8);
        return gson.fromJson(s,ImpersonatedDataElement.class);
    }
    static byte []  serialize(ImpersonatedDataElement element)
    {
        Gson gson = new Gson();

        return gson.toJson(element).getBytes(StandardCharsets.UTF_8);

    }

}
