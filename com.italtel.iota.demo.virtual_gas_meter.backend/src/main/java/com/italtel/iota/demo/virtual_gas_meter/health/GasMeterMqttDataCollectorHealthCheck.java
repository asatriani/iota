package com.italtel.iota.demo.virtual_gas_meter.health;

import com.codahale.metrics.health.HealthCheck;
import com.italtel.iota.demo.virtual_gas_meter.core.GasMeterMqttDataCollector;

/**
 * Created by satriani on 30/07/2017.
 */
public class GasMeterMqttDataCollectorHealthCheck extends HealthCheck {

    private final GasMeterMqttDataCollector collector;

    public GasMeterMqttDataCollectorHealthCheck(GasMeterMqttDataCollector collector) {
        this.collector = collector;
    }

    @Override
    protected Result check() throws Exception {
        if (!collector.isConnectedToBroker()) {
            return Result.unhealthy("Cannot ping broker to "
                    + collector.getBrokerUrl());
        }
        if (!collector.isConnectedToDB()) {
            return Result.unhealthy("Cannot ping DB to "
                    + collector.getInfluxDBUrl());
        }
        return Result.healthy();
    }
}