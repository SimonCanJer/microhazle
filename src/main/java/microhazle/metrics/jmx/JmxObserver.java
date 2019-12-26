package microhazle.metrics.jmx;

import com.google.gson.Gson;
import com.sun.jmx.mbeanserver.MXBeanMappingFactory;

import java.lang.management.*;
import java.util.*;

public class JmxObserver {

    static public final String GC_ID= "java.lang:type=GarbageCollector";
    static public final String MEMORY_ID="java.lang:type=Memory";
    static public final String  THREADING_ID="java.lang:type=Threading";
    private List<GarbageCollectorMXBean> gcBeans;
    private ThreadMXBean threadObserver;

    void init()
    {
        gcBeans=ManagementFactory.getGarbageCollectorMXBeans();
        threadObserver= ManagementFactory.getThreadMXBean();


    }
    static class Memory extends Metric<MemoryMXBean>
    {
        MemoryUsage heap;
        MemoryUsage nonHeap;

        @Override
        protected void doFill(MemoryMXBean src) {
            heap= src.getHeapMemoryUsage();
            nonHeap= src.getNonHeapMemoryUsage();
        }
    }
    static abstract class Metric<FROM>
    {
        Metric fill(FROM src)
        {
            doFill(src);
            return  this;
        }

        protected abstract void doFill(FROM src);

    }
    Map<String,Object> collectMetrics()
    {
        Map<String, Object> mapMetrix = new HashMap<>();


        mapMetrix.put(GC_ID,new GC().fill(gcBeans));
        mapMetrix.put(MEMORY_ID,new Threads().fill(threadObserver));
        mapMetrix.put(THREADING_ID,new Memory().fill(ManagementFactory.getMemoryMXBean()));
        return mapMetrix;
    }
    static class GC extends Metric<List<GarbageCollectorMXBean>>
    {
        final List<GCUnit> units = new ArrayList<>();


        @Override
        protected void doFill(List<GarbageCollectorMXBean> src) {
            src.forEach(gc->{units.add(new GCUnit(gc));});
        }
    }
    String jsonSerializedGrab()
    {
        Gson gson= new Gson();
        return gson.toJson(collectMetrics());
    }
    static class GCUnit
    {
        public String getName() {
            return name;
        }

        public long getCount() {
            return count;
        }

        public long getTime() {
            return time;
        }

        final String name;
        final long count;
        final long time;


        GCUnit(GarbageCollectorMXBean src) {
            this.name = src.getName();
            this.count = src.getCollectionCount();
            this.time = src.getCollectionTime();
        }
    }
    static class Threads extends Metric<ThreadMXBean> {
        int threadsNum;
        List<ThreadData> data = new ArrayList<>();
        @Override
        protected void doFill(ThreadMXBean src)
        {
            threadsNum = src.getThreadCount();
            Arrays.stream(src.getAllThreadIds()).forEach(id->data.add(new ThreadData(id,src.getThreadCpuTime(id))));

        }
    }
    static class ThreadData
    {

        final long id;
        final long time;


        ThreadData(long id, long time) {
            this.id = id;
            this.time = time;
        }
    }
}
