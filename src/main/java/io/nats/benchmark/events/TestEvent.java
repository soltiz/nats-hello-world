package io.nats.benchmark.events;

import java.time.Instant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.support.JsonSerializable;

public abstract class TestEvent implements JsonSerializable {
    String testId;

    public String getEventTs() {
        return eventTs.toString();
    }

    Instant eventTs;
    public TestEvent(String testId) {

        this.testId = testId;
        this.eventTs = Instant.now();
    }

    public String getTestId() {
        return testId;
    }

    public String toJson()  {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Json serialization error",e);
        }
    }

    public abstract String getEventType();
}

