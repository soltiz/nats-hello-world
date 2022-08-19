package io.nats.benchmark.events;

import java.time.Instant;

import io.nats.benchmark.types.SyncMode;
import io.nats.client.support.JsonSerializable;

public class WriteSessionStartEvent extends TestEvent {

    Instant startTime;
    SyncMode syncMode;

    public WriteSessionStartEvent(String testId, Instant startTime, SyncMode syncMode, int nbMessages) {
        super(testId);
        this.startTime = startTime;
        this.syncMode = syncMode;
        this.nbMessages = nbMessages;
    }

    int nbMessages;

    public String getStartTime() {
        return startTime.toString();
    }



    @Override
    public String getEventType() {
        return "write_session_start";
    }
}
