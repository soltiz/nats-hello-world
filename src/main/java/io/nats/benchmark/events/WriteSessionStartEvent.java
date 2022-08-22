package io.nats.benchmark.events;

import java.time.Instant;

import io.nats.benchmark.types.SyncMode;
import io.nats.client.support.JsonSerializable;

public class WriteSessionStartEvent extends TestEvent {

    public int getSessionIndex() {
        return sessionIndex;
    }

    private final int sessionIndex;
    Instant startTime;
    SyncMode syncMode;

    public SyncMode getSyncMode() {
        return syncMode;
    }

    public String getFlow() {
        return flow;
    }

    public int getNbMessages() {
        return nbMessages;
    }

    String flow;

    public WriteSessionStartEvent(String testId, int sessionIndex, Instant startTime, SyncMode syncMode, int nbMessages, String flow) {
        super(testId);
        this.sessionIndex = sessionIndex;
        this.startTime = startTime;
        this.syncMode = syncMode;
        this.nbMessages = nbMessages;
        this.flow = flow;
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
