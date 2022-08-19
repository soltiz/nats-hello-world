package io.nats.benchmark;

// Author: C. Van Frachem
// Derivated from java-nats-examples/hello-world

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
import io.nats.benchmark.types.SyncMode;
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

public class StreamInit {
    public static void main(String[] args) throws IOException, InterruptedException, JetStreamApiException {
        String streamName = "teststream";
        String flow = "testflow";
        String server = "localhost:4222";
        Integer replicas = null;
        StorageType storageType = StorageType.File;
        boolean removePreviousStream = true;

        for (int argi = 0; argi < args.length; argi++) {
            String arg = args[argi];
            switch (arg) {
                case "-s":
                    argi++;
                    server = args[argi];
                    break;
                case "--flow":
                    argi++;
                    flow = args[argi];
                    break;
                case "--no-delete":
                    removePreviousStream = false;
                    break;
                case "--stream":
                    argi++;
                    streamName = args[argi];
                    break;
                case "--memory":
                    storageType = StorageType.Memory;
                    break;
                case "--replicas":
                    argi++;
                    streamName = args[argi];
                    break;
                default:
                    System.err.println("Unsupported argument: '" + arg + "'");
                    System.exit(2);
                    break;
            }

        }


        try (Connection nc = Nats.connect("nats://" + server)) {
            JetStreamManagement jsm = nc.jetStreamManagement();
            if (removePreviousStream && jsm.getStreamNames().contains(streamName)) {
                System.out.println("Deleting previous stream with same name...");
                jsm.deleteStream(streamName);
            }

            // Build the configuration
            StreamConfiguration.Builder configBuilder = StreamConfiguration.builder()
                    .name(streamName)
                    .storageType(storageType)
                    .subjects(flow);
            if (replicas != null) {
                configBuilder.replicas(replicas);
            }

            StreamConfiguration streamConfig = configBuilder.build();

            // Create the stream
            StreamInfo streamInfo = jsm.addStream(streamConfig);
            JsonUtils.printFormatted(streamInfo);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(99);
        }
        System.exit(0);
    }
}
