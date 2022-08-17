package io.nats.hello;

// Author: C. Van Frachem
// Derivated from java-nats-examples/hello-world

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.*;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;


public class SerializationLatencyBench
{
    public static void main( String[] args ) throws IOException, ClassNotFoundException, InterruptedException {
        boolean isVerbose = false;
        int nbMessages = 1000;
        for (int argi=0 ; argi < args.length ; argi++) {
            String arg=args[argi];
            if ( "-n".contentEquals(arg) ) {
                argi ++;
                nbMessages=Integer.parseInt(args[argi]);
            } else if ( "-v".contentEquals(arg)) {
                isVerbose = true;
            } else {
                System.err.println(String.format("Unexpected parameter '%s'.",arg));
                System.exit(1);
            }
        }

        try {

            final Histogram latencies = new Histogram(new SlidingTimeWindowArrayReservoir(120, TimeUnit.SECONDS));
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

            long minl = 1000000;
            long maxl = 0;

            for (int iMsg = 0; iMsg < nbMessages; iMsg++) {
                Date date = new Date();
                StreamRecord record = new StreamRecord();
                String payload = String.format("Message #%d sent at %s.", iMsg, record.getCreation().toString());

                record.setTimestamp(payload);
                try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                     ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                    oos.writeObject(record);


                    byte[] payloadBytes = bos.toByteArray();
                    try (ByteArrayInputStream bis = new ByteArrayInputStream(payloadBytes);
                         ObjectInputStream ois = new ObjectInputStream(bis)) {
                        StreamRecord rec = (StreamRecord) ois.readObject();
                        Instant deserialized = Instant.now();
                        long latency = Duration.between(rec.getCreation(), deserialized).toNanos() / 1000;
                        if (latency > maxl) {
                            maxl = latency;
                        }
                        if (latency < minl) {
                            minl = latency;
                        }
                        if (isVerbose) {

                            System.out.println("Deserialized: " + rec.getTimestamp() + " overall latency=" + latency + "ns");

                            Instant a = Instant.now();
                            Instant b = Instant.now();
                            long xlat = Duration.between(a, b).toNanos() / 1000;
                            System.out.println("XLat=" + xlat + "ns");


                        }
                        if (iMsg > 200000) {
                            latencies.update(latency);
                        }
                    }
                }
            }
            Snapshot ms = latencies.getSnapshot();
            System.out.println(String.format("Number of serialized/deserialized messages: %d", latencies.getCount()));
            System.out.println(String.format("Number of samples in histogram reservoir: %d", ms.size()));
            System.out.println(String.format("Min latency: %d µs vs %d", ms.getMin(), minl));
            System.out.println(String.format("Max latency: %d µs vs %d ", ms.getMax(), maxl));
            System.out.println(String.format("Average latency: %f µs", ms.getMean()));
            System.out.println(String.format("latency stddev: %f µs", ms.getStdDev()));


        }         catch (Exception e) {
            e.printStackTrace();
        }
    }
}
