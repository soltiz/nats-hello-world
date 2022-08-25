package io.nats.benchmark.events;

import java.time.Instant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.support.JsonSerializable;

public abstract class TestEvent implements JsonSerializable {
    static String testId;
    static String flowName;

    public static void initContext(String flowName, String testId) {
        TestEvent.flowName = flowName;
        TestEvent.testId = testId;
    }


    public String getEventTs() {
        return eventTs.toString();
    }

    Instant eventTs;
    public TestEvent() {

        this.eventTs = Instant.now();
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

    public String getTestId() {
        return testId;
    }

    public String getFlowName() {
        return flowName;
    }

}

