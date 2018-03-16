package com.italtel.iota.demo.virtual_gas_meter;

import com.italtel.iota.demo.virtual_gas_meter.api.mapper.ViolationExceptionMapper;
import com.italtel.iota.demo.virtual_gas_meter.core.GasMeterController;
import com.italtel.iota.demo.virtual_gas_meter.core.GasMeterDBConnector;
import com.italtel.iota.demo.virtual_gas_meter.core.GasMeterMqttDataCollector;
import com.italtel.iota.demo.virtual_gas_meter.health.GasMeterControllerHealthCheck;
import com.italtel.iota.demo.virtual_gas_meter.health.GasMeterDbConnectorHealthCheck;
import com.italtel.iota.demo.virtual_gas_meter.health.GasMeterMqttDataCollectorHealthCheck;
import com.italtel.iota.demo.virtual_gas_meter.resources.GasMeterControllerService;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class BackendApplication extends Application<BackendConfiguration> {

    public static void main(final String[] args) throws Exception {
        new BackendApplication().run(args);
    }

    @Override
    public String getName() {
        return "Backend";
    }

    @Override
    public void initialize(final Bootstrap<BackendConfiguration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
                bootstrap.getConfigurationSourceProvider(),
                new EnvironmentVariableSubstitutor(false)));
    }

    @Override
    public void run(final BackendConfiguration configuration,
                    final Environment environment) {
        // ExceptionMapper
        final ViolationExceptionMapper violationExceptionMapper = new ViolationExceptionMapper();
        environment.jersey().register(violationExceptionMapper);

        // Managed
        final GasMeterDBConnector gasMeterDBConnector = configuration.getGasMeterDBConnectorFactory()
                .build(environment);
        final GasMeterMqttDataCollector gasMeterMqttDataCollector = configuration.getGasMeterMqttDataCollectorFactory()
                .build(gasMeterDBConnector, environment);
        final GasMeterController gasMeterController = configuration.getGasMeterControllerFactory()
                .build(environment, gasMeterMqttDataCollector);

        // HealthChecks
        environment.healthChecks().register("gasMeterDBConnector",
                new GasMeterDbConnectorHealthCheck(gasMeterDBConnector));
        environment.healthChecks().register("gasMeterMqttDataCollector",
                new GasMeterMqttDataCollectorHealthCheck(gasMeterMqttDataCollector));
        environment.healthChecks().register("gasMeterController",
                new GasMeterControllerHealthCheck(gasMeterController));

        // Resources
        final GasMeterControllerService gasMeterControllerService = new GasMeterControllerService
                (gasMeterController, gasMeterDBConnector);
        environment.jersey().register(gasMeterControllerService);
    }

}
