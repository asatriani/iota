package com.italtel.iota.demo.virtual_gas_meter.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.italtel.iota.demo.virtual_gas_meter.api.*;
import io.dropwizard.lifecycle.Managed;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.Random;

/**
 * Created by satriani on 05/07/2017.
 */
public class GasMeterController implements Managed {

    private static final Logger log = LoggerFactory.getLogger(GasMeterController.class);

    private final String brokerUrl;
    private final String clientId;
    private final Integer qos;
    private final GasMeterMqttDataCollector gasMeterMqttDataCollector;
    private String reqBaseTopic = "$EDC/iota/iotagw/VirtualGasMeterGateway";
    private MqttClient sampleClient;

    GasMeterController(Optional <String> brokerUrl, Optional <String> clientId, Optional <Integer> qos,
                       GasMeterMqttDataCollector gasMeterMqttDataCollector) {
        this.brokerUrl = brokerUrl.orElse("tcp://mosquitto:1883");
        this.clientId = clientId.orElse("GasMeterController");
        this.qos = qos.orElse(1);
        this.gasMeterMqttDataCollector = gasMeterMqttDataCollector;
    }

    public String getBrokerUrl() {
        return brokerUrl;
    }

    public void start() {
        try {
            MemoryPersistence persistence = new MemoryPersistence();
            sampleClient = new MqttClient(brokerUrl, clientId, persistence);
            sampleClient.setTimeToWait(2000);

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setAutomaticReconnect(true);
            connOpts.setCleanSession(true);

            log.info("Connecting GasMeterController to brokerUrl: {}", sampleClient.getServerURI());
            sampleClient.connect(connOpts);
            log.info("Connected GasMeterController");
        } catch (MqttException e) {
            log.error("Error starting GasMeterController: {}", e.getMessage(), e);
            throw new RuntimeException("Error starting GasMeterController: " + e.getMessage());
        }
        log.info("Started GasMeterController");
    }

    public void stop() {
        try {
            sampleClient.disconnect();
        } catch (MqttException e) {
            log.error("Error stopping GasMeterController: {}", e.getMessage(), e);
            throw new RuntimeException("Error stopping GasMeterController: " + e.getMessage());
        }
        log.info("Stopped GasMeterController");
    }

    public synchronized Result getMeterById(final String id) throws Exception {
        final Result res = new Result(500, "Timeout");

        String reqId = getRequestId();
        String topicFilter = "$EDC/iota/" + clientId + "/VirtualGasMeterGateway/REPLY/" + reqId;
        sampleClient.subscribe(topicFilter, (topic, message) -> {
            synchronized (GasMeterController.this) {
                log.debug("Received response to retrieve meter by id {}", id);
                ObjectMapper mapper = new ObjectMapper();
                KuraGasMeterResponse gasMeterResponse = mapper.readValue(message.getPayload(), KuraGasMeterResponse.class);
                log.debug("Received response to retrieve meter by id {}: {}", id, gasMeterResponse);
                int code = gasMeterResponse.getResponse().getCode();
                res.setCode(code);
                if (code != 200) {
                    String exMessage = gasMeterResponse.getResponse().getExMessage();
                    res.setMessage(exMessage);
                    log.error("Error retrieving meter by id {}: {}", id, exMessage);
                } else {
                    try {
                        GasMeter gasMeter = mapper.readValue(gasMeterResponse.getBody(), GasMeter.class);
                        res.setData(gasMeter);
                        res.setMessage("Success");
                        log.info("Successfully retrieving meter by id {}: {}", id, gasMeter);
                    } catch (Exception e) {
                        res.setCode(500);
                        res.setMessage(e.getMessage());
                        log.error("Error retrieving meter by id {}: {}", id, e.getMessage());
                    }
                }

                GasMeterController.this.notify();
            }
        });

        ObjectMapper mapper = new ObjectMapper();
        KuraGasMeterRequest.Request request = new KuraGasMeterRequest.Request(reqId, clientId, id);
        KuraGasMeterRequest gasMeterRequest = new KuraGasMeterRequest(request);
        String content = mapper.writeValueAsString(gasMeterRequest);
        String topic = reqBaseTopic + "/GET";
        log.info("Publishing to topic {} message to get meter by id {}: {}", topic, id, content);
        MqttMessage message = new MqttMessage(content.getBytes());
        message.setQos(qos);
        sampleClient.publish(topic, message);
        log.info("Message published");

        try {
            this.wait(10000);
        } finally {
            try {
                sampleClient.unsubscribe(topicFilter);
            } catch (Exception e) {
            }
        }
        return res;
    }

