package microhazle.impl.containers;

import microhazle.channels.abstrcation.hazelcast.*;
import microhazle.processors.api.AbstractProcessor;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.rmi.UnknownHostException;
import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ProcessorSiteTest {
    private DTOReply<StringReply> lastReply;
    private StringCommand scenario1Data;
    private Consumer<DTOReply<? extends IReply>> requestSink;
    private StringCommand scenario1DataForwarded;
    private StringCommand scenario1ReplyOnData;
    private DTOMessageTransport<StringCommand> scenario1_post;
    private StringReply secondProcessroLevelRep;
    private DTOReply<? extends IReply> secondLevelReplyDTO;
    private int replyCount = 0;
    private DTOReply<? extends IReply> finalTwoLevelResult;
    private String messageIdScenario1;
    private Processor proc;
    ProcessorSite ps;

    SecondLevelProcessor proc2;
    ProcessorSite ps2;
    private StringCommand lastProcessedData;
    private StringReply lastReplyOnCommand;
    int scenario = 0;
    ProcessingState stateOnStartProcessingInScenario1 =null;
    ProcessingState stateOnReplyInScenario1 =null;
    int stateEndScenarion1= 0;
    int stateStartScenario2=-1;

    static class StringCommand implements IMessage {
        public String getPayload() {
            return payload;
        }

        final private String payload;

        StringCommand(String payload) {
            this.payload = payload;
        }
    }

    static class StringReply implements IReply {
        public String getPayload() {
            return payload;
        }

        final private String payload;

        StringReply(String payload) {
            this.payload = payload;
        }
    }


    class Processor extends AbstractProcessor<StringCommand,Integer> {
        @Override
        public void process(StringCommand value) {
            switch (scenario) {
                // checks simple case
                 case 0:
                     //take current state from state holder and remember fro assertion
                    stateOnStartProcessingInScenario1 = (ProcessingState) ps.openJobs.get(messageIdScenario1);
                    lastProcessedData = value;//remember prccessed data fro assertion
                    String res = "REPLY_" + value.getPayload();
                    //instead of direct call of reply, we check operations with state value
                    jobContext().mutateState((s)->{return 1;},1, new Runnable() {
                        @Override
                        public void run() {
                            stateEndScenarion1=jobContext().getState();///remember  final state for simple one level scenarion
                            reply(lastReplyOnCommand = new StringReply(res));
                        }
                    });
                    break;
                case 1:
                    scenario1Data = value;// for assertion: remember initial request
                    res = "FORWARD_" + value.getPayload();
                    StringCommand comm;
                    String id = sendRequestMessage(comm = new StringCommand(res));
                    jobContext().mutateState((s)->{return 1;},null,null);
                    scenario1DataForwarded = comm;/// remember secondary request for assertion
                    stateStartScenario2= jobContext().getState();// remember state after secondary request sent for assertion
            }

        }

        @Override
        public Set<Class> announceRequestNeeded() {
            HashSet<Class> set = new HashSet<>();
            set.add(StringCommand.class);
            return set;
        }

        @Override
        protected <R extends IReply> void listener(String id, R t) {
            // is actual only foe scenario
            if (scenario == 1) {

                IJobContext <StringCommand,Integer>jb=jobContext();
                scenario1ReplyOnData= (StringCommand) jobContext().getReactedRequest();//remember reply on listener
                StringReply sr = (StringReply) t;
                StringReply reply = new StringReply("FINALLY_" + sr.payload);
                jb.mutateState((s)->{return s-1;},0,()->{this.reply(reply);});


            }

        }
    }

    class SecondLevelProcessor extends AbstractProcessor<StringCommand,Integer> {

        @Override
        public void process(StringCommand value) {
            lastProcessedData = value;// remember initial data fro assertion
            String res = "TRANSFORM_" + value.getPayload();
            this.reply(secondProcessroLevelRep = new StringReply(res));///remember reply from assertion
        }

        @Override
        public Set<Class> announceRequestNeeded() {
            return new HashSet<>();
        }

        @Override
        protected <R extends IReply> void listener(String id, R t) {

        }
    }

    int callSeq = 0;
@SuppressWarnings("all")
    IProducerChannel<StringCommand> channelProducer = new IProducerChannel<StringCommand>() {
        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public <Response extends IReply> String post(DTOMessageTransport<StringCommand> message, Consumer<DTOReply<Response>> listener) throws UnknownHostException {
            if (scenario == 1) {
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ps2.handle(message);

                    }
                });
                Object o = listener;
                message.getHeader().setSource("firstLevel");
                scenario1_post = message;

                requestSink = (Consumer<DTOReply<? extends IReply>>) o;
                t.start();

            }
            return message.getHeader().getId();

        }

        @Override
        public <R extends IReply> Mono<R> post(DTOMessageTransport<StringCommand> message) throws UnknownHostException {
            return null;
        }

        @Override
        public <R extends IReply> Future<R> send(DTOMessageTransport<StringCommand> message) throws UnknownHostException {
            return null;
        }

    };
