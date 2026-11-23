package com.example.airquality;

public class Reading {
    public final long timestamp;
    public final float gas;          // MQ-2 gas / smoke index (ppm-equivalent)

    public Reading(long timestamp, float gas) {
        this.timestamp = timestamp;
        this.gas = gas;
    }

    public float value(Param p) {
        return gas;
    }

    public Level overall() {
        return Param.GAS.classify(gas);
    }
}