package microhazle.channels.abstrcation.hazelcast;

import com.google.gson.JsonElement;

import java.util.Map;
import java.util.function.Consumer;

public interface IImpresonatedData {
    class CachingExpired extends Exception
    {
        public long getSetAt() {
            return setAt;
        }

        public long getExpirationTime() {
            return expirationTime;
        }

        final long setAt  ;
        final long expirationTime;

        CachingExpired(long setAt, long expirationTime) {
            this.setAt = setAt;
            this.expirationTime = expirationTime;
        }
    }
     void modify(Consumer<Map<String, JsonElement>> data);
     JsonElement get(String s);
     IdentificatedObject getImpersonation() throws Exception;




}
