package io.nats.hello;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.time.Duration;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamManagement;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import io.nats.client.api.StreamInfo;
import io.nats.client.support.JsonUtils;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

public class StreamWriter
{
    public static void main( String[] args )
    {
        int nbMessages = 5;
        String server = "localhost:4222";
        boolean isEmissionActive = true;
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

            JsonUtils.printFormatted(streamInfo);

            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

            JetStream js = nc.jetStream();
            Instant startTime = Instant.now();
            System.out.println(
                    String.format("Starting at %s - sending messages...",
                            startTime.toString()));

            StreamRecord record = null;
            for (int iMsg = 0 ; iMsg < nbMessages ; iMsg++) {
                Date date = new Date();
                record = new StreamRecord(1839.000754, 25649.23456, 1182.123456, -67.18346, 12.3456, -0.0001874, 4.567, 8.910, 0.001234, 14156.1);

                try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                     ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                    oos.writeObject(record);
                    if (isEmissionActive) {
                        js.publish("flow", bos.toByteArray());
                    }
                } catch (Exception e) {
                    throw(e);
                }


            }
            Instant lastEventTime = record.getCreation();
            Double emissionDurationSeconds = Duration.between(startTime, lastEventTime).toMillis() / 1000.0;
            Double throughput = nbMessages / emissionDurationSeconds;
            System.out.println(String.format("Finished sending messages at %s. Sent %d objects in %f seconds, which is about %d messages/second",
                    lastEventTime.toString(),
                    nbMessages,
                    emissionDurationSeconds,
                    throughput.intValue()
                    ));

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
