package microhazle.impl.containers;

import microhazle.channels.abstrcation.hazelcast.*;
import microhazle.processors.api.AbstractProcessor;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.rmi.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * The class manage lifecycle of a processor, providing
 * request/reply envelope into DTO, trailing and hiding behind the scene
 * send, receive
 * interaction
 * @param <T>
 */
public class ProcessorSite<T extends IMessage,S extends Serializable> implements IMessageConsumer {
// this listener dedicated to listen response to a message sent
    private final BiConsumer<String, IReply> responseListener;
    IRouter router;
    Logger logger=Logger.getLogger(this.getClass());
    AbstractProcessor<T,S> processor;
    Class mClass=IMessage.class;;
    Map<String,RequestInfo> sentItems  = new ConcurrentHashMap<>();
    Map<String,ProcessingState> openJobs= new ConcurrentHashMap<>();
    IJobContext<T,S > jobContext= new IJobContext<T,S >() {
           @Override
        public T getInitialData() {
           return processRequestContext((rc)->{return (T) rc.state.getInitial();});
        }

        @Override
        public S getState() {

            return processRequestContext((rc)->{return (S) rc.state.getState();});
        }

        @Override
        public void mutateState(Function<S, S> mutator, S triggered, Runnable trigger) {
            ProcessingState<T,S> state=processRequestContext((rc)->{return rc.state;});
            if(state!=null)
                state.mute(mutator,triggered,trigger);
        }


        @Override
        public ITransport getReactedRequest() {
            return processRequestContext((rc)->{return (T) rc.incomingRequest.getData();});
        }
    };

    private <RES extends Serializable>  RES processRequestContext(Function<RequestContext,RES> f) {
        RequestContext currContext= contextLink.get();
        if(null==currContext)
        {
            return null;
        }
        return f.apply(currContext);
    }

    class RequestContext
    {

        RequestInfo incomingRequest;
        ProcessingState state;
        void reset()
        {
            incomingRequest=null;
            state=null;
        }
    }

// keeps incoming transport object around processing
    ThreadLocal<DTOMessageTransport<? extends ITransport>> incoming= new ThreadLocal<>();
    ThreadLocal<RequestContext> contextLink=new ThreadLocal<>();

    public ProcessorSite(AbstractProcessor<T,S> p, IRouter router)
    {
        processor =p;
        logger.info("processor instance wrapped "+p.getClass());
        this.router=router;
        responseListener = (BiConsumer<String, IReply>) p.getResponseListener();
        p.setRequestSender(this::send);
        p.setResponseSender(this::sendResult);
        Method[] methods =p.getClass().getDeclaredMethods();
        p.setDataContext(jobContext);
        for(Method m:methods)
        {
            if(m.getName().equals("process"))
            {
                if(m.getParameterTypes().length==1)
                {
                    /*
                     * only extending class considered as recent(note, a complicated inheritance can be)
                     */
                    if(mClass.isAssignableFrom(m.getParameterTypes()[0])) {
                        logger.info("added processor for " + m.getParameterTypes()[0]);
                        mClass = m.getParameterTypes()[0];
                    }
                }

            }
        }
   }

    /**
     * envelops into DTO and send message to destination
     * @param r              request object
     * @param <R>            request type
     * @return               identifier of current send. Delegates the identifier creation
     *
     */
    <R extends IMessage,S extends Serializable> String send(R r)
    {
        IProducerChannel<R> channel=router.getChannel((Class<R>)r.getClass(),null);
        DTOMessageTransport<? extends ITransport> dto = incoming.get();
        try {

             String jobId= null;
             String reqId=null;
             if(dto!=null)
             {

                    if(dto instanceof DTOReply)
                    {
                        String[] split= dto.getHeader().getProcessingLabel().split("\\.");
                        jobId=split[0];

                    }
                    else
                    {
                        jobId= dto.getHeader().getId();
                    }
                    DTOMessage<R> newMessage = new DTOMessage<R>(r, dto);
                    String strLabel;
                    newMessage.getHeader().setProcessingLabel(strLabel=String.format("%s.%s",jobId,newMessage.getHeader().getId()));
                    sentItems.put(strLabel, new RequestInfo(jobId,r));
                    return channel.post(newMessage,this::onReply);

             }



        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
         return null;

    }

    /**
     * handles reply to message has been sent
     * @param reply DTO of reply
     * @param <R> reply type
     */
    <R extends IReply>void onReply(DTOReply<R> reply)
    {
        incoming.set(reply);
        RequestInfo ri=sentItems.remove(reply.getHeader().getProcessingLabel());
        ProcessingState ps= openJobs.get(ri.getJobId());
        RequestContext rc= new RequestContext();
        rc.incomingRequest=ri;
        rc.state=ps;
        this.contextLink.set(rc);
        R data=reply.getData();
        responseListener.accept(reply.getHeader().getId(),data);
        incoming.set(null);
        contextLink.set(null);

    }

    /**
     * sends result to destination, using
     * @see DTOReply
     * @param r       returned result
     * @param <R>     type of returned result
     */
    <R extends IReply> void sendResult(R r)
    {
        DTOMessageTransport<? extends ITransport> transport=incoming.get();
        String id=transport.getHeader().getId();

        DTOReply<R> send=null;
        try {
            if (transport instanceof DTOReply) {
                String[] split = transport.getHeader().getProcessingLabel().split("\\.");
                if (split.length > 1)
                    id = split[0];
                DTOReply<? extends IReply> reply = (DTOReply<R>) transport;
                send = reply.continueReply(r);
                router.reply(send);
                return;

            } else if (transport instanceof DTOMessage) {
                send = new DTOReply<R>(r, transport);
                router.reply(send);
            }
        }
        finally{
            openJobs.remove(id);
        }
    }

    /**
     * performs primary handling incoming request
     *
     */
    @Override
    public void handle(DTOMessageTransport<? extends ITransport> dto) {
        incoming.set(dto);
        ProcessingState state;
        openJobs.put(dto.getHeader().getId(),state=new ProcessingState(dto.getData()) );
        RequestContext context= new RequestContext();
        context.state=state;
        contextLink.set(context);
        processor.process((T) dto.getData());
        incoming.set(null);
    }


    @Override
    public Set<String> getHandledMessageClasses() {
        List<String> list= Arrays.asList(new String[]{mClass.getName()} );
        return new HashSet<>(list);
    }
}
