package microhazle.impl.containers;

import microhazle.channels.abstrcation.hazelcast.*;
import microhazle.channels.abstrcation.hazelcast.Error;
import microhazle.processors.api.AbstractProcessor;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.*;

public class ConsumingContainer implements IMessageConsumer {
    final IRouter mRouter;
    Logger logger=Logger.getLogger(ConsumingContainer.class);
    HashMap<String,ProcessorSite<? extends IMessage ,? extends Serializable>> mapProcessors= new HashMap<>();
    //private Consumer<Set<Class>> onReady;

    public ConsumingContainer(IRouter mRouter) {
        this.mRouter = mRouter;
    }
    HashSet<Class> setAnnouncedRequests= new HashSet<>();
   /// AtomicInteger satisfied= new AtomicInteger(0);
    public <T extends IMessage, S extends Serializable> void addProcessor(AbstractProcessor<T,S> p)
    {
        ProcessorSite<T,S> site= new ProcessorSite<>(p,mRouter);
        logger.trace("adding processor "+p.getClass());
        site.getHandledMessageClasses().stream().forEach(s->mapProcessors.put(s,site));
        Set<Class> announced = p.announceRequestNeeded();
        if(announced!=null)
            setAnnouncedRequests.addAll(announced);
    }


    @Override
    public void handle(DTOMessageTransport<? extends ITransport> dto) {
        try
        {
            ProcessorSite<? extends IMessage,? extends Serializable> processor= mapProcessors.get(dto.getData().getClass().getName());
            if(processor==null)
            {
                if(dto instanceof DTOReply)
                {
                    DTOReply rep= (DTOReply) dto;
                    if(rep.canBePropagated())
                    {
                        logger.trace("continue reply");
                        mRouter.reply(rep.continueReply((IReply)null));

                    }
                    return ;
                }
                mRouter.reply(new DTOReply<Error>(new Error("type "+dto.getData().getClass(), new Exception(dto.getData().getClass().toString())),dto));
                return ;
            }
            processor.handle(dto);
        }
        catch(Exception e)
        {
            ///e.printStackTrace();
            logger.error(e.toString());
            mRouter.reply(new DTOReply<Error>(new Error("cannot process", e),dto));

        }

    }

    @Override
    public Set<String> getHandledMessageClasses() {
        return  Collections.unmodifiableSet(mapProcessors.keySet());
    }
}
