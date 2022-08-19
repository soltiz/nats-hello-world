package io.nats.benchmark.events;

import java.time.Duration;
import java.time.Instant;

import io.nats.benchmark.types.SyncMode;

public class WriteSessionEndEvent extends TestEvent{
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

    public WriteSessionEndEvent(String testId, SyncMode syncMode, Instant lastEventTime, int nbMessages, Double emissionDurationSeconds, int msgRate) {
        super(testId);
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
}
