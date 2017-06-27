/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package com.italtel.iota.demo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.message.KuraPayload;
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

import ch.hsr.geohash.GeoHash;

public class VirtualGasMeterGateway implements ConfigurableComponent, CloudClientListener {

    private static final Logger s_logger = LoggerFactory.getLogger(VirtualGasMeterGateway.class);

    // Cloud Application identifier
    private static final String APP_ID = "VirtualGasMeterGateway";

    public static final String METER_PREFIX_NAME = "Gas_Meter_";

    // Publishing Property Names
    public static final String PUBLISH_CRON_EXPR_PROP_NAME = "publish.cron.expr";
    public static final String PUBLISH_TOPIC_PROP_NAME = "publish.semanticTopic";
    public static final String PUBLISH_ALERT_TOPIC_PROP_NAME = "publish.alert.semanticTopic";
    public static final String PUBLISH_QOS_PROP_NAME = "publish.qos";
    public static final String PUBLISH_RETAIN_PROP_NAME = "publish.retain";

    public static final String METER_SIZE_PROP_NAME = "meter.size";

    public static final String INITIAL_MEASURE_PROP_NAME = "initial.measure";
    public static final String MAX_CONSUMPTION_PROP_NAME = "max.consumption";

    public static final String INITIAL_BATTERY_LEVEL_PROP_NAME = "initial.battery.level";
    public static final String MAX_BATTERY_LEVEL_CONSUMPTION_PROP_NAME = "max.battery.level.consumption";

    public static final String REFERENCE_LOCATION_PROP_NAME = "ref.location";

    public static final String ALERTING_MESSAGES_PROP_NAME = "alerting.messages";
    public static final String ALERTING_MESSAGES_AS_ARRAY_PROP_NAME = "alerting.messages.as.array";

    public static final String[] alertingMessages = new String[] { "Hardware fault detected", "Gas leak detected",
            "Tampering detected" };

    private CloudService m_cloudService;
    private CloudClient m_cloudClient;

    private Map<String, Object> m_properties;
    private Random m_random;
    private JobDetail sendMeasureJob;
    private Trigger trigger;
    private Scheduler scheduler;

    private Map<String, VirtualGasMeter> meters;

    public VirtualGasMeterGateway() {
        super();
        m_random = new Random();
        meters = new HashMap<>();
    }

    public void setCloudService(CloudService cloudService) {
        this.m_cloudService = cloudService;
    }

    public void unsetCloudService(CloudService cloudService) {
        this.m_cloudService = null;
    }

    public Map<String, VirtualGasMeter> getMeters() {
        return meters;
    }

    public Map<String, Object> getProperties() {
        return m_properties;
    }

    public Random getRandom() {
        return m_random;
    }

    public CloudClient getCloudClient() {
        return m_cloudClient;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        s_logger.info("Activating VirtualGasMeterGateway...");

        this.m_properties = properties;
        for (String s : properties.keySet()) {
            s_logger.info("Activate - " + s + ": " + properties.get(s));
        }

        // get the mqtt client for this application
        try {
            // Acquire a Cloud Application Client for this Application
            s_logger.info("Getting CloudClient for {}...", APP_ID);
            this.m_cloudClient = this.m_cloudService.newCloudClient(APP_ID);
            this.m_cloudClient.addCloudClientListener(this);

            JobDataMap jobDM = new JobDataMap();
            jobDM.put("virtualGasMeterGateway", this);
            sendMeasureJob = JobBuilder.newJob(SendMeasureJob.class).setJobData(jobDM)
                    .withIdentity("sendMeasureJob", "group").build();

            scheduler = new StdSchedulerFactory().getScheduler();

            scheduler.start();
            // Don't subscribe because these are handled by the default
            // subscriptions and we don't want to get messages twice
            doUpdate(false);
        } catch (Exception e) {
            s_logger.error("Error during component activation", e);
            throw new ComponentException(e);
        }

        s_logger.info("Activating VirtualGasMeterGateway... Done.");
    }

