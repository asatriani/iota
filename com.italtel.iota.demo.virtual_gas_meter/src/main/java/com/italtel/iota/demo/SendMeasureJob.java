package com.italtel.iota.demo;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

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
            String topic = (String) m_properties.get(VirtualGasMeterGateway.PUBLISH_TOPIC_PROP_NAME);
            Integer qos = (Integer) m_properties.get(VirtualGasMeterGateway.PUBLISH_QOS_PROP_NAME);
            Boolean retain = (Boolean) m_properties.get(VirtualGasMeterGateway.PUBLISH_RETAIN_PROP_NAME);

            Long currentTimestamp = new Date().getTime();

            for (Entry<String, VirtualGasMeter> entry : virtualGasMeterGateway.getMeters().entrySet()) {
                String meterName = entry.getKey();
                VirtualGasMeter meter = entry.getValue();

                double comsumption = m_random.nextDouble() * 4;
                double measure = meter.getMeasure() + comsumption;
                meter.setMeasure(measure);

                String finalTopic = new StringBuilder(meterName).append("/").append(topic).toString();

                String content = new StringBuilder("{").append("\"timestamp\": ").append(currentTimestamp)
                        .append(", \"meter\": \"").append(meterName).append("\", \"measure\": ").append(measure)
                        .append("}").toString();

                // Publish the message
                try {
                    virtualGasMeterGateway.getCloudClient().publish(finalTopic, content.getBytes(), qos, retain, 5);
                    s_logger.info("Published message to {} for {}", finalTopic, meterName);
                } catch (Exception e) {
                    s_logger.error("Cannot publish on topic {} for {}: {}", finalTopic, meterName, e.getMessage(), e);
                }

            }

        }
    }

}