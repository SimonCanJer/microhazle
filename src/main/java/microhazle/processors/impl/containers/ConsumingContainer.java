package microhazle.processors.impl.containers;

import microhazle.channels.abstrcation.hazelcast.*;
import microhazle.channels.abstrcation.hazelcast.Error;
import microhazle.processors.api.AbstractProcessor;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ConsumingContainer implements IMessageConsumer {
    final IRouter mRouter;
    HashMap<String,ProcessorSite<? extends IMessage >> mapProcessors= new HashMap<>();
    //private Consumer<Set<Class>> onReady;

    public ConsumingContainer(IRouter mRouter) {
        this.mRouter = mRouter;
    }
    HashSet<Class> setAnnouncedRequests= new HashSet<>();
   /// AtomicInteger satisfied= new AtomicInteger(0);
    public <T extends IMessage> void addProcessor(AbstractProcessor<T> p)
    {
        ProcessorSite<T> site= new ProcessorSite<>(p,mRouter);
        site.getHandledMessageClasses().stream().forEach(s->mapProcessors.put(s,site));
        Set<Class> announced = p.announceRequestNeeded();
        if(announced!=null)
            setAnnouncedRequests.addAll(announced);
    }


    @Override
    public void handle(DTOMessageTransport<? extends ITransport> dto) {
        try
        {
            mapProcessors.get(dto.getData().getClass().getName()).handle(dto);
        }
        catch(Exception e)
        {
            mRouter.reply(new DTOReply<Error>(new Error("cannot process", null),dto));

        }

    }

    @Override
    public Set<String> getHandledMessageClasses() {
        return  Collections.unmodifiableSet(mapProcessors.keySet());
    }
}
