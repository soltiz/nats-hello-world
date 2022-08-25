package io.nats.benchmark;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.time.Duration;

import io.nats.benchmark.events.ReadSessionStartEvent;
import io.nats.benchmark.events.TestEvent;
import io.nats.benchmark.events.WriteSessionEndEvent;
import io.nats.benchmark.events.WriteSessionStartEvent;
import io.nats.benchmark.types.SyncMode;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamManagement;
import io.nats.client.Nats;
import io.nats.client.api.PublishAck;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import io.nats.client.api.StreamInfo;
import io.nats.client.support.JsonUtils;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class StreamWriter
{
    static void checkFutureAcks(List<CompletableFuture<PublishAck>> futures) throws ExecutionException, InterruptedException {
        while (futures.size() > 0) {
            List<CompletableFuture<PublishAck>> notDone = new ArrayList<>((int)futures.size());
            for (CompletableFuture<PublishAck> f : futures) {
                if (!f.isDone()) {
                    notDone.add(f);
                }
                else if (f.isCompletedExceptionally()) {
                    throw new RuntimeException("Got exception : " + f.get().getError());
                }
                else {
                    // handle the completed ack
                    PublishAck pa = f.get(); // this call blocks if it's not done, but we know it's done because we checked
                    if (pa.hasError()) {
                        throw new RuntimeException("Got error : " + f.get().getError());
                    }
                }
            }
            if (notDone.size() > 0 ) {
                Thread.sleep(1);
            }
            futures = notDone;
        }
    }


    private static void argError(String message) {
        System.err.println(getUsage());
        System.err.println("FATAL ERROR: " + message);
        System.exit(5);
    }


    private static String getUsage() {
        final String usage = "" +
                " Options: \n" +
                "\n" +
                "   -s <host:[port]>                    alternate NATS API server (default is localhost:4222) \n" +
                "   --flow   <flowName>                 the flow into which to write (default is 'testflow'). \n" +
                "                              It must be associated to a pre-created stream in the NATS cluster \n" +
                "   -n <msgCount[,msgCount[...]]>       number of messages to send in each successive sending session(s). Default is 100 messages (one session only). \n" +
                "   --interval-between-sessions <nbSeconds>     interval in seconds between sending sessions (if more than one). Default is 10 seconds. \n" +
                "   --async-batches  <batch size>      Instead of sending and waiting for server ack at each message \n" +
                "                                      activates asynchronous mode, checking acks only after a batch \n" +
                "                                      of messages has been sent. \n" +
                "   --max-rate <nb msgs / s>           Will limit write speed to this number of messages per second (default = unlimited)\n" +
                "   --test-id   <id string>            Allow to customize testId field in json output, for traceability \n"+
                "                                      purposes (default is 'test')  \n" +
                "   --json                              activates json output (1 line per document) to ease post-processing/indexing of events and metrics \n";

        return usage;
    }
    public static void main( String[] args )
    {
        List<Integer> sessionsMessagesCounts = new ArrayList<Integer> ();
        sessionsMessagesCounts.add(100);
        String server = "localhost:4222";
        String flow = "testflow";
        boolean isEmissionActive = true;
        SyncMode syncMode = SyncMode.SYNC;
        boolean isJsonOutput = false;
        String testId = "test";
        int intervalBetweenSessionsSeconds = 10;
        double maxMsgsRatePerMs = 0;
        int asyncBatchSize = 1000;



        for (int argi = 0; argi < args.length ; argi++) {
            String arg=args[argi];
            switch (arg) {
                case "--help":
                    System.out.println(getUsage());
                    System.exit(0);
                case "-n":
                    argi ++;
                    try {
                        String messagesCountsString = args[argi];
                        sessionsMessagesCounts= new ArrayList<>();
                        for (String countString: messagesCountsString.split(",")) {
                            sessionsMessagesCounts.add(Integer.parseInt(countString));
                        }
                    } catch (Exception e){
                        argError("Expecting comma-separated list of integer argument after '-n'  (e.g. -n 10000 or -n 10,100,1000 ).");
                    }
                    break;
                case "--interval-between-sessions":
                    argi ++;
                    try {
                        intervalBetweenSessionsSeconds=Integer.parseInt(args[argi]);
                    } catch  (Exception e){
                        argError("Expecting integer argument (seconds) after '--interval-between-sessions'.");
                    }
                    break;

                case "-s":
                    argi ++;
                    try {
                        server = args[argi];
                    } catch (Exception e) {
                        argError("Expecting <server[:port]> argument after '-s'.");
                    }
                    break;
                case "--flow":
                    argi++;
                    try {
                        flow = args[argi];
                    } catch (Exception e) {
                        argError("Expecting <flowName> argument after '--flow'.");
                    }
                    break;
                    case "--nosend":
                    isEmissionActive = false;
                    break;
                case "--async-batches":
                    argi ++;
                    syncMode = SyncMode.ASYNC;
                    try {
                        asyncBatchSize=Integer.parseInt(args[argi]);
                    } catch  (Exception e){
                        argError("Expecting integer argument after '--async-batches'.");
                    }
                    break;
                case "--max-rate":
                    argi ++;
                    try {
                        maxMsgsRatePerMs = Integer.parseInt(args[argi]) / 1000.0;
                    } catch  (Exception e){
                        argError("Expecting integer argument after '--max-rate'.");
                    }
                    break;
                case "--json":
                    isJsonOutput = true;
                    break;
                case "--test-id":
                    argi++;
                    try {
                        testId = args[argi];
                    } catch (Exception e) {
                        argError("Expecting <testId> argument after '--test-id'.");
                    }
                    break;
                default:
                    argError("Unsupported argument: '" + arg + "'");
                    break;
            }

        }


        TestEvent.initContext(flow, testId);

        try (Connection nc = Nats.connect("nats://" + server)) {

            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

            JetStream js = nc.jetStream();


            int sendingSessionIndex = 0;
            for ( int nbMessages : sessionsMessagesCounts) {
                sendingSessionIndex++;
                if (sendingSessionIndex != 1) {
                    Thread.sleep(intervalBetweenSessionsSeconds * 1000);
                }


                Instant startTime = Instant.now();

                if (isJsonOutput) {
                    System.out.println((new WriteSessionStartEvent(
                            testId,
                            sendingSessionIndex,
                            startTime,
                            syncMode,
                            nbMessages,
                            flow
                    )).toJson());
                } else {
                    System.out.println(
                            String.format("Starting at %s - sending messages...",
                                    startTime.toString()));
                }

                StreamRecord record = null;
                List<CompletableFuture<PublishAck>> futures = new ArrayList<>();


                for (int iMsg = 0; iMsg < nbMessages; iMsg++) {
                    Date date = new Date();
                    record = new StreamRecord(1839.000754, 25649.23456, 1182.123456, -67.18346, 12.3456, -0.0001874, 4.567, 8.910, 0.001234, 14156.1);

                    // If a maximum messages rate is specified, wait if needed
                    if (maxMsgsRatePerMs > 0 ) {
                        // We always compute from the start time, to ensure overall average
                        Instant doNotSendBefore = startTime.plus( (long)(iMsg / maxMsgsRatePerMs), ChronoUnit.MILLIS);

                        if (doNotSendBefore.isAfter(record.getCreation())) {
                            long waitTimeMs = Math.max (1, Duration.between(record.getCreation(), doNotSendBefore).toMillis());
                            Thread.sleep(waitTimeMs);
                            record.updateCreationTime();
                        }
                    }
                    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                         ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                        oos.writeObject(record);
                        if (isEmissionActive) {
                            if (syncMode == SyncMode.SYNC) {
                                js.publish(flow, bos.toByteArray());
                            } else {
                                futures.add(js.publishAsync(flow, bos.toByteArray()));
                                if (futures.size() >= asyncBatchSize) {
                                    checkFutureAcks(futures);
                                    futures = new ArrayList<>();
                                }
                            }

                        }
                    } catch (Exception e) {
                        throw (e);
                    }


                }
                checkFutureAcks(futures);

                Instant lastEventTime = record.getCreation();
                Double emissionDurationSeconds = Duration.between(startTime, lastEventTime).toMillis() / 1000.0;
                Double throughput = nbMessages / emissionDurationSeconds;

                if (isJsonOutput) {
                    System.out.println((new WriteSessionEndEvent(
                            testId,
                            sendingSessionIndex,
                            syncMode,
                            lastEventTime,
                            nbMessages,
                            emissionDurationSeconds,
                            throughput.intValue()
                    )).toJson());
                } else {


                    System.out.println(String.format("Finished sending messages at %s. Sent %d objects in %f seconds, which is about %d messages/second",
                            lastEventTime.toString(),
                            nbMessages,
                            emissionDurationSeconds,
                            throughput.intValue()
                    ));
                }
            }

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
