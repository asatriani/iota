package com.italtel.iota.demo.virtual_gas_meter.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.Arrays;

/**
 * Created by satriani on 05/07/2017.
 */
public class Alert {

    private Long timestamp;
    private String meter;
    private String geohash;
    private String alertMsg;
    private Boolean closed;

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
    @NotNull
    public String getAlertMsg() {
        return alertMsg;
    }

    public void setAlertMsg(String alertMsg) {
        this.alertMsg = alertMsg;
    }

    @JsonProperty
    @NotNull
    public Boolean isClosed() {
        return closed;
    }

    public void setClosed(Boolean closed) {
        this.closed = closed;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Alert{");
        sb.append("timestamp=").append(timestamp);
        sb.append(", meter='").append(meter).append('\'');
        sb.append(", geohash='").append(geohash).append('\'');
        sb.append(", alertMsg='").append(alertMsg).append('\'');
        sb.append(", closed=").append(closed);
        sb.append('}');
        return sb.toString();
    }

    @JsonIgnore
    public boolean isValid() {
        if (timestamp == null || meter == null || geohash == null
                || alertMsg == null || closed == null) {
            return  false;
        }
        return true;
    }
}