    public synchronized Result getAllMeter(final boolean fullFetch) throws Exception {
        final Result res = new Result(500, "Timeout");

        String reqId = getRequestId();
        String topicFilter = "$EDC/iota/" + clientId + "/VirtualGasMeterGateway/REPLY/" + reqId;
        sampleClient.subscribe(topicFilter, (topic, message) -> {
            synchronized (GasMeterController.this) {
                log.debug("Received response to retrieve all meter (fullFetch={}): {}", fullFetch);
                ObjectMapper mapper = new ObjectMapper();
                KuraGasMeterResponse gasMeterResponse = mapper.readValue(message.getPayload(), KuraGasMeterResponse.class);
                log.debug("Received response to retrieve all meter (fullFetch={}): {}", fullFetch, gasMeterResponse);
                int code = gasMeterResponse.getResponse().getCode();
                res.setCode(code);
                if (code != 200) {
                    String exMessage = gasMeterResponse.getResponse().getExMessage();
                    res.setMessage(exMessage);
                    log.error("Error retrieving all meter (fullFetch={}): {}", fullFetch, exMessage);
                } else {
                    try {
                        Object[] data;
                        if (fullFetch) {
                            data = mapper.readValue(gasMeterResponse.getBody(), GasMeter[].class);
                        } else {
                            data = mapper.readValue(gasMeterResponse.getBody(), String[].class);
                        }
                        Arrays.sort(data);
                        res.setData(data);
                        res.setMessage("Success");
                        log.info("Successfully retrieving all meter (fullFetch={}): {}", fullFetch, Arrays.toString(data));
                    } catch (Exception e) {
                        res.setCode(500);
                        res.setMessage(e.getMessage());
                        log.error("Error retrieving all meter (fullFetch={}): {}", fullFetch, e.getMessage());
                    }
                }


                GasMeterController.this.notify();
            }
        });

        ObjectMapper mapper = new ObjectMapper();
        KuraGasMeterRequest.Request request = new KuraGasMeterRequest.Request(reqId, clientId);
        request.setFullFetch(fullFetch);
        KuraGasMeterRequest gasMeterRequest = new KuraGasMeterRequest(request);
        String content = mapper.writeValueAsString(gasMeterRequest);
        String topic = reqBaseTopic + "/GET";
        log.info("Publishing to topic {} message to get all meter (fullFetch={}): {}", topic, fullFetch, content);
        MqttMessage message = new MqttMessage(content.getBytes());
        message.setQos(qos);
        sampleClient.publish(topic, message);
        log.info("Message published");

        try {
            this.wait(10000);
        } finally {
            try {
                sampleClient.unsubscribe(topicFilter);
            } catch (Exception e) {
            }
        }
        return res;
    }

