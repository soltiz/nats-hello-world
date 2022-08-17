package io.nats.hello;

import java.io.Serializable;
import java.time.Instant;

public class StreamRecord implements Serializable {
    private Instant creation;
    private String timestamp;

    public StreamRecord() {
        creation = Instant.now();
    };

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Instant getCreation() {
        return creation;
    }
}
