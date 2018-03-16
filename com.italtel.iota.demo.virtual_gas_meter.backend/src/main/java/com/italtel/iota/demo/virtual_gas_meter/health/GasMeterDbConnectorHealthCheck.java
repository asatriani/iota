package com.italtel.iota.demo.virtual_gas_meter.health;

import com.codahale.metrics.health.HealthCheck;
import com.italtel.iota.demo.virtual_gas_meter.core.GasMeterDBConnector;
import com.italtel.iota.demo.virtual_gas_meter.core.GasMeterMqttDataCollector;

/**
 * Created by satriani on 30/07/2017.
 */
public class GasMeterDbConnectorHealthCheck extends HealthCheck {

    private final GasMeterDBConnector connector;

    public GasMeterDbConnectorHealthCheck(GasMeterDBConnector connector) {
        this.connector = connector;
    }

    @Override
    protected Result check() throws Exception {
        if (!connector.isConnectedToDB()) {
            return Result.unhealthy("Cannot ping DB to "
                    + connector.getInfluxDBUrl());
        }
        return Result.healthy();
    }
}