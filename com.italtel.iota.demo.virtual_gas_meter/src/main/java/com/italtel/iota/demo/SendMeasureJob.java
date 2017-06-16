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
        Map<String, Object> m_properties = virtualGasMeter.getProperties();

        // fetch the publishing configuration from the publishing properties
        String topic = (String) m_properties.get(VirtualGasMeter.PUBLISH_TOPIC_PROP_NAME);
        Integer qos = (Integer) m_properties.get(VirtualGasMeter.PUBLISH_QOS_PROP_NAME);
        Boolean retain = (Boolean) m_properties.get(VirtualGasMeter.PUBLISH_RETAIN_PROP_NAME);

        double comsumption = virtualGasMeter.getRandom().nextDouble() * 4;
        double m_measure = virtualGasMeter.getMeasure() + comsumption;
        virtualGasMeter.setMeasure(m_measure);

        // Allocate a new payload
        KuraPayload payload = new KuraPayload();

        // Timestamp the message
        payload.setTimestamp(new Date());

        // Add metric to the payload
        payload.addMetric("value", m_measure);

        // Publish the message
        try {
            virtualGasMeter.getCloudClient().publish(topic, payload, qos, retain);
            s_logger.info("Published to {} message: {}", topic, payload);
        } catch (Exception e) {
            s_logger.error("Cannot publish topic: " + topic, e);
        }

    }

}