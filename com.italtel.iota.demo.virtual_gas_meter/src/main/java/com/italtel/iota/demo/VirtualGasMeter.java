package com.italtel.iota.demo;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.hsr.geohash.GeoHash;

public class VirtualGasMeter implements Serializable {

    private static final long serialVersionUID = -7548325806050930900L;

    private static final Logger s_logger = LoggerFactory.getLogger(VirtualGasMeter.class);

    private static final String LOCK_ALERT_MESSAGE = "Gas meter is locked";
    private static final String BATTERY_ALERT_MESSAGE = "Battery level is very low";

    private transient VirtualGasMeterGateway gw;

    private String id;
    private String coordinates;
    private transient String geohash;
    private double measure;
    private double battery;
    private double temperature;
    private boolean offline;
    private boolean locked;
    private Set<String> activeAlertMsgs;

    public VirtualGasMeter() {
    }

    public VirtualGasMeter(String id, VirtualGasMeterGateway gw) {
        this(id, gw, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public VirtualGasMeter(String id, VirtualGasMeterGateway gw, Optional<String> coordinates, Optional<Double> measure,
            Optional<Double> battery, Optional<Double> temperature) {
        this.gw = gw;
        this.id = id;
        this.coordinates = coordinates.orElse(gw.getRandomGenerator().getCoordinates());
        this.measure = measure
                .orElse((Double) gw.getProperties().get(VirtualGasMeterGateway.INITIAL_MEASURE_PROP_NAME));
        this.battery = battery
                .orElse((Double) gw.getProperties().get(VirtualGasMeterGateway.INITIAL_BATTERY_LEVEL_PROP_NAME));
        this.temperature = temperature
                .orElse((Double) gw.getProperties().get(VirtualGasMeterGateway.INITIAL_TEMEPERATURE_PROP_NAME));
    }

    @JsonIgnore
    public VirtualGasMeterGateway getVirtualGasMeterGateway() {
        return gw;
    }

    public void setVirtualGasMeterGateway(VirtualGasMeterGateway gw) {
        this.gw = gw;
    }

    @JsonProperty
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty
    public String getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(String coordinates) {
        if (coordinates == null || coordinates.trim().isEmpty()) {
            throw new RuntimeException("Coordinates are null or empty!");
        }

        String[] parts = coordinates.split(",");
        if (parts.length != 2) {
            throw new RuntimeException("Coordinates are not separated by ','");
        }

        float lat = Float.parseFloat(parts[0].trim());
        if (lat < -90 || lat > 90) {
            throw new RuntimeException("Latitude is invalid!");
        }

        float lon = Float.parseFloat(parts[1].trim());
        if (lon < -180 || lon > 180) {
            throw new RuntimeException("Longitude is invalid!");
        }

        this.coordinates = coordinates;
    }

    @JsonIgnore
    public String getGeohash() {
        if (geohash == null) {
            String[] parts = coordinates.split(",");
            return GeoHash
                    .withCharacterPrecision(Float.parseFloat(parts[0].trim()), Float.parseFloat(parts[1].trim()), 9)
                    .toBase32();
        }
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

    @JsonProperty
    public boolean isOffline() {
        return offline;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
    }

    @JsonProperty
    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    @JsonProperty("alerts")
    public Set<String> getActiveAlertMsgs() {
        Set<String> tmpAlertMsgs = new HashSet<>();
        if (activeAlertMsgs != null) {
            tmpAlertMsgs.addAll(activeAlertMsgs);
        }

        if (locked) {
            tmpAlertMsgs.add(LOCK_ALERT_MESSAGE);
        }

        if (battery < (Double) gw.getProperties().get(VirtualGasMeterGateway.LOW_BATTERY_LEVEL_PROP_NAME)) {
            tmpAlertMsgs.add(BATTERY_ALERT_MESSAGE);
        }

        return tmpAlertMsgs;
    }

    public void setActiveAlertMsgs(Set<String> activeAlertMsgs) {
        this.activeAlertMsgs = null;
        if (activeAlertMsgs != null) {
            this.activeAlertMsgs = new HashSet<>(activeAlertMsgs);
            this.activeAlertMsgs.remove(LOCK_ALERT_MESSAGE);
            this.activeAlertMsgs.remove(BATTERY_ALERT_MESSAGE);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("VirtualGasMeter [id=").append(id).append(", coordinates=").append(coordinates)
                .append(", geohash=").append(getGeohash()).append(", measure=").append(measure).append(", battery=")
                .append(battery).append(", temperature=").append(temperature).append(", offline=").append(offline)
                .append(", locked=").append(locked).append(", activeAlertMsgs=").append(getActiveAlertMsgs())
                .append("]");
        return builder.toString();
    }

    public boolean hasIdenticalActiveAlertMsgs(VirtualGasMeter other) {
        return getActiveAlertMsgs().equals(other.getActiveAlertMsgs());
    }

    public void sendMetricMessage(long timestamp) {
        synchronized (gw) {
            Map<String, Object> properties = gw.getProperties();

            if (!offline) {
                // Publish metric
                String parentTopic = (String) properties.get(VirtualGasMeterGateway.PUBLISH_TOPIC_PROP_NAME);
                String meterTopic = id + "/" + parentTopic;
                Integer qos = (Integer) properties.get(VirtualGasMeterGateway.PUBLISH_QOS_PROP_NAME);
                Boolean retain = (Boolean) properties.get(VirtualGasMeterGateway.PUBLISH_RETAIN_PROP_NAME);

                ObjectMapper mapper = new ObjectMapper();
                Metric metric = makeMetric(timestamp);
                try {
                    gw.getCloudClient().publish(meterTopic, mapper.writeValueAsBytes(metric), qos, retain, 5);
                    s_logger.info("Published metric message {} to {} for {}", metric, meterTopic, id);
                } catch (Exception e) {
                    s_logger.error("Cannot publish metric message {} on topic {} for {}: {}", metric, meterTopic, id,
                            e.getMessage(), e);
                }
            } else {
                s_logger.warn("No sent metric message because meter {} is offline", id);
            }

            // Update measure, battery level and temperature
            RandomGenerator randomGenerator = gw.getRandomGenerator();
            if (!locked) {
                measure = round(measure + randomGenerator.getConsumption(), 2);
            }

            battery = round(battery - randomGenerator.getBatteryConsumption(), 2);
            if (battery < 0) {
                battery = 0;
            }

            double autoReloadBatteryLevel = (Double) properties
                    .get(VirtualGasMeterGateway.AUTO_RELOAD_BATTERY_LEVEL_PROP_NAME);
            if (autoReloadBatteryLevel > 0 && battery < autoReloadBatteryLevel) {
                battery = (Double) properties.get(VirtualGasMeterGateway.INITIAL_BATTERY_LEVEL_PROP_NAME);
            }

            temperature = round(temperature + randomGenerator.getTemperatureDeviation(), 2);
        }
    }

    public void sendCurrentAlertMessages() {
        sendAlertMessages(makeAlerts(System.currentTimeMillis(), false), false);
    }

    public void sendClearingCurrentAlertMessages() {
        sendAlertMessages(makeAlerts(System.currentTimeMillis(), true), true);
    }

    public void sendAlertMessages(Set<Alert> alerts, boolean force) {
        synchronized (gw) {
            if (!force && offline) {
                s_logger.warn("No sent alert message because meter {} is offline", id);
                return;
            }

            Map<String, Object> properties = gw.getProperties();
            String parentTopic = (String) properties.get(VirtualGasMeterGateway.PUBLISH_ALERT_TOPIC_PROP_NAME);
            String meterTopic = id + "/" + parentTopic;
            Integer qos = (Integer) properties.get(VirtualGasMeterGateway.PUBLISH_QOS_PROP_NAME);
            Boolean retain = (Boolean) properties.get(VirtualGasMeterGateway.PUBLISH_RETAIN_PROP_NAME);

            // Publish alert
            ObjectMapper mapper = new ObjectMapper();
            for (Alert alert : alerts) {
                try {
                    gw.getCloudClient().publish(meterTopic, mapper.writeValueAsBytes(alert), qos, retain, 5);
                    s_logger.info("Published alert message {} to {} for {}", alert, meterTopic, id);
                } catch (Exception e) {
                    s_logger.error("Cannot publish alert message {} on topic {} for {}: {}", alert, meterTopic, id,
                            e.getMessage(), e);
                }
            }
        }
    }

    private Metric makeMetric(long timestamp) {
        Metric m = new Metric();
        m.setTimestamp(timestamp);
        m.setMeter(id);
        m.setGeohash(getGeohash());
        m.setMeasure(measure);
        m.setBattery(battery);
        m.setTemperature(temperature);
        Set<String> aMsgs = getActiveAlertMsgs();
        m.setAlertMsgs(aMsgs.toArray(new String[aMsgs.size()]));
        return m;
    }

    private Set<Alert> makeAlerts(long timestamp, boolean clearing) {
        return makeAlerts(getActiveAlertMsgs(), timestamp, clearing);
    }

    public Set<Alert> makeAlerts(Set<String> alertMsgs, long timestamp, boolean clearing) {
        Set<Alert> alerts = new HashSet<>();
        for (String aam : alertMsgs) {
            Alert a = new Alert();
            a.setTimestamp(timestamp);
            a.setMeter(id);
            a.setGeohash(getGeohash());
            a.setAlertMsg(aam);
            a.setClosed(clearing);
            alerts.add(a);
        }
        return alerts;
    }

    private double round(double value, int decNum) {
        double temp = Math.pow(10, decNum);
        return Math.round(value * temp) / temp;
    }
}
