package com.italtel.iota.demo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Alert {

    private long timestamp;
    private String meter;
    private String geohash;
    private String alertMsg;
    private boolean closed;

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
    public String getAlertMsg() {
        return alertMsg;
    }

    public void setAlertMsg(String alertMsg) {
        this.alertMsg = alertMsg;
    }

    @JsonProperty
    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Alert [timestamp=").append(timestamp).append(", meter=").append(meter).append(", geohash=")
                .append(geohash).append(", alertMsg=").append(alertMsg).append(", closed=").append(closed).append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((alertMsg == null) ? 0 : alertMsg.hashCode());
        result = prime * result + (closed ? 1231 : 1237);
        result = prime * result + ((geohash == null) ? 0 : geohash.hashCode());
        result = prime * result + ((meter == null) ? 0 : meter.hashCode());
        result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Alert other = (Alert) obj;
        if (alertMsg == null) {
            if (other.alertMsg != null)
                return false;
        } else if (!alertMsg.equals(other.alertMsg))
            return false;
        if (closed != other.closed)
            return false;
        if (geohash == null) {
            if (other.geohash != null)
                return false;
        } else if (!geohash.equals(other.geohash))
            return false;
        if (meter == null) {
            if (other.meter != null)
                return false;
        } else if (!meter.equals(other.meter))
            return false;
        if (timestamp != other.timestamp)
            return false;
        return true;
    }

}
