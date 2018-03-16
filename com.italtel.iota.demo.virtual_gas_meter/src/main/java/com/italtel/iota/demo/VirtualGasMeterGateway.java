package com.italtel.iota.demo;

import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class VirtualGasMeterGateway implements ConfigurableComponent, CloudClientListener {

    private static final Logger s_logger = LoggerFactory.getLogger(VirtualGasMeterGateway.class);

    // Application identifier
    private static final String APP_ID = "VirtualGasMeterGateway";
    public static final String METER_PREFIX_NAME = "Gas_Meter_";

    // Publishing Property Names
    public static final String PUBLISH_CRON_EXPR_PROP_NAME = "publish.cron.expr";
    public static final String PUBLISH_TOPIC_PROP_NAME = "publish.semanticTopic";
    public static final String PUBLISH_ALERT_TOPIC_PROP_NAME = "publish.alert.semanticTopic";
    public static final String PUBLISH_QOS_PROP_NAME = "publish.qos";
    public static final String PUBLISH_RETAIN_PROP_NAME = "publish.retain";

    public static final String INITIAL_METER_SIZE_PROP_NAME = "initial.virtual.meter.size";

    public static final String INITIAL_MEASURE_PROP_NAME = "initial.measure";
    public static final String MAX_CONSUMPTION_PROP_NAME = "max.consumption";

    public static final String INITIAL_BATTERY_LEVEL_PROP_NAME = "initial.battery.level";
    public static final String MAX_BATTERY_LEVEL_CONSUMPTION_PROP_NAME = "max.battery.level.consumption";
    public static final String LOW_BATTERY_LEVEL_PROP_NAME = "low.battery.level";
    public static final String AUTO_RELOAD_BATTERY_LEVEL_PROP_NAME = "auto.reload.battery.level";

    public static final String INITIAL_TEMEPERATURE_PROP_NAME = "initial.temperature";
    public static final String MAX_TEMEPERATURE_DEVIATION_PROP_NAME = "max.temperature.deviation";

    public static final String REFERENCE_LOCATION_PROP_NAME = "ref.location";

    private static final String CONTROL_TOPIC_PATCH = "PATCH";
    private static final String CONTROL_TOPIC_GET = "GET";
    private static final String CONTROL_TOPIC_DELETE = "DELETE";
    private static final String CONTROL_TOPIC_POST = "POST";
    private static final String CONTROL_TOPIC_MASSIVE_LOAD = "MASSIVE_LOAD";
    private static final String CONTROL_TOPIC_PING = "PING";

    private CloudService cloudService;
    private CloudClient cloudClient;

    private Map<String, Object> properties;
    private String cronExpr;
    private Float refLatitude;
    private Float refLongitude;
    private RandomGenerator randomGenerator;
    private JobDetail sendMeasureJob;
    private Trigger trigger;
    private Scheduler scheduler;

    private Map<String, VirtualGasMeter> meters;

    public VirtualGasMeterGateway() {
        randomGenerator = new RandomGenerator(this);
    }

    public void setCloudService(CloudService cloudService) {
        this.cloudService = cloudService;
    }

    public void unsetCloudService(CloudService cloudService) {
        this.cloudService = null;
    }

    public Map<String, VirtualGasMeter> getMeters() {
        return meters;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public CloudClient getCloudClient() {
        return cloudClient;
    }

    public RandomGenerator getRandomGenerator() {
        return randomGenerator;
    }

    public Float getRefLatitude() {
        return refLatitude;
    }

    public Float getRefLongitude() {
        return refLongitude;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        s_logger.info("Activating VirtualGasMeterGateway...");

        this.properties = properties;
        for (Entry<String, Object> e : properties.entrySet()) {
            s_logger.info("Activate - {}: {}", e.getKey(), e.getValue());
        }

        // get the mqtt client for this application
        try {
            // Acquire a Cloud Application Client for this Application
            s_logger.info("Getting CloudClient for {}...", APP_ID);
            cloudClient = cloudService.newCloudClient(APP_ID);
            cloudClient.addCloudClientListener(this);

            JobDataMap jobDM = new JobDataMap();
            jobDM.put("virtualGasMeterGateway", this);
            sendMeasureJob = JobBuilder.newJob(SendMeasureJob.class).setJobData(jobDM)
                    .withIdentity("sendMeasureJob", "group").build();

            scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.start();

            // Restore meters from file
            meters = DataStoreUtils.restoreMeterMapFromFile(this);

            doUpdate(false);
            s_logger.info("Activating VirtualGasMeterGateway... Done.");
        } catch (Exception e) {
            s_logger.error("Error during VirtualGasMeterGateway activation", e);
            throw new ComponentException(e);
        }
    }

    protected void deactivate(ComponentContext componentContext) {
        synchronized (this) {
            s_logger.debug("Deactivating VirtualGasMeterGateway...");

            // Store meters in file
            DataStoreUtils.storeMeterMapInFile(meters);
            meters.clear();

            try {
                scheduler.shutdown();
            } catch (SchedulerException e) {
                s_logger.error("Error during scheduler shutdown", e.getMessage());
            }

            s_logger.info("Releasing Cloud Client for {}...", APP_ID);
            cloudClient.release();

            s_logger.debug("Deactivating VirtualGasMeterGateway... Done.");
        }
    }

    public void updated(Map<String, Object> properties) {
        s_logger.info("Updated VirtualGasMeterGateway...");

        // store the properties received
        this.properties = properties;
        for (Entry<String, Object> e : properties.entrySet()) {
            s_logger.info("Update - {}: {}", e.getKey(), e.getValue());
        }

        doUpdate(true);
        s_logger.info("Update VirtualGasMeterGateway...Done.");
    }

    @Override
    public void onControlMessageArrived(String deviceId, String controlTopic, KuraPayload msg, int qos,
            boolean retain) {
        KuraRequestPayload request;
        if (!(msg instanceof KuraRequestPayload)) {
            try {
                request = KuraRequestPayload.buildFromKuraPayload(msg);
            } catch (ParseException e) {
                s_logger.warn("Unexpected control message: {}. Discard it!", e.getMessage());
                return;
            }
        } else {
            request = (KuraRequestPayload) msg;
        }

        s_logger.info("Control message arrived to {} on {} from {}", deviceId, controlTopic,
                request.getRequesterClientId());

        KuraResponsePayload response = null;
        if (CONTROL_TOPIC_GET.equals(controlTopic)) {
            response = getMeter(msg);
        } else if (CONTROL_TOPIC_PATCH.equals(controlTopic)) {
            response = updateMeter(msg);
        } else if (CONTROL_TOPIC_POST.equals(controlTopic)) {
            response = createMeter(msg);
        } else if (CONTROL_TOPIC_DELETE.equals(controlTopic)) {
            response = deleteMeter(msg);
        } else if (CONTROL_TOPIC_MASSIVE_LOAD.equals(controlTopic)) {
            response = massiveLoad(msg);
        } else if (CONTROL_TOPIC_PING.equals(controlTopic)) {
            response = ping();
        } else {
            s_logger.error("Unknown topic {}", controlTopic);
            response = new KuraResponsePayload(404);
        }

        String respApptopic = "REPLY/" + request.getRequestId();
        String respClientId = request.getRequesterClientId();
        try {
            cloudClient.controlPublish(respClientId, respApptopic, response, 0, false, 0);
            s_logger.info("Published response message on topic {} for {}", respApptopic, respClientId);
        } catch (Exception e) {
            s_logger.error("Cannot publish response message on topic {} for {}: {}", respApptopic, respClientId,
                    e.getMessage(), e);
        }
    }

    private KuraResponsePayload massiveLoad(KuraPayload msg) {
        KuraResponsePayload response;
        if (cronExpr == null) {
            String errMsg = "Invalid cron expression in config prop " + PUBLISH_CRON_EXPR_PROP_NAME;
            s_logger.error(errMsg);
            response = new KuraResponsePayload(400);
            response.setExceptionMessage(errMsg);
            return response;
        }
        if (refLatitude == null || refLongitude == null) {
            String errMsg = "Invalid coordinates in config prop " + REFERENCE_LOCATION_PROP_NAME;
            s_logger.error(errMsg);
            response = new KuraResponsePayload(400);
            response.setExceptionMessage(errMsg);
            return response;
        }
        Integer meterSize = (Integer) msg.getMetric("meter.size");
        if (meterSize == null || meterSize <= 0) {
            String errMsg = "Meter size parameter '" + meterSize + "' is invalid! It must be greater than 0";
            s_logger.error(errMsg);
            response = new KuraResponsePayload(400);
            response.setExceptionMessage(errMsg);
            return response;
        }
        String meterPrefixName = (String) msg.getMetric("meter.prefix.name");
        if (meterPrefixName == null || meterPrefixName.trim().length() == 0) {
            String errMsg = "Meter prefix name '" + meterPrefixName
                    + "' parameter is invalid! It must be not null or empty";
            s_logger.error(errMsg);
            response = new KuraResponsePayload(400);
            response.setExceptionMessage(errMsg);
        }
        Long startTimestamp = (Long) msg.getMetric("start.timestamp");
        if (startTimestamp == null || startTimestamp <= 0) {
            String errMsg = "Meter start timestamp '" + startTimestamp
                    + "' parameter is invalid! It must be greater than 0";
            s_logger.error(errMsg);
            response = new KuraResponsePayload(400);
            response.setExceptionMessage(errMsg);
        }

        Date startDate = new Date(startTimestamp);
        String meterID;
        VirtualGasMeter meter;
        for (int i = 0; i < meterSize; i++) {
            meterID = meterPrefixName + i;
            if (meters.get(meterID) != null) {
                s_logger.error("Meter {} already exists! Skip it", meterID);
                continue;
            }

            meter = new VirtualGasMeter(meterID, this, Optional.empty(), Optional.of(0D), Optional.of(100D),
                    Optional.of(15D));
            List<Date> fireTimes = CronUtils.getNextFireTimes(cronExpr, startDate, new Date());
            for (Date t : fireTimes) {
                meter.sendMetricMessage(t.getTime());
            }
            meters.put(meterID, meter);
            s_logger.trace("Successfully meter {} load complete", meterID);
        }

        response = new KuraResponsePayload(200);
        s_logger.info("Successfully massive load complete");
        return response;
    }

    private KuraResponsePayload ping() {
        KuraResponsePayload response;
        response = new KuraResponsePayload(200);
        response.setBody("PONG".getBytes());
        s_logger.info("Successfully response to PING");
        return response;
    }

    private KuraResponsePayload deleteMeter(KuraPayload msg) {
        KuraResponsePayload response;
        String meterID = (String) msg.getMetric("meter.id");
        if (meterID == null) {
            String errMsg = "Meter id parameter is null!";
            s_logger.error(errMsg);
            response = new KuraResponsePayload(400);
            response.setExceptionMessage(errMsg);
        } else {
            VirtualGasMeter currentVGM = meters.get(meterID);
            if (currentVGM == null) {
                String errMsg = "Meter " + meterID + " not found";
                s_logger.error(errMsg);
                response = new KuraResponsePayload(404);
                response.setExceptionMessage(errMsg);
            } else {
                try {
                    Set<String> activeAlertMsgs = currentVGM.getActiveAlertMsgs();
                    if (!activeAlertMsgs.isEmpty()) {
                        currentVGM.sendClearingCurrentAlertMessages();
                    }

                    ObjectMapper m = new ObjectMapper();
                    response = new KuraResponsePayload(200);
                    response.setBody(m.writeValueAsBytes(currentVGM));
                    meters.remove(meterID);
                    s_logger.info("Successfully deleting meter {}", meterID);
                } catch (Exception e) {
                    String errMsg = "Error deleting meter " + meterID + ": " + e.getMessage();
                    s_logger.error(errMsg, e);
                    response = new KuraResponsePayload(500);
                    response.setExceptionMessage(errMsg);
                }
            }
        }
        return response;
    }

    private KuraResponsePayload createMeter(KuraPayload msg) {
        KuraResponsePayload response;
        String meterID = (String) msg.getMetric("meter.id");
        if (meterID == null) {
            String errMsg = "Meter id parameter is null!";
            s_logger.error(errMsg);
            response = new KuraResponsePayload(400);
            response.setExceptionMessage(errMsg);
        } else {
            VirtualGasMeter currentVGM = meters.get(meterID);
            if (currentVGM != null) {
                String errMsg = "Meter " + meterID + " already exists";
                s_logger.error(errMsg);
                response = new KuraResponsePayload(400);
                response.setExceptionMessage(errMsg);
            } else {
                byte[] body = msg.getBody();
                if (body == null) {
                    String errMsg = "Body parameter is null!";
                    s_logger.error(errMsg);
                    response = new KuraResponsePayload(400);
                    response.setExceptionMessage(errMsg);
                } else {
                    try {
                        ObjectMapper m = new ObjectMapper();
                        VirtualGasMeter addVGM = m.readValue(msg.getBody(), VirtualGasMeter.class);
                        addVGM.setVirtualGasMeterGateway(this);
                        if (!meterID.equals(addVGM.getId())) {
                            String errMsg = "Unexpected specified meter!";
                            s_logger.error(errMsg);
                            response = new KuraResponsePayload(400);
                            response.setExceptionMessage(errMsg);
                        } else {
                            if (cronExpr != null) {
                                // Send metric message referred to previous metric fire time
                                long timestamp = CronUtils.getPreviousFireTime(cronExpr, new Date()).getTime();
                                addVGM.sendMetricMessage(timestamp);
                            }

                            // Check if send alert message in case of alert messages are not empty
                            if (!addVGM.getActiveAlertMsgs().isEmpty()) {
                                addVGM.sendCurrentAlertMessages();
                            }

                            meters.put(meterID, addVGM);
                            response = new KuraResponsePayload(200);
                            response.setBody(m.writeValueAsBytes(addVGM));
                        }
                        s_logger.info("Successfully adding meter {}", meterID);
                    } catch (Exception e) {
                        String errMsg = "Error adding meter " + meterID + ": " + e.getMessage();
                        s_logger.error(errMsg, e);
                        response = new KuraResponsePayload(500);
                        response.setExceptionMessage(errMsg);
                    }
                }
            }
        }
        return response;
    }

    private KuraResponsePayload updateMeter(KuraPayload msg) {
        KuraResponsePayload response;
        String meterID = (String) msg.getMetric("meter.id");
        if (meterID == null) {
            String errMsg = "Meter id parameter is null!";
            s_logger.error(errMsg);
            response = new KuraResponsePayload(400);
            response.setExceptionMessage(errMsg);
        } else {
            VirtualGasMeter currentVGM = meters.get(meterID);
            if (currentVGM == null) {
                String errMsg = "Meter " + meterID + " not found";
                s_logger.error(errMsg);
                response = new KuraResponsePayload(404);
                response.setExceptionMessage(errMsg);
            } else {
                byte[] body = msg.getBody();
                if (body == null) {
                    String errMsg = "Body parameter is null!";
                    s_logger.error(errMsg);
                    response = new KuraResponsePayload(400);
                    response.setExceptionMessage(errMsg);
                } else {
                    try {
                        ObjectMapper m = new ObjectMapper();
                        VirtualGasMeter updateVGM = m.readValue(msg.getBody(), VirtualGasMeter.class);
                        updateVGM.setVirtualGasMeterGateway(this);
                        if (!meterID.equals(updateVGM.getId())) {
                            String errMsg = "Unexpected specified meter!";
                            s_logger.error(errMsg);
                            response = new KuraResponsePayload(400);
                            response.setExceptionMessage(errMsg);
                        } else {
                            s_logger.info("currentVGM: {}", currentVGM);
                            s_logger.info("updateVGM: {}", updateVGM);

                            if (cronExpr != null && !currentVGM.getGeohash().equals(updateVGM.getGeohash())) {
                                s_logger.info("Updating last metric to change geohash of {}", meterID);
                                // Send metric message referred to previous metric fire time
                                long timestamp = CronUtils.getPreviousFireTime(cronExpr, new Date()).getTime();
                                updateVGM.sendMetricMessage(timestamp);
                            }

                            // Check if send alert message in case of status change
                            if (!currentVGM.getGeohash().equals(updateVGM.getGeohash())
                                    && !currentVGM.getActiveAlertMsgs().isEmpty()) {
                                s_logger.info("Clear old active alarm of {}", meterID);
                                // Clear old active alarm in case of geohash change
                                currentVGM.sendClearingCurrentAlertMessages();
                                updateVGM.sendCurrentAlertMessages();
                            } else if (!currentVGM.hasIdenticalActiveAlertMsgs(updateVGM)
                                    || (currentVGM.isOffline() && !updateVGM.isOffline())) {
                                // Send alarm (clearing and open) merging data
                                Set<String> currentActiveAlertMsgs = currentVGM.getActiveAlertMsgs();
                                if (!currentActiveAlertMsgs.isEmpty()) {
                                    s_logger.debug("Merging current and update alerts");
                                    Set<String> updateActiveAlertMsgs = updateVGM.getActiveAlertMsgs();
                                    Set<String> updateClearingAlertMsgs = new HashSet<>(currentActiveAlertMsgs);
                                    updateClearingAlertMsgs.removeAll(updateActiveAlertMsgs);
                                    if (!updateClearingAlertMsgs.isEmpty()) {
                                        s_logger.debug("Clearing alerts: {}", updateClearingAlertMsgs);
                                        updateVGM.sendAlertMessages(updateVGM.makeAlerts(updateClearingAlertMsgs,
                                                System.currentTimeMillis(), true), true);
                                    }
                                    updateActiveAlertMsgs.removeAll(currentActiveAlertMsgs);
                                    if (!updateActiveAlertMsgs.isEmpty()) {
                                        s_logger.debug("Open alerts: {}", updateActiveAlertMsgs);
                                        updateVGM.setActiveAlertMsgs(updateActiveAlertMsgs);
                                    }
                                }
                                updateVGM.sendCurrentAlertMessages();
                            }

                            meters.put(meterID, updateVGM);
                            response = new KuraResponsePayload(200);
                            response.setBody(m.writeValueAsBytes(updateVGM));
                            s_logger.info("Successfully updating meter {}", meterID);
                        }
                    } catch (Exception e) {
                        String errMsg = "Error updating meter " + meterID + ": " + e.getMessage();
                        s_logger.error(errMsg, e);
                        response = new KuraResponsePayload(500);
                        response.setExceptionMessage(errMsg);
                    }
                }
            }
        }
        return response;
    }

    private KuraResponsePayload getMeter(KuraPayload msg) {
        KuraResponsePayload response;
        String meterID = (String) msg.getMetric("meter.id");
        try {
            if (meterID == null) {
                // Get all meter
                boolean fullFetch = (Boolean) msg.getMetric("full.fetch");
                Object[] body;
                if (fullFetch) {
                    body = meters.values().toArray();
                } else {
                    body = meters.keySet().toArray();
                }
                response = new KuraResponsePayload(200);
                ObjectMapper m = new ObjectMapper();
                response.setBody(m.writeValueAsBytes(body));
                s_logger.info("Successfully reading all meter (full.fetch={})", fullFetch);
            } else {
                VirtualGasMeter virtualGasMeter = meters.get(meterID);
                if (virtualGasMeter == null) {
                    String errMsg = "Meter " + meterID + " not found";
                    s_logger.error(errMsg);
                    response = new KuraResponsePayload(404);
                    response.setExceptionMessage(errMsg);
                } else {
                    response = new KuraResponsePayload(200);
                    ObjectMapper m = new ObjectMapper();
                    response.setBody(m.writeValueAsBytes(virtualGasMeter));
                }
                s_logger.info("Successfully reading meter {}", meterID);
            }
        } catch (Exception e) {
            String errMsg = "Error reading meter " + meterID + ": " + e.getMessage();
            s_logger.error(errMsg, e);
            response = new KuraResponsePayload(500);
            response.setExceptionMessage(errMsg);
        }
        return response;
    }

    @Override
    public void onMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        // Nothing to do
    }

    @Override
    public void onConnectionLost() {
        // Nothing to do
    }

    @Override
    public void onConnectionEstablished() {
        // Nothing to do
    }

    @Override
    public void onMessageConfirmed(int messageId, String appTopic) {
        // Nothing to do
    }

    @Override
    public void onMessagePublished(int messageId, String appTopic) {
        // Nothing to do
    }

    private void doUpdate(boolean onUpdate) {
        synchronized (this) {
            String tempCronExpr = (String) this.properties.get(PUBLISH_CRON_EXPR_PROP_NAME);
            if (tempCronExpr == null) {
                s_logger.error("Update VirtualGasMeterGateway - Properties do not contain {}",
                        VirtualGasMeterGateway.PUBLISH_CRON_EXPR_PROP_NAME);
                return;
            }
            cronExpr = tempCronExpr;

            Double initialMeasure;
            if (!this.properties.containsKey(INITIAL_MEASURE_PROP_NAME)) {
                s_logger.error("Update VirtualGasMeterGateway - Properties do not contain {}",
                        VirtualGasMeterGateway.INITIAL_MEASURE_PROP_NAME);
                return;
            } else {
                initialMeasure = (Double) this.properties.get(INITIAL_MEASURE_PROP_NAME);
                if (initialMeasure < 0) {
                    s_logger.error("Update VirtualGasMeterGateway - Invalid {}. Must be positive",
                            VirtualGasMeterGateway.INITIAL_MEASURE_PROP_NAME);
                    return;
                }
            }

            Double initialBatteryLevel;
            if (!this.properties.containsKey(INITIAL_BATTERY_LEVEL_PROP_NAME)) {
                s_logger.error("Update VirtualGasMeterGateway - Properties do not contain {}",
                        VirtualGasMeterGateway.INITIAL_BATTERY_LEVEL_PROP_NAME);
                return;
            } else {
                initialBatteryLevel = (Double) this.properties.get(INITIAL_BATTERY_LEVEL_PROP_NAME);
                if (initialBatteryLevel < 0) {
                    s_logger.error("Update VirtualGasMeterGateway - Invalid {}. Must be positive",
                            VirtualGasMeterGateway.INITIAL_BATTERY_LEVEL_PROP_NAME);
                    return;
                }
            }

            if (!this.properties.containsKey(INITIAL_TEMEPERATURE_PROP_NAME)) {
                s_logger.error("Update VirtualGasMeterGateway - Properties do not contain {}",
                        VirtualGasMeterGateway.INITIAL_TEMEPERATURE_PROP_NAME);
                return;
            }

            if (!this.properties.containsKey(REFERENCE_LOCATION_PROP_NAME)) {
                s_logger.error("Update VirtualGasMeterGateway - Properties do not contain {}",
                        VirtualGasMeterGateway.REFERENCE_LOCATION_PROP_NAME);
                return;
            } else {
                String refLocation = (String) this.properties.get(REFERENCE_LOCATION_PROP_NAME);
                String[] locData = refLocation.split(" ");
                if (locData.length != 2) {
                    s_logger.error("Update VirtualGasMeterGateway - Invalid {}",
                            VirtualGasMeterGateway.REFERENCE_LOCATION_PROP_NAME);
                    return;
                }

                try {
                    refLatitude = Float.parseFloat(locData[0]);
                } catch (Exception e) {
                    s_logger.error("Update VirtualGasMeterGateway - Invalid latitude {}", locData[0]);
                    return;
                }
                try {
                    refLongitude = Float.parseFloat(locData[1]);
                } catch (Exception e) {
                    s_logger.error("Update VirtualGasMeterGateway - Invalid longitude {}", locData[1]);
                    return;
                }
            }

            // TODO Check other props

            if (!onUpdate && meters.isEmpty()) {
                int initialMeterSize;
                if (!this.properties.containsKey(INITIAL_METER_SIZE_PROP_NAME)) {
                    s_logger.error("Update VirtualGasMeterGateway - Ignore as properties do not contain {}",
                            VirtualGasMeterGateway.INITIAL_METER_SIZE_PROP_NAME);
                    return;
                } else {
                    initialMeterSize = (int) this.properties.get(INITIAL_METER_SIZE_PROP_NAME);
                    if (initialMeterSize < 0) {
                        s_logger.error("Update VirtualGasMeterGateway - Invalid {}. Must be equal or greater than 0",
                                VirtualGasMeterGateway.INITIAL_METER_SIZE_PROP_NAME);
                        return;
                    }
                }

                for (int i = 0; i < initialMeterSize; i++) {
                    try {
                        meters.put(METER_PREFIX_NAME + i, new VirtualGasMeter(METER_PREFIX_NAME + i, this));
                    } catch (Exception e) {
                        s_logger.error("Update VirtualGasMeterGateway - Error creating VirtualGasMeterGateway {}: {}",
                                METER_PREFIX_NAME + i, e.getMessage());
                    }
                }
            }

            if (trigger != null) {
                try {
                    scheduler.unscheduleJob(trigger.getKey());
                } catch (SchedulerException e) {
                    s_logger.error("Update VirtualGasMeterGateway - Error unscheduling old sender job", e);
                }
            }

            try {
                trigger = TriggerBuilder.newTrigger().withIdentity("trigger", "group")
                        .withSchedule(CronScheduleBuilder.cronSchedule(cronExpr)).build();
                scheduler.scheduleJob(sendMeasureJob, trigger);
            } catch (SchedulerException e) {
                s_logger.error("Update VirtualGasMeterGateway - Error scheduling sender job", e);
            }

        }

    }

}