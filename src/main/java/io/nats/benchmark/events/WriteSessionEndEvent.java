package io.nats.benchmark.events;

import java.time.Instant;

import io.nats.benchmark.types.SyncMode;

public class WriteSessionEndEvent extends TestEvent{
    private final int sessionIndex;
    SyncMode syncMode;
    Instant lastEventTime;
    int nbMessages;
    Double emissionDurationSeconds;
    int msgRate;

    public SyncMode getSyncMode() {
        return syncMode;
    }

    public String getLastEventTime() {
        return lastEventTime.toString();
    }

    public int getNbMessages() {
        return nbMessages;
    }

    public Double getEmissionDurationSeconds() {
        return emissionDurationSeconds;
    }

    public int getMsgRate() {
        return msgRate;
    }

    public WriteSessionEndEvent(String testId, int sessionIndex, SyncMode syncMode, Instant lastEventTime, int nbMessages, Double emissionDurationSeconds, int msgRate) {
        super();
        this.sessionIndex = sessionIndex;
        this.syncMode = syncMode;
        this.lastEventTime = lastEventTime;
        this.nbMessages = nbMessages;
        this.emissionDurationSeconds = emissionDurationSeconds;
        this.msgRate = msgRate;
    }

    @Override
    public String getEventType() {
        return "write_session_end";
    }

    public int getSessionIndex() {
        return sessionIndex;
    }
}
