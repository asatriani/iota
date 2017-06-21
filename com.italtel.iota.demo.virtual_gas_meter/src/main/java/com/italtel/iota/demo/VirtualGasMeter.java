package com.italtel.iota.demo;

import java.util.List;

public class VirtualGasMeter {

    private String name;
    private double measure;
    private double batteryLevel;
    private String geohash;
    private List<String> alertingMessage;

    public VirtualGasMeter(String name, double measure, double batteryLevel, String geohash,
            List<String> alertingMessage) {
        this.name = name;
        this.measure = measure;
        this.batteryLevel = batteryLevel;
        this.geohash = geohash;
        this.alertingMessage = alertingMessage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getMeasure() {
        return measure;
    }

    public void setMeasure(double measure) {
        this.measure = measure;
    }

    public double getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(double batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public String getGeohash() {
        return geohash;
    }

    public void setGeohash(String geohash) {
        this.geohash = geohash;
    }

    public List<String> getAlertingMessage() {
        return alertingMessage;
    }

    public void setAlertingMessage(List<String> alertingMessage) {
        this.alertingMessage = alertingMessage;
    }

    @Override
    public String toString() {
        return "VirtualGasMeter [name=" + name + ", measure=" + measure + ", batteryLevel=" + batteryLevel
                + ", geohash=" + geohash + ", alertingMessage=" + alertingMessage + "]";
    }

}
