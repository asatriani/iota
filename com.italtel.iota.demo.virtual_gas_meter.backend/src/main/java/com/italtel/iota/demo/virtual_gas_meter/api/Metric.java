package com.italtel.iota.demo.virtual_gas_meter.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.Arrays;

/**
 * Created by satriani on 05/07/2017.
 */
public class Metric {

    private Long timestamp;
    private String meter;
    private String geohash;
    private float measure;
    private float battery;
    private float temperature;
    private String[] alertMsgs;

    @JsonProperty
    @NotNull
    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @JsonProperty
    @NotNull
    public String getMeter() {
        return meter;
    }

    public void setMeter(String meter) {
        this.meter = meter;
    }

    @JsonProperty
    @NotNull
    public String getGeohash() {
        return geohash;
    }

    public void setGeohash(String geohash) {
        this.geohash = geohash;
    }

    @JsonProperty
    public float getMeasure() {
        return measure;
    }

    public void setMeasure(float measure) {
        this.measure = measure;
    }

    @JsonProperty
    public float getBattery() {
        return battery;
    }

    public void setBattery(float battery) {
        this.battery = battery;
    }

    @JsonProperty
    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
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
        final StringBuilder sb = new StringBuilder("Metric{");
        sb.append("timestamp=").append(timestamp);
        sb.append(", meter='").append(meter).append('\'');
        sb.append(", geohash='").append(geohash).append('\'');
        sb.append(", measure=").append(measure);
        sb.append(", battery=").append(battery);
        sb.append(", temperature=").append(temperature);
        sb.append(", alertMsgs=").append(Arrays.toString(alertMsgs));
        sb.append('}');
        return sb.toString();
    }

    @JsonIgnore
    public boolean isValid() {
        if (timestamp == null || meter == null || geohash == null) {
            return  false;
        }
        return true;
    }
}