@SuppressWarnings("all")
    IRouter router = new IRouter() {
        @Override
        public <T extends ITransport> IProducerChannel<T> getChannel(Class<T> router, Consumer<IProducerChannel<T>> readyHandler) {
            return (IProducerChannel<T>) channelProducer;
        }

        @Override
        public void reply(DTOReply<? extends IReply> transport) {
            if (scenario == 0) {
                lastReply = (DTOReply<StringReply>) transport;//remember reply for assertion
            } else if (scenario == 1) {
                if (replyCount == 0) {//related to reply on second level
                    secondLevelReplyDTO = transport;//rememebr second level reply
                    lastReply = (DTOReply<StringReply>) transport;

                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            requestSink.accept(transport);

                        }
                    });
                    replyCount++;
                    t.start();
                } else
                    finalTwoLevelResult = transport;
            }

        }
    };


    @Before
    public void initMe() {
        proc = new Processor();
        ps = new ProcessorSite(proc, router);

    }

    @Test
    public void scenario() {
        checkProcessors();
        checkMessageProcessingReply();
        checkTwoLevelReply();
    }


    private void checkProcessors() {
        assertEquals(ps.getHandledMessageClasses().size(), 1);
        assertEquals(ps.getHandledMessageClasses().iterator().next(), StringCommand.class.getName());

    }

    void checkMessageProcessingReply() {
        String src = "testing";
        String strMessage = "message";
        DTOMessage<StringCommand> message = new DTOMessage<StringCommand>(new StringCommand(strMessage));
        message.getHeader().setSource(src);
        messageIdScenario1=message.getHeader().getId();
        ps.handle(message);
        assertNotNull(stateOnStartProcessingInScenario1);
        assertEquals(ps.openJobs.size(),0);
        assertEquals(stateEndScenarion1,1);
        assertEquals(lastProcessedData.getPayload(), strMessage);
        assertNotNull(lastReplyOnCommand);
        assertEquals(lastReplyOnCommand.getPayload(), "REPLY_" + strMessage);
        assertNotNull(lastReply);
        assertEquals(lastReply.getHeader().getSource(), src);
        assertEquals(lastReply.getData().getPayload(), "REPLY_" + strMessage);
    }

    void checkTwoLevelReply() {
        proc2 = new SecondLevelProcessor();
        ps2 = new ProcessorSite(proc2, router);
        String src = "testing";
        String strMessage = "message";
        DTOMessage<StringCommand> message = new DTOMessage<StringCommand>(new StringCommand(strMessage));
        message.getHeader().setSource(src);
        scenario = 1;
        ps.handle(message);
        assertNotNull(scenario1Data);

        assertEquals(scenario1DataForwarded.getPayload(), "FORWARD_" + scenario1Data.getPayload());
        assertNotNull(requestSink);
        assertEquals(scenario1_post.getData(), scenario1DataForwarded);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertNotNull(scenario1ReplyOnData);
        assertEquals(scenario1DataForwarded,scenario1ReplyOnData);
        assertNotNull(lastProcessedData);
        assertEquals(ps.openJobs.size(),0);
        assertEquals(ps.sentItems.size(),0);
        String progress;
        assertEquals(lastProcessedData.getPayload(), progress = "FORWARD_" + scenario1Data.getPayload());
        assertEquals(secondProcessroLevelRep.getPayload(), progress = "TRANSFORM_" + progress);
        assertNotNull(secondProcessroLevelRep);
        assertNotNull(secondLevelReplyDTO);
        assertEquals(secondLevelReplyDTO.getHeader().getSource(), "firstLevel");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertNotNull(finalTwoLevelResult);
        assertEquals(finalTwoLevelResult.getHeader().getSource(),src);


    }

}