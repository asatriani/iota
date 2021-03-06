package com.italtel.iota.demo.virtual_gas_meter.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.setup.Environment;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.Optional;

/**
 * Created by satriani on 02/07/2017.
 */
public class GasMeterMqttDataCollectorFactory {

    private String brokerUrl;
    private String clientId;
    @Min(0)
    @Max(2)
    private Integer qos;

    @JsonProperty
    public String getBrokerUrl() {
        return brokerUrl;
    }

    public void setBrokerUrl(String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

    @JsonProperty
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @JsonProperty
    public Integer getQos() {
        return qos;
    }

    public void setQos(Integer qos) {
        this.qos = qos;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GasMeterMqttDataCollectorFactory{");
        sb.append("brokerUrl=").append(brokerUrl);
        sb.append(", clientId=").append(clientId);
        sb.append(", qos=").append(qos);
        sb.append('}');
        return sb.toString();
    }

    public GasMeterMqttDataCollector build(GasMeterDBConnector gasMeterDBConnector, Environment environment) {
        GasMeterMqttDataCollector mqttDataCollector = new GasMeterMqttDataCollector(
                gasMeterDBConnector, Optional.ofNullable(getBrokerUrl()),
                Optional.ofNullable(getClientId()), Optional.ofNullable(getQos()));
        environment.lifecycle().manage(mqttDataCollector);
        return mqttDataCollector;
    }
}
