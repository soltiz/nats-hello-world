package io.nats.benchmark.events;

import java.time.Instant;

import io.nats.benchmark.types.PullMode;
import io.nats.client.support.JsonSerializable;

public class ReadSessionStartEvent extends TestEvent implements  JsonSerializable {
    Instant startTime;
    int listeningSession;
    PullMode pullMode;
    String pullerId;
    String testId;
    Integer batchSize;

    public ReadSessionStartEvent(String testId, Instant startTime, int listeningSession, PullMode pullMode, String pullerId, Integer batchSize) {
        super(testId);
        this.startTime = startTime;
        this.listeningSession = listeningSession;
        this.pullMode = pullMode;
        this.pullerId = pullerId;
        this.batchSize = batchSize;
    }

    @Override
    public String getEventType() {
        return "read_session_start";
    }
}
