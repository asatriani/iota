package com.italtel.iota.demo.virtual_gas_meter.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.italtel.iota.demo.virtual_gas_meter.api.Alert;
import com.italtel.iota.demo.virtual_gas_meter.api.Metric;
import io.dropwizard.lifecycle.Managed;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Created by satriani on 02/07/2017.
 */
public class GasMeterMqttDataCollector implements Managed, MqttCallbackExtended {

    private static final Logger log = LoggerFactory.getLogger(GasMeterMqttDataCollector.class);

    public static final String DATABASE_NAME = "gas_metering";
    public static final String RETANTION_POLICY_NAME = "5years";
    public static final String RETANTION_POLICY_DURATION = "1826d";
    public static final String MEASURE_MEASUREMENT_1H = "gas_metrics_1h";
    public static final String MEASURE_MEASUREMENT_1D = "gas_metrics_1d";
    public static final String MEASURE_MEASUREMENT_1M = "gas_metrics_1M";
    public static final String MEASURE_MEASUREMENT_1Y = "gas_metrics_1Y";
    public static final String ALERT_MEASUREMENT = "gas_alerts";

    private final String brokerUrl;
    private final String clientId;
    private final Integer qos;
    private final String measureTopic = "iota/iotagw/VirtualGasMeterGateway/+/measure";
    private final String alertTopic = "iota/iotagw/VirtualGasMeterGateway/+/alert";

    private MqttClient sampleClient;
    private boolean connectedToBroker;
    private GasMeterDBConnector dbConnector;


    GasMeterMqttDataCollector(GasMeterDBConnector dbConnector, Optional <String> brokerUrl,
                              Optional <String> clientId, Optional <Integer> qos) {
        this.dbConnector = dbConnector;
        this.brokerUrl = brokerUrl.orElse("tcp://mosquitto:1883");
        this.clientId = clientId.orElse("GasMeterMqttDataCollector");
        this.qos = qos.orElse(1);
    }

    public String getBrokerUrl() {
        return brokerUrl;
    }

    public String getInfluxDBUrl() {
        return dbConnector.getInfluxDBUrl();
    }

    public void start() {
        try {
            MemoryPersistence persistence = new MemoryPersistence();
            sampleClient = new MqttClient(brokerUrl, clientId, persistence);
            sampleClient.setCallback(this);

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setAutomaticReconnect(true);
            connOpts.setCleanSession(false);

            log.info("Connecting GasMeterMqttDataCollector to brokerUrl: {}", sampleClient.getServerURI());
            sampleClient.connect(connOpts);
            log.info("Connected GasMeterMqttDataCollector");

            sampleClient.subscribe(measureTopic, qos);
            sampleClient.subscribe(alertTopic, qos);
        } catch (MqttException e) {
            log.error("Error starting GasMeterMqttDataCollector: {}", e.getMessage(), e);
            throw new RuntimeException("Error starting GasMeterMqttDataCollector: " + e.getMessage());
        }

        log.info("Started GasMeterMqttDataCollector");
    }

    public void stop() {
        try {
            if (sampleClient != null) {
                sampleClient.disconnect();
            }
        } catch (MqttException e) {
            log.error("Error stopping GasMeterMqttDataCollector: {}", e.getMessage(), e);
            throw new RuntimeException("Error stopping GasMeterMqttDataCollector: " + e.getMessage());
        }
        log.info("Stopped GasMeterMqttDataCollector");
    }

    public boolean isConnectedToBroker() {
        return connectedToBroker;
    }

    public boolean isConnectedToDB() {
        return dbConnector.isConnectedToDB();
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        connectedToBroker = true;
    }

    @Override
    public void connectionLost(Throwable throwable) {
        connectedToBroker = false;
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        try {
            if (mqttMessage == null) {
                return;
            }

            String mess = new String(mqttMessage.getPayload());
            InfluxDB influxDB = dbConnector.getInfluxDB();
            if (topic.endsWith("measure")) {
                Metric metric;
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    metric = mapper.readValue(mess, Metric.class);
                    log.info("Arrived measure on {}: {}", topic, metric);
                } catch (Exception e) {
                    log.error("Fail decoding arrived measure on {}: {}", topic, e.getMessage());
                    return;
                }

                // TODO Validate measure in smart mode
                if (!metric.isValid()) {
                    log.warn("Ignore invalid measure: {}", metric);
                    return;
                }

                long timeInMillis = metric.getTimestamp();
                Calendar date = Calendar.getInstance();
                date.setTimeInMillis(timeInMillis);

                if (date.get(Calendar.MINUTE) == 0) {
                    insertMetric(metric, MEASURE_MEASUREMENT_1H);

                    if (date.get(Calendar.HOUR_OF_DAY) == 0) {
                        insertMetric(metric, MEASURE_MEASUREMENT_1D);

                        if (date.get(Calendar.DAY_OF_MONTH) == 1) {
                            insertMetric(metric, MEASURE_MEASUREMENT_1M);
                        }

                        if (date.get(Calendar.DAY_OF_YEAR) == 1) {
                            insertMetric(metric, MEASURE_MEASUREMENT_1Y);
                        }
                    }
                }
            } else if (topic.endsWith("alert")) {
                log.debug("Alert arrived on {}", topic);
                // Manage alert
                Alert alert;
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    alert = mapper.readValue(mess, Alert.class);
                    log.info("Arrived alert on {}: {}", topic, alert);
                } catch (Exception e) {
                    log.error("Fail decoding arrived alert on {}: {}", topic, e.getMessage());
                    return;
                }

                // TODO Validate alert in smart mode
                if (!alert.isValid()) {
                    log.warn("Ignore invalid alert: {}", alert);
                    return;
                }

                insertAlert(alert);
            } else {
                log.warn("Unexpected arrived message on {}: {}", topic, mess);
            }
        } catch (Exception e) {
            log.error("Error handling arrived message: {}", e.getMessage(), e);
            throw e;
        }
    }

    public void insertMetric(Metric metric, String measurement) {
        InfluxDB influxDB = dbConnector.getInfluxDB();
        Point point = Point.measurement(measurement)
                .time(metric.getTimestamp(), TimeUnit.MILLISECONDS)
                .addField("measure", metric.getMeasure()).addField("battery", metric.getBattery())
                .addField("temperature", metric.getTemperature()).addField("geohash", metric.getGeohash())
                .tag("meter", metric.getMeter()).build();
        influxDB.write(DATABASE_NAME, RETANTION_POLICY_NAME, point);
    }

    public void insertAlert(Alert alert) {
        InfluxDB influxDB = dbConnector.getInfluxDB();
        Point point = Point.measurement(ALERT_MEASUREMENT)
                .time(alert.getTimestamp(), TimeUnit.MILLISECONDS)
                .addField("open", !alert.isClosed())
                .addField("geohash", alert.getGeohash())
                .tag("msg", alert.getAlertMsg())
                .tag("meter", alert.getMeter())
                .build();
        influxDB.write(DATABASE_NAME, RETANTION_POLICY_NAME, point);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }

}
