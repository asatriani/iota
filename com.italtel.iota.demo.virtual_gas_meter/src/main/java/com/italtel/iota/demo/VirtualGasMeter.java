package com.italtel.iota.demo;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualGasMeter {

    private static final Logger s_logger = LoggerFactory.getLogger(VirtualGasMeter.class);

    public static final String LOCK_ALERT_MESSAGE = "Gas Meter is locked";

    private final String name;
    private double measure;
    private double batteryLevel;
    private final String geohash;
    private Set<String> alertingMessages;
    private final VirtualGasMeterGateway virtualGasMeterGateway;
    private boolean lock;

    public VirtualGasMeter(String name, String geohash, VirtualGasMeterGateway virtualGasMeterGateway) {
        this.virtualGasMeterGateway = virtualGasMeterGateway;
        this.name = name;
        this.geohash = geohash;
        this.measure = (Double) virtualGasMeterGateway.getProperties()
                .get(VirtualGasMeterGateway.INITIAL_MEASURE_PROP_NAME);
        this.batteryLevel = (Double) virtualGasMeterGateway.getProperties()
                .get(VirtualGasMeterGateway.INITIAL_BATTERY_LEVEL_PROP_NAME);

    }

    public String getName() {
        return name;
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

    public Set<String> getAlertingMessages() {
        if (alertingMessages == null) {
            alertingMessages = new HashSet<String>();
        }
        return alertingMessages;
    }

    public boolean isLock() {
        return lock;
    }

    public void setLock(boolean lock) {
        this.lock = lock;
        if (lock) {
            getAlertingMessages().add(LOCK_ALERT_MESSAGE);
        } else {
            getAlertingMessages().remove(LOCK_ALERT_MESSAGE);
        }
        sendAlertMessage();
    }

    public void sendMetricMessage() {
        synchronized (virtualGasMeterGateway) {
            Map<String, Object> m_properties = virtualGasMeterGateway.getProperties();
            Random random = virtualGasMeterGateway.getRandom();

            // fetch the publishing configuration from the publishing properties
            Double maxConsumption = (Double) m_properties.get(VirtualGasMeterGateway.MAX_CONSUMPTION_PROP_NAME);
            Double maxBatteryConsumption = (Double) m_properties
                    .get(VirtualGasMeterGateway.MAX_BATTERY_LEVEL_CONSUMPTION_PROP_NAME);
            Boolean alertMsgAsArray = (Boolean) m_properties
                    .get(VirtualGasMeterGateway.ALERTING_MESSAGES_AS_ARRAY_PROP_NAME);
            String topic = (String) m_properties.get(VirtualGasMeterGateway.PUBLISH_TOPIC_PROP_NAME);
            Integer qos = (Integer) m_properties.get(VirtualGasMeterGateway.PUBLISH_QOS_PROP_NAME);
            Boolean retain = (Boolean) m_properties.get(VirtualGasMeterGateway.PUBLISH_RETAIN_PROP_NAME);

            Long currentTimestamp = new Date().getTime();

            if (!lock) {
                double consumption = random.nextDouble() * maxConsumption;
                measure = round(measure + consumption, 2);
            }

            double batteryComsumption = round(random.nextDouble() * maxBatteryConsumption, 2);
            batteryLevel = round(batteryLevel - batteryComsumption, 2);

            StringBuilder b = new StringBuilder("{").append("\"timestamp\": ").append(currentTimestamp)
                    .append(", \"meter\": \"").append(name).append("\", \"geohash\": \"").append(geohash)
                    .append("\", \"measure\": ").append(measure).append(", \"battery\": ").append(batteryLevel)
                    .append(", \"alertingCount\": ").append(alertingMessages != null ? alertingMessages.size() : 0)
                    .append(", \"alertingMessages\": ");
            b.append(alertMsgAsArray ? "[" : "\"");
            if (alertingMessages != null && !alertingMessages.isEmpty()) {
                for (Iterator<String> iterator = alertingMessages.iterator(); iterator.hasNext();) {
                    b.append(alertMsgAsArray ? "\"" : "").append(iterator.next()).append(alertMsgAsArray ? "\"" : "");
                    if (iterator.hasNext()) {
                        b.append(", ");
                    }
                }
            }
            b.append(alertMsgAsArray ? "]" : "\"");
            b.append(" }");

            String destTopic = new StringBuilder(name).append("/").append(topic).toString();

            // Publish the message
            try {
                virtualGasMeterGateway.getCloudClient().publish(destTopic, b.toString().getBytes(), qos, retain, 5);
                s_logger.info("Published message to {} for {}", destTopic, name);
            } catch (Exception e) {
                s_logger.error("Cannot publish on topic {} for {}: {}", destTopic, name, e.getMessage(), e);
            }

        }

    }

    private double round(double value, int decNum) {
        double temp = Math.pow(10, decNum);
        return Math.round(value * temp) / temp;
    }

    public void sendAlertMessage() {
        synchronized (virtualGasMeterGateway) {
            Map<String, Object> m_properties = virtualGasMeterGateway.getProperties();

            // fetch the publishing configuration from the publishing properties
            Boolean alertMsgAsArray = (Boolean) m_properties
                    .get(VirtualGasMeterGateway.ALERTING_MESSAGES_AS_ARRAY_PROP_NAME);
            String topic = (String) m_properties.get(VirtualGasMeterGateway.PUBLISH_ALERT_TOPIC_PROP_NAME);
            Integer qos = (Integer) m_properties.get(VirtualGasMeterGateway.PUBLISH_QOS_PROP_NAME);
            Boolean retain = (Boolean) m_properties.get(VirtualGasMeterGateway.PUBLISH_RETAIN_PROP_NAME);

            StringBuilder b = new StringBuilder("{").append("\"timestamp\": ").append(new Date().getTime())
                    .append(", \"meter\": \"").append(name).append("\", \"alertingCount\": ")
                    .append(alertingMessages != null ? alertingMessages.size() : 0).append(", \"alertingMessages\": ");
            b.append(alertMsgAsArray ? "[" : "\"");
            if (alertingMessages != null && !alertingMessages.isEmpty()) {
                for (Iterator<String> iterator = alertingMessages.iterator(); iterator.hasNext();) {
                    b.append(alertMsgAsArray ? "\"" : "").append(iterator.next()).append(alertMsgAsArray ? "\"" : "");
                    if (iterator.hasNext()) {
                        b.append(", ");
                    }
                }
            }
            b.append(alertMsgAsArray ? "]" : "\"");
            b.append(" }");

            String destTopic = new StringBuilder(name).append("/").append(topic).toString();

            // Publish the message
            try {
                virtualGasMeterGateway.getCloudClient().publish(destTopic, b.toString().getBytes(), qos, retain, 5);
                s_logger.info("Published message to {} for {}", destTopic, name);
            } catch (Exception e) {
                s_logger.error("Cannot publish on topic {} for {}: {}", destTopic, name, e.getMessage(), e);
            }
        }
    }

    @Override
    public String toString() {
        return "VirtualGasMeter [name=" + name + ", measure=" + measure + ", batteryLevel=" + batteryLevel
                + ", geohash=" + geohash + ", alertingMessages=" + alertingMessages + "]";
    }

}