    public synchronized Result addMeter(final GasMeter meter) throws Exception {
        final Result res = new Result(500, "Timeout");

        String reqId = getRequestId();
        String topicFilter = "$EDC/iota/" + clientId + "/VirtualGasMeterGateway/REPLY/" + reqId;
        sampleClient.subscribe(topicFilter, (topic, message) -> {
            synchronized (GasMeterController.this) {
                log.debug("Received response to add meter {}", meter.getId());
                ObjectMapper mapper = new ObjectMapper();
                KuraGasMeterResponse gasMeterResponse = mapper.readValue(message.getPayload(), KuraGasMeterResponse.class);
                log.debug("Received response to add meter {}: {}", meter.getId(), gasMeterResponse);
                int code = gasMeterResponse.getResponse().getCode();
                res.setCode(code);
                if (code != 200) {
                    String exMessage = gasMeterResponse.getResponse().getExMessage();
                    res.setMessage(exMessage);
                    log.error("Error adding meter {}: {}", meter.getId(), exMessage);
                } else {
                    try {
                        GasMeter gasMeter = mapper.readValue(gasMeterResponse.getBody(), GasMeter.class);
                        res.setData(gasMeter);
                        res.setMessage("Success");
                        log.info("Successfully adding meter {}: {}", meter.getId(), gasMeter);
                    } catch (Exception e) {
                        res.setCode(500);
                        res.setMessage(e.getMessage());
                        log.error("Error adding meter {}: {}", meter.getId(), e.getMessage());
                    }
                }

                GasMeterController.this.notify();
            }
        });

        ObjectMapper mapper = new ObjectMapper();
        KuraGasMeterRequest.Request request = new KuraGasMeterRequest.Request(reqId, clientId, meter.getId());
        byte[] body = mapper.writeValueAsBytes(meter);
        KuraGasMeterRequest gasMeterRequest = new KuraGasMeterRequest(request, body);
        String content = mapper.writeValueAsString(gasMeterRequest);
        String topic = reqBaseTopic + "/POST";
        log.info("Publishing to topic {} message to add meter {}: {}", topic, meter.getId(), content);
        MqttMessage message = new MqttMessage(content.getBytes());
        message.setQos(qos);
        sampleClient.publish(topic, message);
        log.info("Message published");

        try {
            this.wait(10000);
        } finally {
            try {
                sampleClient.unsubscribe(topicFilter);
            } catch (Exception e) {
            }
        }

        if (meter.isOffline()) {
            insertOfflineAlert(meter, false);
        }
        return res;
    }

    public synchronized Result updateMeter(final GasMeter meter) throws Exception {
        GasMeter oldMeter = null;
        try {
            Result getOldMeterRes = getMeterById(meter.getId());
            if (getOldMeterRes.getCode() == 200) {
                oldMeter = (GasMeter) getOldMeterRes.getData();
            }
        } catch (Exception e) {
        }

        final Result res = new Result(500, "Timeout");
        String reqId = getRequestId();
        String topicFilter = "$EDC/iota/" + clientId + "/VirtualGasMeterGateway/REPLY/" + reqId;
        sampleClient.subscribe(topicFilter, (topic, message) -> {
            synchronized (GasMeterController.this) {
                log.debug("Received response to update meter {}", meter.getId());
                ObjectMapper mapper = new ObjectMapper();
                KuraGasMeterResponse gasMeterResponse = mapper.readValue(message.getPayload(), KuraGasMeterResponse.class);
                log.debug("Received response to update meter {}: {}", meter.getId(), gasMeterResponse);
                int code = gasMeterResponse.getResponse().getCode();
                res.setCode(code);
                if (code != 200) {
                    String exMessage = gasMeterResponse.getResponse().getExMessage();
                    res.setMessage(exMessage);
                    log.error("Error updating meter {}: {}", meter.getId(), exMessage);
                } else {
                    try {
                        GasMeter gasMeter = mapper.readValue(gasMeterResponse.getBody(), GasMeter.class);
                        res.setData(gasMeter);
                        res.setMessage("Success");
                        log.info("Successfully updating meter {}: {}", meter.getId(), gasMeter);
                    } catch (Exception e) {
                        res.setCode(500);
                        res.setMessage(e.getMessage());
                        log.error("Error updating meter {}: {}", meter.getId(), e.getMessage());
                    }
                }

                GasMeterController.this.notify();
            }
        });

        ObjectMapper mapper = new ObjectMapper();
        KuraGasMeterRequest.Request request = new KuraGasMeterRequest.Request(reqId, clientId, meter.getId());
        byte[] body = mapper.writeValueAsBytes(meter);
        KuraGasMeterRequest gasMeterRequest = new KuraGasMeterRequest(request, body);
        String content = mapper.writeValueAsString(gasMeterRequest);
        String topic = reqBaseTopic + "/PATCH";
        log.info("Publishing to topic {} message to update meter {}: {}", topic, meter.getId(), content);
        MqttMessage message = new MqttMessage(content.getBytes());
        message.setQos(qos);
        sampleClient.publish(topic, message);
        log.info("Message published");

        try {
            this.wait(10000);
        } finally {
            try {
                sampleClient.unsubscribe(topicFilter);
            } catch (Exception e) {
            }
        }

        if (oldMeter != null && oldMeter.isOffline() != meter.isOffline()) {
            insertOfflineAlert(meter, meter.isOffline());
        }
        return res;
    }

