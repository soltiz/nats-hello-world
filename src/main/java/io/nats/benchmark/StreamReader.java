package io.nats.benchmark;

// Author: C. Van Frachem
// Derivated from java-nats-examples/hello-world

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.SlidingTimeWindowArrayReservoir;
import com.codahale.metrics.Snapshot;
import io.nats.benchmark.events.ReadSessionStartEvent;
import io.nats.benchmark.events.ReadSessionEndEvent;
import io.nats.benchmark.types.PullMode;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamManagement;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import io.nats.client.api.StreamInfo;
import io.nats.client.support.JsonUtils;
import com.codahale.metrics.Histogram;
import io.nats.client.*;
import java.util.UUID;

public class StreamReader
{
    ;

    public static void main( String[] args )
    {


        boolean isVerbose = false;
        String flow = "testflow";
        PullMode pullMode = PullMode.PUSH;
        boolean isJsonOutput = false;
        Integer maxListeningSessions = null;
        Duration initialWaitSeconds = Duration.ofSeconds(300);
        Duration finalWaitSeconds = Duration.ofSeconds(5);
        Duration pullWaitMs = Duration.ofMillis(5);
        int metricsReservoirDurationSeconds = 1200;
        String pullerId = UUID.randomUUID().toString();
        String testId = "test";

        String server = "localhost:4222";
        Integer batchSize = null;
        String clientId = null;
        for (int argi=0 ; argi < args.length ; argi++) {
            String arg=args[argi];
            switch (arg) {
                case "--pull":
                    pullMode = PullMode.PULL;
                    argi++;
                    batchSize = Integer.parseInt(args[argi]);
                    break;
                case "--flow":
                    argi++;
                    flow = args[argi];
                    break;
                case "--max-listening-sessions":
                    argi++;
                    maxListeningSessions = Integer.parseInt(args[argi]);
                    break;
                case "-s":
                    argi++;
                    server = args[argi];
                    break;
                case "-v":
                    isVerbose = true;
                    break;
                case "--json":
                    isJsonOutput = true;
                    break;
                case "--test-id":
                    argi++;
                    testId = args[argi];
                    break;
                case "--client-id":
                    argi++;
                    clientId = args[argi];
                    break;
                default:
                    System.err.println(String.format("Unexpected parameter '%s'.",arg));
                    System.exit(1);
            }
        }


//        responseSizes.update(response.getContent().length);

        try (Connection nc = Nats.connect("nats://" + server )) {
            JetStream js = nc.jetStream();
            JetStreamManagement jsm = nc.jetStreamManagement();
            jsm.getStreamNamesBySubjectFilter(flow);


            List<String> names = jsm.getStreamNamesBySubjectFilter(flow);
            if (!isJsonOutput) {

                if (names.size() == 0) {
                    System.out.println("Warning: no existing stream for '" + flow + "' flow.");
                    System.exit(4);
                } else {
                    System.out.println("Existing Streams for '" + flow + "' flow:");
                    for (String name : names) {
                        System.out.println("  - " + name);
                    }
                    if (isVerbose) {
                        for (String streamName : names) {
                            StreamInfo streamInfo = jsm.getStreamInfo(streamName);
                            JsonUtils.printFormatted(streamInfo);
                        }
                    }
                }
            }
            JetStreamSubscription sub;

            ConsumerConfiguration config = ConsumerConfiguration.builder()
                    // .deliverPolicy(DeliverPolicy.New)
                    .build();

            if (! isJsonOutput) { System.out.println(pullMode.toString() + " consumer mode");}
            if (clientId == null) {
                clientId = pullMode.toString().toLowerCase() + "-client";
            }

            if (pullMode == PullMode.PULL) {
                // Build our subscription options.
                PullSubscribeOptions pullOptions = PullSubscribeOptions.builder()
                        .durable(clientId)
                        .configuration(config)
                        .build();

                sub = js.subscribe(flow,  pullOptions);
                sub.pull(batchSize);

            } else {

                // Build our subscription options.
                PushSubscribeOptions pushOptions = PushSubscribeOptions.builder()
                        .durable(clientId)
                        .configuration(config)
                        .build();


                sub = js.subscribe(flow, "common-queue", pushOptions);
            }
            int listeningSession = 0;
            while (maxListeningSessions == null || listeningSession < maxListeningSessions) {
                listeningSession ++;
                final Histogram latencies = new Histogram(new SlidingTimeWindowArrayReservoir(metricsReservoirDurationSeconds, TimeUnit.SECONDS));
                if (!isJsonOutput) {
                    System.out.println("Waiting for available message/batch...");
                    System.out.println();
                }
                Message m = sub.nextMessage(initialWaitSeconds);
                Instant startTime = Instant.now();
                if (isJsonOutput) {
                    System.out.println((new ReadSessionStartEvent(
                            testId,
                            startTime,
                            listeningSession,
                            pullMode,
                            pullerId,
                            batchSize
                    )).toJson());
                } else {
                    System.out.println(
                            String.format(
                                    "Starting at %s (mode =%s). Processing all available messages until no message available...",
                                    startTime.toString(),
                                    pullMode
                            )
                    );
                }
                Instant deserializedInstant = Instant.now();

                while (m != null) {
                    byte[] payload = m.getData();
                    try (ByteArrayInputStream bis = new ByteArrayInputStream(payload);
                         ObjectInputStream ois = new ObjectInputStream(bis)) {
                        StreamRecord record = (StreamRecord) ois.readObject();
                        deserializedInstant = Instant.now();

                        long overallLatency = Duration.between(record.getCreation(), deserializedInstant).toNanos();
                        latencies.update(overallLatency);
                        if (isVerbose) {
                            System.out.println(
                                    String.format(
                                            "Subject: \"%s\" latency(µs)=%d  Message sent at %s.",
                                            m.getSubject(),
                                            overallLatency / 1000,
                                            record.getCreation().toString()
                                    )
                            );
                            JsonUtils.printFormatted(m.metaData());
                        }
                    }


                    m.ack();
                    if (pullMode == PullMode.PULL) {
                        m = sub.nextMessage(pullWaitMs);
                        if (m == null) {
                            sub.pull(batchSize);
                            m = sub.nextMessage(finalWaitSeconds);
                        }
                    } else {
                        m = sub.nextMessage(finalWaitSeconds);
                    }
                }

                Snapshot ms = latencies.getSnapshot();
                long receptionDurationMs = Duration.between(startTime, deserializedInstant).toMillis();
                Double throughput = latencies.getCount() / (receptionDurationMs / 1000.0);
                double receptionTimeframeSeconds = receptionDurationMs / 1000.0;
                long minLatencyMs = ms.getMin() / 1000000;
                long maxLatencyMs = ms.getMax() / 1000000;
                double avgLatencyMs = ms.getMean() / 1000000.0;
                double stdDeviationLatencyMs = ms.getStdDev() / 1000000.0;
                if (isJsonOutput) {
                    System.out.println((new ReadSessionEndEvent(
                            startTime,
                            deserializedInstant,
                            listeningSession,
                            pullMode,
                            pullerId,
                            testId,
                            batchSize,
                            latencies.getCount(),
                            receptionTimeframeSeconds,
                            throughput.intValue(),
                            minLatencyMs,
                            maxLatencyMs,
                            avgLatencyMs,
                            stdDeviationLatencyMs,
                            ms.size()
                    )).toJson());
                } else {
                    System.out.println(String.format("No more available messages after %s.", deserializedInstant.toString()));
                    System.out.println(String.format("Messages actual reception timeframe: %f seconds", receptionTimeframeSeconds));
                    System.out.println(String.format("Number of retrieved messages: %d", latencies.getCount()));
                    System.out.println(String.format("Average retrieval speed: %d messages/second", throughput.intValue()));
                    System.out.println(String.format("Number of samples in histogram reservoir: %d", ms.size()));

                    System.out.println(String.format("Min latency: %d ms", minLatencyMs));
                    System.out.println(String.format("Average end-to-end latency: %f ms", avgLatencyMs));
                    System.out.println(String.format("Max latency: %d ms", maxLatencyMs));
                    System.out.println(String.format("Standard deviation of end-to-end latency: %f ms", stdDeviationLatencyMs));
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
