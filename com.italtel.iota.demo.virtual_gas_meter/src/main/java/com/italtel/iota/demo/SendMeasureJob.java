package com.italtel.iota.demo;

import java.util.Date;
import java.util.Map;

import org.eclipse.kura.message.KuraPayload;
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

            Date currentDate = new Date();
            int meterNumber = virtualGasMeter.getMeasures().size();
            for (int i = 0; i < meterNumber; i++) {
                String nameMeter = VirtualGasMeter.METER_PREFIX_NAME + i;
                double comsumption = virtualGasMeter.getRandom().nextDouble() * 4;
                double measure = virtualGasMeter.getMeasures().get(nameMeter) + comsumption;
                virtualGasMeter.getMeasures().put(nameMeter, measure);

                String finalTopic = new StringBuilder(nameMeter).append("/").append(topic).toString();

                // Allocate a new payload
                KuraPayload payload = new KuraPayload();

                // Timestamp the message
                payload.setTimestamp(currentDate);

                // Add metric to the payload
                payload.addMetric("value", measure);
                payload.addMetric("meter", nameMeter);

                byte[] body = payload.getBody();
                String meterBody = new String(body);
                s_logger.info("Published meterBody {}", meterBody);

                // Publish the message
                try {
                    virtualGasMeter.getCloudClient().publish(finalTopic, payload, qos, retain);
                    s_logger.info("Published message to {} for {}", finalTopic, nameMeter);
                } catch (Exception e) {
                    s_logger.error("Cannot publish on topic {} for {}: {}", finalTopic, nameMeter, e.getMessage(), e);
                }

            }
        }
    }

}