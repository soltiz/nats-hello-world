package io.nats.benchmark.events;

import java.time.Instant;

import io.nats.benchmark.types.PullMode;

public class ReadSessionEndEvent extends TestEvent {
    Instant startTime;
    Instant lastReceptionTime;
    int listeningSession;
    PullMode pullMode;
    String pullerId;
    Integer pullBatchSize;

    @Override
    public String getEventType() {
        return "read_session_end";
    }
    public String getStartTime() {
        return startTime.toString();
    }

    public String getLastReceptionTime() {
        return lastReceptionTime.toString();
    }

    public int getListeningSession() {
        return listeningSession;
    }

    public PullMode getPullMode() {
        return pullMode;
    }

    public String getPullerId() {
        return pullerId;
    }

    public Integer getPullBatchSize() {
        return pullBatchSize;
    }

    public long getNbMessages() {
        return nbMessages;
    }

    public int getMsgRate() {
        return msgRate;
    }

    public double getReceptionDurationSeconds() {
        return receptionDurationSeconds;
    }

    public long getMinLatencyMs() {
        return minLatencyMs;
    }

    public long getMaxLatencyMs() {
        return maxLatencyMs;
    }

    public double getAvgLatencyMs() {
        return avgLatencyMs;
    }

    public double getStdDeviationLatencyMs() {
        return stdDeviationLatencyMs;
    }

    public int getMetrics_samples_size() {
        return metrics_samples_size;
    }

    long nbMessages;
    int msgRate;
    double receptionDurationSeconds;
    long minLatencyMs;
    long maxLatencyMs;
    double avgLatencyMs;
    double stdDeviationLatencyMs;
    int metrics_samples_size;

    public ReadSessionEndEvent(
            Instant startTime,
            Instant lastReceptionTime,
            int listeningSession,
            PullMode pullMode, String pullerId,
            String testId, Integer pullBatchSize,
            long nbMessages,
            double receptionDurationSeconds,
            int msgRate,
            long minLatencyMs,
            long maxLatencyMs,
            double avgLatencyMs,
            double stdDeviationLatencyMs,
            int metrics_samples_size) {
        super(testId);
        this.startTime = startTime;
        this.lastReceptionTime = lastReceptionTime;
        this.listeningSession = listeningSession;
        this.pullMode = pullMode;
        this.pullerId = pullerId;
        this.pullBatchSize = pullBatchSize;
        this.nbMessages = nbMessages;
        this.receptionDurationSeconds = receptionDurationSeconds;
        this.msgRate = msgRate;
        this.minLatencyMs = minLatencyMs;
        this.maxLatencyMs = maxLatencyMs;
        this.avgLatencyMs = avgLatencyMs;
        this.stdDeviationLatencyMs = stdDeviationLatencyMs;
        this.metrics_samples_size = metrics_samples_size;
    }


}
