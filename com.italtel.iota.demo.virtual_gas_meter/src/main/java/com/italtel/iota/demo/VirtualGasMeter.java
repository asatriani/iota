package com.italtel.iota.demo;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualGasMeter {

    private static final Logger s_logger = LoggerFactory.getLogger(VirtualGasMeter.class);

    private String name;
    private double measure;
    private double batteryLevel;
    private String geohash;
    private Set<String> alertingMessages;
    private final VirtualGasMeterGateway virtualGasMeterGateway;

    public VirtualGasMeter(String name, double measure, double batteryLevel, String geohash,
            Set<String> alertingMessages, VirtualGasMeterGateway virtualGasMeterGateway) {
        this.name = name;
        this.measure = measure;
        this.batteryLevel = batteryLevel;
        this.geohash = geohash;
        this.alertingMessages = alertingMessages;
        this.virtualGasMeterGateway = virtualGasMeterGateway;
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

    public Set<String> getAlertingMessages() {
        return alertingMessages;
    }

    public void setAlertingMessages(Set<String> alertingMessages) {
        this.alertingMessages = alertingMessages;
        sendAlertMessage(alertingMessages);
    }

    private void sendAlertMessage(Set<String> alertingMessages) {
        Map<String, Object> m_properties = virtualGasMeterGateway.getProperties();

        // fetch the publishing configuration from the publishing properties
        String topic = (String) m_properties.get(VirtualGasMeterGateway.PUBLISH_ALERT_TOPIC_PROP_NAME);
        Integer qos = (Integer) m_properties.get(VirtualGasMeterGateway.PUBLISH_QOS_PROP_NAME);
        Boolean retain = (Boolean) m_properties.get(VirtualGasMeterGateway.PUBLISH_RETAIN_PROP_NAME);

        StringBuilder b = new StringBuilder("{").append("\"timestamp\": ").append(new Date().getTime())
                .append(", \"meter\": \"").append(name).append("\", alertingMessages\": [");
        if (alertingMessages != null && !alertingMessages.isEmpty()) {
            for (Iterator<String> iterator = alertingMessages.iterator(); iterator.hasNext();) {
                b.append("\"").append(iterator.next()).append("\"");
                if (iterator.hasNext()) {
                    b.append(", ");
                }
            }
        }
        b.append("]}");
        String content = b.toString();

        String destTopic = new StringBuilder(name).append("/").append(topic).toString();

        // Publish the message
        try {
            virtualGasMeterGateway.getCloudClient().publish(destTopic, content.getBytes(), qos, retain, 5);
            s_logger.info("Published message to {} for {}", destTopic, name);
        } catch (Exception e) {
            s_logger.error("Cannot publish on topic {} for {}: {}", destTopic, name, e.getMessage(), e);
        }

    }

    @Override
    public String toString() {
        return "VirtualGasMeter [name=" + name + ", measure=" + measure + ", batteryLevel=" + batteryLevel
                + ", geohash=" + geohash + ", alertingMessages=" + alertingMessages + "]";
    }

}
