package com.italtel.iota.demo.virtual_gas_meter;

import com.italtel.iota.demo.virtual_gas_meter.core.GasMeterControllerFactory;
import com.italtel.iota.demo.virtual_gas_meter.core.GasMeterDBConnectorFactory;
import com.italtel.iota.demo.virtual_gas_meter.core.GasMeterMqttDataCollectorFactory;
import io.dropwizard.Configuration;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.*;

public class BackendConfiguration extends Configuration {

    @Valid
    @NotNull
    private GasMeterDBConnectorFactory gasMeterDBConnector = new GasMeterDBConnectorFactory();

    @Valid
    @NotNull
    private GasMeterMqttDataCollectorFactory gasMeterMqttDataCollector = new GasMeterMqttDataCollectorFactory();

    @Valid
    @NotNull
    private GasMeterControllerFactory gasMeterController = new GasMeterControllerFactory();

    @JsonProperty("gasMeterDBConnector")
    public GasMeterDBConnectorFactory getGasMeterDBConnectorFactory() {
        return gasMeterDBConnector;
    }

    public void setGasMeterDBConnectorFactory(GasMeterDBConnectorFactory gasMeterDBConnector) {
        this.gasMeterDBConnector = gasMeterDBConnector;
    }

    @JsonProperty("gasMeterMqttDataCollector")
    public GasMeterMqttDataCollectorFactory getGasMeterMqttDataCollectorFactory() {
        return gasMeterMqttDataCollector;
    }

    public void setGasMeterMqttDataCollectorFactory(GasMeterMqttDataCollectorFactory factory) {
        this.gasMeterMqttDataCollector = factory;
    }

    @JsonProperty("gasMeterController")
    public GasMeterControllerFactory getGasMeterControllerFactory() {
        return gasMeterController;
    }

    public void setGasMeterControllerFactory(GasMeterControllerFactory factory) {
        this.gasMeterController = factory;
    }
}
