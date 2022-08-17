package io.nats.hello;

// Author: C. Van Frachem
// Derivated from java-nats-examples/hello-world

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.SlidingTimeWindowArrayReservoir;
import com.codahale.metrics.Snapshot;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamManagement;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.PullSubscribeOptions;
import io.nats.client.PushSubscribeOptions;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import io.nats.client.support.JsonUtils;

public class NatsTester {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Please provide a subcommand as first parameter of NatsTester.");
            System.exit(1);
        }
        String arg = args[0];
        String[] modifiedArgs = Arrays.copyOfRange(args, 1, args.length);

        try {
            switch (arg) {
                case "reader":
                    StreamReader.main(modifiedArgs);
                    break;
                case "writer":
                    StreamWriter.main(modifiedArgs);
                    break;
                default:
                    System.err.println("Unknown subcommand: '" + arg + "'");
                    System.exit(2);
                    break;
            }
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(99);
        }
    }
}
