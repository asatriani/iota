package com.italtel.iota.demo;

import java.util.Random;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttPublishSample implements MqttCallback {

    private static final Logger log = LoggerFactory.getLogger(MqttPublishSample.class);

    public void executeAlerts() {
        String reqBaseTopic = "$EDC/iota/iotagw/VirtualGasMeterGateway";
        String requesterId = "virtualGasMeterController";
        int qos = 0;
        String broker = "tcp://138.132.28.138:1883";
        String clientId = "JavaSample";

        try {
            MemoryPersistence persistence = new MemoryPersistence();
            MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            log.info("Connecting to broker: {}", broker);
            sampleClient.connect(connOpts);
            log.info("Connected");

            String reqId = Integer.toString(new Random().nextInt());
            sampleClient.setCallback(this);
            sampleClient.subscribe("$EDC/iota/virtualGasMeterController/VirtualGasMeterGateway/REPLY/" + reqId);

            String content = new StringBuilder("{\"metrics\":{").append("\"request.id\":\"").append(reqId)
                    .append("\", \"requester.client.id\":\"").append(requesterId).append("\", \"alerts\":\"")
                    .append("Virtual_Gas_Meter_0[1]").append("\"}}").toString();
            log.info("Publishing message: {}", content);
            MqttMessage message = new MqttMessage(content.getBytes());
            message.setQos(qos);
            sampleClient.publish(reqBaseTopic + "/set_alerts", message);
            log.info("Message published");

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            sampleClient.disconnect();
            log.info("Disconnected");

        } catch (MqttException me) {
            log.error("reason: {}", me.getReasonCode());
            log.error("msg: {}", me.getMessage());
            log.error("loc: {}", me.getLocalizedMessage());
            log.error("cause: {}", me.getCause());
            log.error("excep: {}", me);
        }

    }

    public void executeBattery() {
        String reqBaseTopic = "$EDC/iota/iotagw/VirtualGasMeterGateway";
        String requesterId = "virtualGasMeterController";
        int qos = 0;
        String broker = "tcp://138.132.28.138:1883";
        String clientId = "JavaSample";

        try {
            MemoryPersistence persistence = new MemoryPersistence();
            MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            log.info("Connecting to broker: {}", broker);
            sampleClient.connect(connOpts);
            log.info("Connected");

            String reqId = Integer.toString(new Random().nextInt());
            sampleClient.setCallback(this);
            sampleClient.subscribe("$EDC/iota/virtualGasMeterController/VirtualGasMeterGateway/REPLY/" + reqId);

            String content = new StringBuilder("{\"metrics\":{").append("\"request.id\":\"").append(reqId)
                    .append("\", \"requester.client.id\":\"").append(requesterId).append("\", \"meter\":").append(0)
                    .append(", \"reload_battery\":").append(false).append("}}").toString();
            log.info("Publishing message: {}", content);
            MqttMessage message = new MqttMessage(content.getBytes());
            message.setQos(qos);
            sampleClient.publish(reqBaseTopic + "/reload_battery", message);
            log.info("Message published");

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            sampleClient.disconnect();
            log.info("Disconnected");

        } catch (MqttException me) {
            log.error("reason: {}", me.getReasonCode());
            log.error("msg: {}", me.getMessage());
            log.error("loc: {}", me.getLocalizedMessage());
            log.error("cause: {}", me.getCause());
            log.error("excep: {}", me);
        }

    }

    public void executeLock() {
        String reqBaseTopic = "$EDC/iota/iotagw/VirtualGasMeterGateway";
        String requesterId = "virtualGasMeterController";
        int qos = 0;
        String broker = "tcp://138.132.28.138:1883";
        String clientId = "JavaSample";

        try {
            MemoryPersistence persistence = new MemoryPersistence();
            MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            log.info("Connecting to broker: {}", broker);
            sampleClient.connect(connOpts);
            log.info("Connected");

            String reqId = Integer.toString(new Random().nextInt());
            sampleClient.setCallback(this);
            sampleClient.subscribe("$EDC/iota/virtualGasMeterController/VirtualGasMeterGateway/REPLY/" + reqId);

            String content = new StringBuilder("{\"metrics\":{").append("\"request.id\":\"").append(reqId)
                    .append("\", \"requester.client.id\":\"").append(requesterId).append("\", \"meter\":").append(0)
                    .append(", \"lock\":").append(false).append("}}").toString();
            log.info("Publishing message: {}", content);
            MqttMessage message = new MqttMessage(content.getBytes());
            message.setQos(qos);
            sampleClient.publish(reqBaseTopic + "/lock", message);
            log.info("Message published");

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            sampleClient.disconnect();
            log.info("Disconnected");

        } catch (MqttException me) {
            log.error("reason: {}", me.getReasonCode());
            log.error("msg: {}", me.getMessage());
            log.error("loc: {}", me.getLocalizedMessage());
            log.error("cause: {}", me.getCause());
            log.error("excep: {}", me);
        }

    }

    public static void main(String[] args) {
        MqttPublishSample mqttPublishSample = new MqttPublishSample();

        // mqttPublishSample.executeLock();
        mqttPublishSample.executeAlerts();
        // mqttPublishSample.executeBattery();

    }

    public void messageArrived(String topic, MqttMessage message) throws Exception {
        log.info("Received response: {}", message);
    }

    @Override
    public void connectionLost(Throwable arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken arg0) {
        // TODO Auto-generated method stub

    }

}