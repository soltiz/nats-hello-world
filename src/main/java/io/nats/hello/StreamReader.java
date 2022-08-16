package io.nats.hello;

// Author: C. Van Frachem
// Derivated from java-nats-examples/hello-world

import java.time.Duration;

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
        int initialWaitSeconds = 300;

        Integer batchSize = null;
        for (int argi=0 ; argi < args.length ; argi++) {
            String arg=args[argi];
            if ( "--pull".contentEquals(arg) ) {
                isPullMode = true;
                argi ++;
                batchSize=Integer.parseInt(args[argi]);
            } else if ( "-v".contentEquals(arg)) {
                isVerbose = true;
            } else {
                System.err.println(String.format("Unexpected parameter '%s'.",arg));
                System.exit(1);
            }
        }

        final MetricRegistry metrics = new MetricRegistry();

        final Histogram latencies = metrics.histogram("overall-latency");

//        responseSizes.update(response.getContent().length);

        try (Connection nc = Nats.connect("nats://localhost:4222")) {
            JetStreamManagement jsm = nc.jetStreamManagement();


            JetStream js = nc.jetStream();

            JetStreamSubscription sub;

            ConsumerConfiguration config = ConsumerConfiguration.builder()
                    .deliverPolicy(DeliverPolicy.New)
                    .build();

            if (isPullMode) {
                System.out.println("PULL consumer mode");
                // Build our subscription options.
                PullSubscribeOptions pullOptions = PullSubscribeOptions.builder()
                        .durable("pull-client")
                        .configuration(config)
                        .build();

                sub = js.subscribe("flow", pullOptions);
                sub.pull(batchSize);

            } else {
                System.out.println("PUSH consumer mode");

                // Build our subscription options.


                PushSubscribeOptions pushOptions = PushSubscribeOptions.builder()
                        .durable("push-client")
                        .configuration(config)
                        .build();


                sub = js.subscribe("flow", pushOptions);
            }
            System.out.println("Waiting for first available message/batch...");
            Message m = sub.nextMessage(Duration.ofSeconds(initialWaitSeconds));
            System.out.println("Processing all available messages until no message available...");

            while ( m != null) {
                latencies.update(60);
                if (isVerbose) {
                    System.out.println("Message: " + m.getSubject() + " " + new String(m.getData()));
                    JsonUtils.printFormatted(m.metaData());
                }
                m.ack();
                if (isPullMode) {
                    m = sub.nextMessage(Duration.ofMillis(5));
                    sub.pull(batchSize);
                    if (m==null) {
                        m = sub.nextMessage(Duration.ofSeconds(5));
                    }
                } else {
                    m = sub.nextMessage(Duration.ofSeconds(5));
                }
            }

            Snapshot ms = latencies.getSnapshot();
            System.out.println(String.format("Number of retrieved messages: %d", ms.size()));
            System.out.println(String.format("Min latency: %d ms", ms.getMin()));
            System.out.println(String.format("Max latency: %d ms", ms.getMax()));
            System.out.println(String.format("Average latency: %f ms", ms.getMean()));

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
