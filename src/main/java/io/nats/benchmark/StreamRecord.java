package io.nats.benchmark;

import java.io.Serializable;
import java.time.Instant;

public class StreamRecord implements Serializable {
    private Instant creation;

    private double posX;
    private double posY;
    private double posZ;

    private double vX;
    private double vY;

    public StreamRecord(double posX, double posY, double posZ, double vX, double vY, double vZ, double rX, double rY, double rZ, double estimatedWeight) {
        this.creation = Instant.now();
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.vX = vX;
        this.vY = vY;
        this.vZ = vZ;
        this.rX = rX;
        this.rY = rY;
        this.rZ = rZ;
        this.estimatedWeight = estimatedWeight;
    }

    private double vZ;

    private double rX;
    private double rY;
    private double rZ;

    private double estimatedWeight;


    public StreamRecord() {

    };
    public Instant getCreation() {
        return creation;
    }
}
