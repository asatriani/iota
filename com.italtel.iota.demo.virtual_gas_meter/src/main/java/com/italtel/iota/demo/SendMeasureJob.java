package com.italtel.iota.demo;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendMeasureJob implements Job {

    private static final Logger s_logger = LoggerFactory.getLogger(SendMeasureJob.class);

    private VirtualGasMeterGateway virtualGasMeterGateway;

    public void setVirtualGasMeterGateway(VirtualGasMeterGateway virtualGasMeterGateway) {
        this.virtualGasMeterGateway = virtualGasMeterGateway;
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long currentTimestamp = context.getScheduledFireTime().getTime();
        synchronized (virtualGasMeterGateway) {
            if (!virtualGasMeterGateway.getMeters().isEmpty()) {
                for (VirtualGasMeter vgm : virtualGasMeterGateway.getMeters().values()) {
                    vgm.sendMetricMessage(currentTimestamp);
                }
                // Store meters in file
                DataStoreUtils.storeMeterMapInFile(virtualGasMeterGateway.getMeters());
            } else {
                s_logger.warn("Virtual meter set is empty: nothing to do!");
            }
        }
    }

}