    public synchronized Result deleteMeterById(final String id) throws Exception {
        final Result res = new Result(500, "Timeout");

        String reqId = getRequestId();
        String topicFilter = "$EDC/iota/" + clientId + "/VirtualGasMeterGateway/REPLY/" + reqId;
        sampleClient.subscribe(topicFilter, (topic, message) -> {
            synchronized (GasMeterController.this) {
                log.debug("Received response to delete meter {}", id);
                ObjectMapper mapper = new ObjectMapper();
                KuraGasMeterResponse gasMeterResponse = mapper.readValue(message.getPayload(), KuraGasMeterResponse.class);
                log.debug("Received response to delete meter {}: {}", id, gasMeterResponse);
                int code = gasMeterResponse.getResponse().getCode();
                res.setCode(code);
                if (code != 200) {
                    String exMessage = gasMeterResponse.getResponse().getExMessage();
                    res.setMessage(exMessage);
                    log.error("Error deleting meter {}: {}", id, exMessage);
                } else {
                    try {
                        GasMeter gasMeter = mapper.readValue(gasMeterResponse.getBody(), GasMeter.class);
                        res.setData(gasMeter);
                        res.setMessage("Success");
                        log.info("Successfully deleting meter {}: {}", id, gasMeter);
                    } catch (Exception e) {
                        res.setCode(500);
                        res.setMessage(e.getMessage());
                        log.error("Error deleting meter {}: {}", id, e.getMessage());
                    }
                }

                GasMeterController.this.notify();
            }
        });

        ObjectMapper mapper = new ObjectMapper();
        KuraGasMeterRequest.Request request = new KuraGasMeterRequest.Request(reqId, clientId, id);
        KuraGasMeterRequest gasMeterRequest = new KuraGasMeterRequest(request);
        String content = mapper.writeValueAsString(gasMeterRequest);
        String topic = reqBaseTopic + "/DELETE";
        log.info("Publishing to topic {} message to delete meter {}: {}", topic, id, content);
        MqttMessage message = new MqttMessage(content.getBytes());
        message.setQos(qos);
        sampleClient.publish(topic, message);
        log.info("Message published");

        try {
            this.wait(10000);
        } finally {
            try {
                sampleClient.unsubscribe(topicFilter);
            } catch (Exception e) {
            }
        }

        GasMeter gasMeter = (GasMeter)res.getData();
        if (gasMeter.isOffline()) {
            insertOfflineAlert(gasMeter, true);
        }
        return res;
    }

