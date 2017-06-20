package com.italtel.iota.demo;

import java.util.Date;
import java.util.Map;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendMeasureJob implements Job {

    public static final Logger s_logger = LoggerFactory.getLogger(SendMeasureJob.class);

    private VirtualGasMeter virtualGasMeter;

    public void setVirtualGasMeter(VirtualGasMeter virtualGasMeter) {
        this.virtualGasMeter = virtualGasMeter;
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        synchronized (virtualGasMeter) {
            Map<String, Object> m_properties = virtualGasMeter.getProperties();

            // fetch the publishing configuration from the publishing properties
            String topic = (String) m_properties.get(VirtualGasMeter.PUBLISH_TOPIC_PROP_NAME);
            Integer qos = (Integer) m_properties.get(VirtualGasMeter.PUBLISH_QOS_PROP_NAME);
            Boolean retain = (Boolean) m_properties.get(VirtualGasMeter.PUBLISH_RETAIN_PROP_NAME);

            Long currentTimestamp = new Date().getTime();
            int meterNumber = virtualGasMeter.getMeasures().size();
            for (int i = 0; i < meterNumber; i++) {
                String meterName = VirtualGasMeter.METER_PREFIX_NAME + i;
                double comsumption = virtualGasMeter.getRandom().nextDouble() * 4;
                double measure = virtualGasMeter.getMeasures().get(meterName) + comsumption;
                virtualGasMeter.getMeasures().put(meterName, measure);

                String finalTopic = new StringBuilder(meterName).append("/").append(topic).toString();

                String content = new StringBuilder("{").append("\"timestamp\": ").append(currentTimestamp)
                        .append(", \"meter\": \"").append(meterName).append("\", \"measure\": ").append(measure)
                        .append("}").toString();

                // Publish the message
                try {
                    virtualGasMeter.getCloudClient().publish(finalTopic, content.getBytes(), qos, retain, 5);
                    s_logger.info("Published message to {} for {}", finalTopic, meterName);
                } catch (Exception e) {
                    s_logger.error("Cannot publish on topic {} for {}: {}", finalTopic, meterName, e.getMessage(), e);
                }

            }
        }
    }

}