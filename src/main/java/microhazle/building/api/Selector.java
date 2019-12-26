package microhazle.building.api;

public class Selector {
   private  static final String DEFAULT_BUIDER="microhazle.building.concrete.Builder";
   static IBuild builder;
    static public IBuild getBuilder()
    {
        if(builder==null) {
            synchronized (Selector.class) {
                if(builder==null) {
                    try {
                        String str=System.getProperty("builder");
                        if(str==null)
                            str=DEFAULT_BUIDER;
                        Class<IBuild> clazz = (Class<IBuild>) Class.forName(str);
                        return clazz.newInstance();
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return null;

    }
}
