package io.nats.hello;

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
import java.util.Date;

public class StreamWriter
{
    public static void main( String[] args )
    {
        int nbMessages = 5;

        for (int argi=0 ; argi < args.length ; argi++) {
            if ( "-n".contentEquals(args[argi]) ) {
                argi ++;
                nbMessages=Integer.parseInt(args[argi]);
            }
        }


        try (Connection nc = Nats.connect("nats://localhost:4222")) {
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

            for (int iMsg = 0 ; iMsg < nbMessages ; iMsg++) {
                Date date = new Date();
                System.out.println();

                String payload = String.format("Message #%d sent at %s.", iMsg, formatter.format(date));
                js.publish("flow", payload.getBytes());

            }

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
