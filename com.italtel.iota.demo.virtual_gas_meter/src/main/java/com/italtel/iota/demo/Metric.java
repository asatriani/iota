package com.italtel.iota.demo;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Metric {

    private long timestamp;
    private String meter;
    private String geohash;
    private double measure;
    private double battery;
    private double temperature;
    private String[] alertMsgs;

    @JsonProperty
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @JsonProperty
    public String getMeter() {
        return meter;
    }

    public void setMeter(String meter) {
        this.meter = meter;
    }

    @JsonProperty
    public String getGeohash() {
        return geohash;
    }

    public void setGeohash(String geohash) {
        this.geohash = geohash;
    }

    @JsonProperty
    public double getMeasure() {
        return measure;
    }

    public void setMeasure(double measure) {
        this.measure = measure;
    }

    @JsonProperty
    public double getBattery() {
        return battery;
    }

    public void setBattery(double battery) {
        this.battery = battery;
    }

    @JsonProperty
    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    @JsonProperty("alerts")
    public String[] getAlertMsgs() {
        return alertMsgs;
    }

    public void setAlertMsgs(String[] alertMsgs) {
        this.alertMsgs = alertMsgs;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Metric [timestamp=").append(timestamp).append(", meter=").append(meter).append(", geohash=")
                .append(geohash).append(", measure=").append(measure).append(", battery=").append(battery)
                .append(", temperature=").append(temperature).append(", alertMsgs=").append(Arrays.toString(alertMsgs))
                .append("]");
        return builder.toString();
    }

}
