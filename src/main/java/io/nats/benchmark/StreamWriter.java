package io.nats.benchmark;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.time.Duration;

import io.nats.benchmark.events.ReadSessionStartEvent;
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
            futures = notDone;
        }
    }

    public static void main( String[] args )
    {
        int nbMessages = 5;
        String server = "localhost:4222";
        boolean isEmissionActive = true;
        SyncMode syncMode = SyncMode.SYNC;
        boolean isJsonOutput = false;
        String testId = "test";

        int asyncBatchSize = 1000;
        for (int argi = 0; argi < args.length ; argi++) {
            String arg=args[argi];
            switch (arg) {
                case "-n":
                    argi ++;
                    nbMessages=Integer.parseInt(args[argi]);
                    break;
                case "-s":
                    argi ++;
                    server=args[argi];
                    break;
                case "--nosend":
                    isEmissionActive = false;
                    break;
                case "--async-batches":
                    argi ++;
                    syncMode = SyncMode.ASYNC;
                    asyncBatchSize=Integer.parseInt(args[argi]);
                    break;
                case "--json":
                    isJsonOutput = true;
                    break;
                case "--test-id":
                    argi++;
                    testId = args[argi];
                    break;
                default:
                    System.err.println("Unsupported argument: '" + arg + "'");
                    System.exit(2);
                    break;
            }

        }




        try (Connection nc = Nats.connect("nats://" + server)) {
            JetStreamManagement jsm = nc.jetStreamManagement();

            // Build the configuration
            StreamConfiguration streamConfig = StreamConfiguration.builder()
                    .name("teststream")
                    .storageType(StorageType.Memory)
                    .subjects("flow")
                    .build();

            // Create the stream
            StreamInfo streamInfo = jsm.addStream(streamConfig);
            if (!isJsonOutput) {
                JsonUtils.printFormatted(streamInfo);
            }

            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

            JetStream js = nc.jetStream();
            Instant startTime = Instant.now();

            if (isJsonOutput) {
                System.out.println((new WriteSessionStartEvent(
                        testId,
                        startTime,
                        syncMode,
                        nbMessages
                )).toJson());
            } else {
                System.out.println(
                        String.format("Starting at %s - sending messages...",
                                startTime.toString()));
            }

            StreamRecord record = null;
            List<CompletableFuture<PublishAck>> futures = new ArrayList<>();

            for (int iMsg = 0 ; iMsg < nbMessages ; iMsg++) {
                Date date = new Date();
                record = new StreamRecord(1839.000754, 25649.23456, 1182.123456, -67.18346, 12.3456, -0.0001874, 4.567, 8.910, 0.001234, 14156.1);

                try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                     ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                    oos.writeObject(record);
                    if (isEmissionActive) {
                        if (syncMode == SyncMode.SYNC) {
                            js.publish("flow", bos.toByteArray());
                        } else
                        {
                            futures.add(js.publishAsync("flow", bos.toByteArray()));
                            if (futures.size() >= asyncBatchSize ) {
                              checkFutureAcks(futures);
                              futures = new ArrayList<>();
                            }
                        }

                    }
                } catch (Exception e) {
                    throw(e);
                }


            }
            checkFutureAcks(futures);

            Instant lastEventTime = record.getCreation();
            Double emissionDurationSeconds = Duration.between(startTime, lastEventTime).toMillis() / 1000.0;
            Double throughput = nbMessages / emissionDurationSeconds;

            if (isJsonOutput) {
                System.out.println((new WriteSessionEndEvent(
                        testId,
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
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
