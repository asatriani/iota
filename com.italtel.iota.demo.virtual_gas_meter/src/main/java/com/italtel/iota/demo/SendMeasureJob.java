package com.italtel.iota.demo;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendMeasureJob implements Job {

    public static final Logger s_logger = LoggerFactory.getLogger(SendMeasureJob.class);

    private VirtualGasMeterGateway virtualGasMeterGateway;

    private Random m_random;

    public SendMeasureJob() {
        m_random = new Random();
    }

    public void setVirtualGasMeterGateway(VirtualGasMeterGateway virtualGasMeterGateway) {
        this.virtualGasMeterGateway = virtualGasMeterGateway;
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        synchronized (virtualGasMeterGateway) {
            Map<String, Object> m_properties = virtualGasMeterGateway.getProperties();

            // fetch the publishing configuration from the publishing properties
            Double maxConsumption = (Double) m_properties.get(VirtualGasMeterGateway.MAX_CONSUMPTION_PROP_NAME);
            Double maxBatteryConsumption = (Double) m_properties
                    .get(VirtualGasMeterGateway.MAX_BATTERY_LEVEL_CONSUMPTION_PROP_NAME);
            String topic = (String) m_properties.get(VirtualGasMeterGateway.PUBLISH_TOPIC_PROP_NAME);
            Integer qos = (Integer) m_properties.get(VirtualGasMeterGateway.PUBLISH_QOS_PROP_NAME);
            Boolean retain = (Boolean) m_properties.get(VirtualGasMeterGateway.PUBLISH_RETAIN_PROP_NAME);

            Long currentTimestamp = new Date().getTime();
            for (Entry<String, VirtualGasMeter> entry : virtualGasMeterGateway.getMeters().entrySet()) {
                String meterName = entry.getKey();
                VirtualGasMeter meter = entry.getValue();

                double consumption = round(m_random.nextDouble() * maxConsumption, 2);
                double measure = meter.getMeasure() + consumption;
                meter.setMeasure(measure);

                double batteryComsumption = round(m_random.nextDouble() * maxBatteryConsumption, 2);
                double batteryLevel = meter.getBatteryLevel() - batteryComsumption;
                meter.setBatteryLevel(batteryLevel);

                StringBuilder b = new StringBuilder("{").append("\"timestamp\": ").append(currentTimestamp)
                        .append(", \"meter\": \"").append(meterName).append("\", \"geohash\": \"")
                        .append(meter.getGeohash()).append("\", \"measure\": ").append(measure)
                        .append(", \"battery\": ").append(batteryLevel).append(", \"alertingMessages\": [");
                Set<String> alertingMessages = meter.getAlertingMessages();
                if (alertingMessages != null && !alertingMessages.isEmpty()) {
                    for (Iterator<String> iterator = alertingMessages.iterator(); iterator.hasNext();) {
                        b.append("\"").append(iterator.next()).append("\"");
                        if (iterator.hasNext()) {
                            b.append(", ");
                        }
                    }
                }
                b.append("] }");
                String content = b.toString();

                String destTopic = new StringBuilder(meterName).append("/").append(topic).toString();

                // Publish the message
                try {
                    virtualGasMeterGateway.getCloudClient().publish(destTopic, content.getBytes(), qos, retain, 5);
                    s_logger.info("Published message to {} for {}", destTopic, meterName);
                } catch (Exception e) {
                    s_logger.error("Cannot publish on topic {} for {}: {}", destTopic, meterName, e.getMessage(), e);
                }

            }

        }

    }

    private double round(double value, int decNum) {
        double temp = Math.pow(10, decNum);
        return Math.round(value * temp) / temp;
    }

}