    public synchronized Result ping() throws Exception {
        final Result res = new Result(500, "Timeout");

        String reqId = getRequestId();
        String topicFilter = "$EDC/iota/" + clientId + "/VirtualGasMeterGateway/REPLY/" + reqId;
        sampleClient.subscribe(topicFilter, (topic, message) -> {
            synchronized (GasMeterController.this) {
                log.debug("Received response to ping request");
                ObjectMapper mapper = new ObjectMapper();
                KuraGasMeterResponse gasMeterResponse = mapper.readValue(message.getPayload(), KuraGasMeterResponse.class);
                log.debug("Received response to ping request: {}", gasMeterResponse);
                int code = gasMeterResponse.getResponse().getCode();
                res.setCode(code);
                if (code != 200) {
                    String exMessage = gasMeterResponse.getResponse().getExMessage();
                    res.setMessage(exMessage);
                    log.error("Error responding to ping request: {}", exMessage);
                } else {
                    try {
                        res.setData(new String(gasMeterResponse.getBody()));
                        res.setMessage("Success");
                        log.info("Successfully ping request");
                    } catch (Exception e) {
                        res.setCode(500);
                        res.setMessage(e.getMessage());
                        log.error("Error responding to ping request: {}", e.getMessage());
                    }
                }

                GasMeterController.this.notify();
            }
        });

        ObjectMapper mapper = new ObjectMapper();
        KuraGasMeterRequest.Request request = new KuraGasMeterRequest.Request(reqId, clientId);
        KuraGasMeterRequest gasMeterRequest = new KuraGasMeterRequest(request);
        String content = mapper.writeValueAsString(gasMeterRequest);
        String topic = reqBaseTopic + "/PING";
        log.info("Publishing to topic {} message to ping gateway: {}", topic, content);
        MqttMessage message = new MqttMessage(content.getBytes());
        message.setQos(qos);
        sampleClient.publish(topic, message);
        log.info("Message published");

        try {
            this.wait(10000);
        } finally {
            try {
                sampleClient.unsubscribe(topicFilter);
            } catch (Exception e) {
            }
        }
        return res;
    }

    public Result massiveLoad(Integer size, String prefix, Long startTimestamp) throws Exception {
        final Result res = new Result(500, "Timeout");
        String reqId = getRequestId();
        String topicFilter = "$EDC/iota/" + clientId + "/VirtualGasMeterGateway/REPLY/" + reqId;
        sampleClient.subscribe(topicFilter, (topic, message) -> {
            synchronized (GasMeterController.this) {
                log.debug("Received response to massive load");
                ObjectMapper mapper = new ObjectMapper();
                KuraGasMeterResponse gasMeterResponse = mapper.readValue(message.getPayload(), KuraGasMeterResponse.class);
                log.debug("Received response to massive load: {}", gasMeterResponse);
                int code = gasMeterResponse.getResponse().getCode();
                res.setCode(code);
                if (code != 200) {
                    String exMessage = gasMeterResponse.getResponse().getExMessage();
                    res.setMessage(exMessage);
                    log.error("Error executing massive load: {}", exMessage);
                } else {
                    res.setMessage("Success");
                    log.info("Successfully executing massive load");
                }

                GasMeterController.this.notify();
            }
        });

        ObjectMapper mapper = new ObjectMapper();
        KuraGasMeterRequest.Request request = new KuraGasMeterRequest.Request(
                reqId, clientId, size, prefix, startTimestamp);
        KuraGasMeterRequest gasMeterRequest = new KuraGasMeterRequest(request);
        String content = mapper.writeValueAsString(gasMeterRequest);
        String topic = reqBaseTopic + "/MASSIVE_LOAD";
        log.info("Publishing to topic {} message to execute massive load: {}", topic, content);
        MqttMessage message = new MqttMessage(content.getBytes());
        message.setQos(qos);
        sampleClient.publish(topic, message);
        log.info("Message published");

        try {
            this.wait(60000);
        } finally {
            try {
                sampleClient.unsubscribe(topicFilter);
            } catch (Exception e) {
            }
        }
        return res;
    }

    private void insertOfflineAlert(final GasMeter meter, boolean closed) {
        Alert offlineAlert = new Alert();
        offlineAlert.setTimestamp(System.currentTimeMillis());
        offlineAlert.setMeter(meter.getId());
        offlineAlert.setClosed(closed);
        offlineAlert.setGeohash(meter.getGeohash());
        offlineAlert.setAlertMsg("Meter is not reachable");
        gasMeterMqttDataCollector.insertAlert(offlineAlert);
    }

    private String getRequestId() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        return dateFormat.format(new Date()) + "-" + Integer.toString(new Random().nextInt());
    }
}