    protected void deactivate(ComponentContext componentContext) {
        s_logger.debug("Deactivating VirtualGasMeterGateway...");

        s_logger.info("Releasing Cloud Client for {}...", APP_ID);
        this.m_cloudClient.release();

        try {
            scheduler.shutdown();
        } catch (SchedulerException e) {
            s_logger.error("Error during scheduler shutdown", e);
        }

        s_logger.debug("Deactivating VirtualGasMeterGateway... Done.");
    }

    public void updated(Map<String, Object> properties) {
        s_logger.info("Updated VirtualGasMeterGateway...");

        // store the properties received
        this.m_properties = properties;
        for (String s : properties.keySet()) {
            s_logger.info("Update - " + s + ": " + properties.get(s));
        }

        // try to kick off a new job
        doUpdate(true);
        s_logger.info("Update VirtualGasMeterGateway...Done.");
    }

    @Override
    public void onControlMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        // s_logger.info("Control message arrived: {} on {} {} {} {}", deviceId, appTopic, msg, qos, retain);

    }

    @Override
    public void onMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
        // s_logger.info("Message arrived: {} on {} {} {} {}", deviceId, appTopic, msg, qos, retain);

    }

    @Override
    public void onConnectionLost() {
        // s_logger.warn("Connection lost");

    }

    @Override
    public void onConnectionEstablished() {
        // s_logger.info("Connection established");

    }

    @Override
    public void onMessageConfirmed(int messageId, String appTopic) {
        // s_logger.info("Message confirmed: {} on {}", messageId, appTopic);

    }

    @Override
    public void onMessagePublished(int messageId, String appTopic) {
        // s_logger.info("Message published: {} on {}", messageId, appTopic);

    }

    private void doUpdate(boolean onUpdate) {
        synchronized (this) {
            String cronExpr = (String) this.m_properties.get(PUBLISH_CRON_EXPR_PROP_NAME);
            if (cronExpr == null) {
                s_logger.error("Update VirtualGasMeterGateway - Ignore as properties do not contain {}",
                        VirtualGasMeterGateway.PUBLISH_CRON_EXPR_PROP_NAME);
                return;
            }

            int m_size;
            if (!this.m_properties.containsKey(METER_SIZE_PROP_NAME)) {
                s_logger.error("Update VirtualGasMeterGateway - Ignore as properties do not contain {}",
                        VirtualGasMeterGateway.METER_SIZE_PROP_NAME);
                return;
            } else {
                m_size = (int) this.m_properties.get(METER_SIZE_PROP_NAME);
                if (m_size < 1) {
                    s_logger.error("Invalid {}. Must be equal or greater than 1",
                            VirtualGasMeterGateway.METER_SIZE_PROP_NAME);
                    return;
                }
            }

            Double initialMeasure;
            if (!this.m_properties.containsKey(INITIAL_MEASURE_PROP_NAME)) {
                s_logger.error("Update VirtualGasMeterGateway - Ignore as properties do not contain {}",
                        VirtualGasMeterGateway.INITIAL_MEASURE_PROP_NAME);
                return;
            } else {
                initialMeasure = (Double) this.m_properties.get(INITIAL_MEASURE_PROP_NAME);
                if (initialMeasure < 0) {
                    s_logger.error("Invalid {}. Must be positive", VirtualGasMeterGateway.INITIAL_MEASURE_PROP_NAME);
                    return;
                }
            }

            Double initialBatteryLevel;
            if (!this.m_properties.containsKey(INITIAL_BATTERY_LEVEL_PROP_NAME)) {
                s_logger.error("Update VirtualGasMeterGateway - Ignore as properties do not contain {}",
                        VirtualGasMeterGateway.INITIAL_BATTERY_LEVEL_PROP_NAME);
                return;
            } else {
                initialBatteryLevel = (Double) this.m_properties.get(INITIAL_BATTERY_LEVEL_PROP_NAME);
                if (initialBatteryLevel < 0) {
                    s_logger.error("Invalid {}. Must be positive",
                            VirtualGasMeterGateway.INITIAL_BATTERY_LEVEL_PROP_NAME);
                    return;
                }
            }

            float lat;
            float lon;
            String refLocation = (String) this.m_properties.get(REFERENCE_LOCATION_PROP_NAME);
            if (refLocation == null) {
                s_logger.error("Update VirtualGasMeterGateway - Ignore as properties do not contain {}",
                        VirtualGasMeterGateway.REFERENCE_LOCATION_PROP_NAME);
                return;
            } else {
                String[] locData = refLocation.split(" ");
                if (locData.length != 2) {
                    s_logger.error("Update VirtualGasMeterGateway - Invalid {}",
                            VirtualGasMeterGateway.REFERENCE_LOCATION_PROP_NAME);
                    return;
                }

                try {
                    lat = Float.parseFloat(locData[0]);
                } catch (Exception e) {
                    s_logger.error("Update VirtualGasMeterGateway - Invalid latitude {}", locData[0]);
                    return;
                }
                try {
                    lon = Float.parseFloat(locData[1]);
                } catch (Exception e) {
                    s_logger.error("Update VirtualGasMeterGateway - Invalid longitude {}", locData[1]);
                    return;
                }
            }

            if (m_size < meters.size()) {
                for (int i = (meters.size() - 1); i >= m_size; i--) {
                    meters.remove(METER_PREFIX_NAME + i);
                }
            } else {
                float rLat, rLon;

                VirtualGasMeter meter;
                for (int i = 0; i < m_size; i++) {
                    meter = meters.get(METER_PREFIX_NAME + i);
                    if (meter == null) {
                        // increment lat and lon
                        rLat = lat + (float) ((m_random.nextFloat() * 0.03) * (m_random.nextBoolean() ? 1 : -1));
                        rLon = lon + (float) ((m_random.nextFloat() * 0.06) * (m_random.nextBoolean() ? 1 : -1));
                        String geohash = GeoHash.withCharacterPrecision(rLat, rLon, 9).toBase32();
                        meters.put(METER_PREFIX_NAME + i, new VirtualGasMeter(METER_PREFIX_NAME + i, geohash, this));
                    } else {
                        // Clear old meter alerting messages
                        meter.setAlertingMessages(null);
                    }
                }

            }

            String alertProp = (String) this.m_properties.get(ALERTING_MESSAGES_PROP_NAME);
            if (alertProp != null && alertProp.trim().length() > 0) {
                Pattern alertPattern = Pattern.compile("(\\d+)\\[([\\d ]+)\\]");
                String[] s = alertProp.split("\\|");
                for (String part : s) {
                    Matcher m = alertPattern.matcher(part);
                    if (!m.matches()) {
                        s_logger.error("Alerting config string '{}' is not valid because it must match regex: {}", part,
                                alertPattern.pattern());
                        continue;
                    }

                    String meterName = METER_PREFIX_NAME + m.group(1);
                    VirtualGasMeter vgm = meters.get(meterName);
                    if (vgm == null) {
                        s_logger.error(
                                "Alerting config string '{}' is not valid because it refences an invald virtual gas meter index: {}",
                                part, m.group(1));
                        continue;
                    }

                    Set<String> messages = new HashSet<>();
                    String[] alert = m.group(2).split(" ");
                    for (String a : alert) {
                        int alertIndex = Integer.parseInt(a);
                        if (alertIndex > alertingMessages.length - 1) {
                            s_logger.error(
                                    "Alerting config string '{}' is not valid because it refences an invald alerting message index: {}",
                                    part, a);
                            continue;
                        }
                        messages.add(alertingMessages[alertIndex]);
                    }
                    vgm.setAlertingMessages(messages);
                }
            }

            if (trigger != null) {
                try {
                    scheduler.unscheduleJob(trigger.getKey());
                } catch (SchedulerException e) {
                    s_logger.error("Error scheduling old sender job", e);
                }
            }

            trigger = TriggerBuilder.newTrigger().withIdentity("trigger", "group")
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpr)).build();

            // schedule a new worker based on the properties of the service
            try {
                scheduler.scheduleJob(sendMeasureJob, trigger);
            } catch (SchedulerException e) {
                s_logger.error("Error scheduling sender job", e);
            }
        }

    }

}