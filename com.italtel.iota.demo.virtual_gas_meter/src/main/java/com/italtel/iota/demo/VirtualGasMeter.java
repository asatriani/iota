package com.italtel.iota.demo;

import java.util.List;

public class VirtualGasMeter {

    private String name;
    private double measure;
    private double battery_level;
    private String geohash;
    private List<String> alerting_message;

    public VirtualGasMeter(String name, double measure, double battery_level, String geohash,
            List<String> alerting_message) {

        this.name = name;
        this.measure = measure;
        this.battery_level = battery_level;
        this.geohash = geohash;
        this.alerting_message = alerting_message;
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

    public double getBattery_level() {
        return battery_level;
    }

    public void setBattery_level(double battery_level) {
        this.battery_level = battery_level;
    }

    public String getGeohash() {
        return geohash;
    }

    public void setGeohash(String geohash) {
        this.geohash = geohash;
    }

    public List<String> getAlerting_message() {
        return alerting_message;
    }

    public void setAlerting_message(List<String> alerting_message) {
        this.alerting_message = alerting_message;
    }

    @Override
    public String toString() {
        return "VirtualGasMeter [name=" + name + ", measure=" + measure + ", battery_level=" + battery_level
                + ", geohash=" + geohash + ", alerting_message=" + alerting_message + "]";
    }

}
