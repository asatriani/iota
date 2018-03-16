package com.italtel.iota.demo.virtual_gas_meter.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.setup.Environment;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.Optional;

/**
 * Created by satriani on 02/07/2017.
 */
public class GasMeterDBConnectorFactory {

    private String influxDBUrl;

    @JsonProperty
    public String getInfluxDBUrl() {
        return influxDBUrl;
    }

    public void setInfluxDBUrl(String influxDBUrl) {
        this.influxDBUrl = influxDBUrl;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GasMeterDBConnectorFactory{");
        sb.append("influxDBUrl=").append(influxDBUrl);
        sb.append('}');
        return sb.toString();
    }

    public GasMeterDBConnector build(Environment environment) {
        GasMeterDBConnector connector = new GasMeterDBConnector(
                Optional.ofNullable(getInfluxDBUrl()));
        environment.lifecycle().manage(connector);
        return connector;
    }
}
