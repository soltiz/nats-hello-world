package io.nats.hello;

// Author: C. Van Frachem
// Derivated from java-nats-examples/hello-world

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.SlidingTimeWindowArrayReservoir;
import com.codahale.metrics.Snapshot;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamManagement;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import io.nats.client.api.StreamInfo;
import io.nats.client.support.JsonUtils;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Histogram;
import io.nats.client.*;

public class StreamReader
{
    public static void main( String[] args )
    {
        boolean isVerbose = false;
        boolean isPullMode = false;
        Duration initialWaitSeconds = Duration.ofSeconds(300);
        Duration finalWaitSeconds = Duration.ofSeconds(5);
        Duration pullWaitMs = Duration.ofMillis(5);
        int metricsReservoirDurationSeconds = 1200;

        String server = "localhost:4222";
        Integer batchSize = null;
        for (int argi=0 ; argi < args.length ; argi++) {
            String arg=args[argi];
            if ( "--pull".contentEquals(arg) ) {
                isPullMode = true;
                argi ++;
                batchSize=Integer.parseInt(args[argi]);
            } else if ( "-s".contentEquals(arg) ) {
                argi ++;
                server=args[argi];
            } else if ( "-v".contentEquals(arg)) {
                isVerbose = true;
            } else {
                System.err.println(String.format("Unexpected parameter '%s'.",arg));
                System.exit(1);
            }
        }


//        responseSizes.update(response.getContent().length);

        try (Connection nc = Nats.connect("nats://" + server )) {
            JetStreamManagement jsm = nc.jetStreamManagement();



            JetStream js = nc.jetStream();


            List<String> names = jsm.getStreamNames();
            if (names.size()==0) {
                System.out.println("Warning: no existing stream.");
            } else {
                System.out.println("Existing Streams:");
                for (String name : names) {
                    System.out.println("  - " + name);
                }
            }

            JetStreamSubscription sub;

            ConsumerConfiguration config = ConsumerConfiguration.builder()
                    // .deliverPolicy(DeliverPolicy.New)
                    .build();

            if (isPullMode) {
                System.out.println("PULL consumer mode");
                // Build our subscription options.
                PullSubscribeOptions pullOptions = PullSubscribeOptions.builder()
                        .durable("pull-client")
                        .configuration(config)
                        .build();

                sub = js.subscribe("flow",  pullOptions);
                sub.pull(batchSize);

            } else {
                System.out.println("PUSH consumer mode");

                // Build our subscription options.


                PushSubscribeOptions pushOptions = PushSubscribeOptions.builder()
                        .durable("push-client")
                        .configuration(config)
                        .build();


                sub = js.subscribe("flow", "common-queue", pushOptions);
            }
            while (true) {
                final Histogram latencies = new Histogram(new SlidingTimeWindowArrayReservoir(metricsReservoirDurationSeconds, TimeUnit.SECONDS));

                System.out.println();
                System.out.println("Waiting for available message/batch...");
                System.out.println();
                Message m = sub.nextMessage(initialWaitSeconds);
                Instant startTime = Instant.now();
                System.out.println(
                        String.format("Starting at %s. Processing all available messages until no message available...",
                                startTime.toString()));
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
                    if (isPullMode) {
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
                System.out.println(String.format("No more available messages after %s.", deserializedInstant.toString()));
                System.out.println(String.format("Messages actual reception timeframe: %f seconds", receptionDurationMs/1000.0  ));
                System.out.println(String.format("Number of retrieved messages: %d", latencies.getCount()));
                System.out.println(String.format("Average retrieval speed: %d messages/second", throughput.intValue()));
                System.out.println(String.format("Number of samples in histogram reservoir: %d", ms.size()));

                System.out.println(String.format("Min latency: %d ms", ms.getMin() / 1000000));
                System.out.println(String.format("Average end-to-end latency: %f ms", ms.getMean() / 1000000));
                System.out.println(String.format("Max latency: %d ms", ms.getMax() / 1000000));
                System.out.println(String.format("Standard deviation of end-to-end latency: %f ms", ms.getStdDev() / 1000000));

            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
