package microhazle.channels.abstrcation.hazelcast.admin;

import java.io.Serializable;

public class MonitoredQ implements Serializable {
    long lastTick=System.currentTimeMillis();

    public boolean isHealty() {
        return healty;
    }

    boolean healty;

    public MonitoredQ(String machineInstance, int numberInQ) {
        this.machineInstance = machineInstance;
        this.numberInQ = numberInQ;
    }
    void  makeTick()
    {
        long tick= System.currentTimeMillis();
        healty= tick-lastTick<2000;
        lastTick=tick;
    }


    public long getLastTick() {
        return lastTick;
    }

    public String getMachineInstance() {
        return machineInstance;
    }

    public int getNumberInQ() {
        return numberInQ;
    }

    final String machineInstance;

    public void setNumberInQ(int numberInQ) {
        this.numberInQ = numberInQ;
    }

    int    numberInQ;

    public void setAdditionalMetrix(Object additionalMetrix) {
        this.additionalMetrix = additionalMetrix;
    }

    public Object getAdditionalMetrix() {
        return additionalMetrix;
    }

    Object additionalMetrix;

}
