package microhazle.processors.impl;

import microhazle.channels.abstrcation.hazelcast.*;
import microhazle.processors.api.AbstractProcessor;
import microhazle.processors.impl.containers.ProcessorSite;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;

import java.rmi.UnknownHostException;
import java.util.*;
import java.util.function.Consumer;

public class ProcessorSiteTest {
    private DTOReply<StringReply> lastReply;
    private StringCommand scenario1Data;
    private Consumer<DTOReply<? extends IReply>> requestSink;
    private StringCommand scenario1DataForwarded;
    private DTOMessageTransport<StringCommand> scenario1_post;
    private StringReply secondProcessroLevelRep;
    private DTOReply<? extends IReply> secondLevelReplyDTO;
    private StringCommand scenario1ListenerGot;
    private int replyCount = 0;
    private DTOReply<? extends IReply> finalTwoLevelResult;


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

    private StringCommand lastProcessedData;
    private StringReply lastReplyOnCommand;
    int scenario = 0;

    class Processor extends AbstractProcessor<StringCommand> {
        Map<String, StringCommand> mapsent = new HashMap<>();


        @Override
        public void process(StringCommand value) {


            switch (scenario) {
                case 0:
                    lastProcessedData = value;
                    String res = "REPLY_" + value.getPayload();

                    reply(lastReplyOnCommand = new StringReply(res));
                    break;
                case 1:
                    scenario1Data = value;
                    res = "FORWARD_" + value.getPayload();
                    StringCommand comm;
                    String id = sendMessage(comm = new StringCommand(res));
                    mapsent.put(id, comm);
                    scenario1DataForwarded = comm;
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
            if (scenario == 1) {
                scenario1ListenerGot = mapsent.get(id);
                StringReply sr = (StringReply) t;
                StringReply reply = new StringReply("FINALLY_" + sr.payload);
                this.reply(reply);
            }

        }
    }

    class SecondLevelProcessor extends AbstractProcessor<StringCommand> {

        @Override
        public void process(StringCommand value) {
            lastProcessedData = value;
            String res = "TRANSFORM_" + value.getPayload();

            this.reply(secondProcessroLevelRep = new StringReply(res));
        }

        @Override
        public Set<Class> announceRequestNeeded() {
            return new HashSet<>();
        }

        @Override
        protected <R extends IReply> void listener(String id, R t) {

        }
    }

  //  DTOMessage<StringCommand> lastSendMessage = null;
    int callSeq = 0;

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
        public <R extends IReply> Flux<R> post(DTOMessageTransport<StringCommand> message) throws UnknownHostException {
            return null;
        }

    };
    IRouter router = new IRouter() {
        @Override
        public <T extends ITransport> IProducerChannel<T> getChannel(Class<T> router, Consumer<IProducerChannel<T>> readyHandler) {
            return (IProducerChannel<T>) channelProducer;
        }

        @Override
        public void reply(DTOReply<? extends IReply> transport) {
            if (scenario == 0) {
                lastReply = (DTOReply<StringReply>) transport;
            } else if (scenario == 1) {
                if (replyCount == 0) {
                    secondLevelReplyDTO = transport;
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
    Processor proc;
    ProcessorSite ps;

    SecondLevelProcessor proc2;
    ProcessorSite ps2;

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
        Assert.assertEquals(ps.getHandledMessageClasses().size(), 1);
        Assert.assertEquals(ps.getHandledMessageClasses().iterator().next(), StringCommand.class.getName());

    }

    void checkMessageProcessingReply() {
        String src = "testing";
        String strMessage = "message";
        DTOMessage<StringCommand> message = new DTOMessage<StringCommand>(new StringCommand(strMessage));
        message.getHeader().setSource(src);
        ps.handle(message);
        Assert.assertEquals(lastProcessedData.getPayload(), strMessage);
        Assert.assertNotNull(lastReplyOnCommand);
        Assert.assertEquals(lastReplyOnCommand.getPayload(), "REPLY_" + strMessage);
        Assert.assertNotNull(lastReply);
        Assert.assertEquals(lastReply.getHeader().getSource(), src);
        Assert.assertEquals(lastReply.getData().getPayload(), "REPLY_" + strMessage);
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
        Assert.assertNotNull(scenario1Data);
        Assert.assertEquals(scenario1DataForwarded.getPayload(), "FORWARD_" + scenario1Data.getPayload());
        Assert.assertNotNull(requestSink);
        Assert.assertEquals(scenario1_post.getData(), scenario1DataForwarded);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertNotNull(lastProcessedData);
        String progress;
        Assert.assertEquals(lastProcessedData.getPayload(), progress = "FORWARD_" + scenario1Data.getPayload());
        Assert.assertEquals(secondProcessroLevelRep.getPayload(), progress = "TRANSFORM_" + progress);
        Assert.assertNotNull(secondProcessroLevelRep);
        Assert.assertNotNull(secondLevelReplyDTO);
        Assert.assertEquals(secondLevelReplyDTO.getHeader().getSource(), "firstLevel");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertNotNull(scenario1ListenerGot);
        Assert.assertNotNull(finalTwoLevelResult);
        Assert.assertEquals(finalTwoLevelResult.getHeader().getSource(),src);


    }

}