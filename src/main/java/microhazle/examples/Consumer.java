package microhazle.examples;

import microhazle.building.api.IBuild;
import microhazle.building.api.IMounter;
import microhazle.channels.abstrcation.hazelcast.IReply;
import microhazle.processors.api.AbstractProcessor;

import java.util.HashSet;
import java.util.Set;

public class Consumer
{

    static private class Capitalizer extends AbstractProcessor<Capitalize,Integer>
    {
        Set<Class> processed= new HashSet<>();
        {
            processed.add(Capitalize.class);
        }

        @Override
        public void process(Capitalize value) {
            Capitalized  res= new Capitalized(value.getData().toUpperCase());
            System.out.println("Server Capitalize: "+value.getData());
            this.reply(res);
        }

        @Override
        public Set<Class> announceRequestNeeded() {
            return processed;
        }

        @Override
        protected <R extends IReply> void listener(String id, R t) {

        }
    }
    static public void main(String[] args)
    {
        IMounter mount= IBuild.INSTANCE.forApplication("test_app_1");
        mount.addProcessor(new Capitalizer());
        mount.mountAndStart((r)->System.out.println("Start"));
        try {
            Thread.sleep(100000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
