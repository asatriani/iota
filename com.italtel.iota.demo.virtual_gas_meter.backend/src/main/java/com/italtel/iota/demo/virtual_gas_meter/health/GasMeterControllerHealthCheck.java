package com.italtel.iota.demo.virtual_gas_meter.health;

import com.codahale.metrics.health.HealthCheck;
import com.italtel.iota.demo.virtual_gas_meter.core.GasMeterController;

/**
 * Created by satriani on 30/07/2017.
 */
public class GasMeterControllerHealthCheck extends HealthCheck {

    private final GasMeterController controller;

    public GasMeterControllerHealthCheck(GasMeterController controller) {
        this.controller = controller;
    }

    @Override
    protected HealthCheck.Result check() throws Exception {
        if (controller.ping().getCode() == 200) {
            return HealthCheck.Result.healthy();
        } else {
            return HealthCheck.Result.unhealthy("Cannot ping broker to "
                    + controller.getBrokerUrl());
        }
    }
}