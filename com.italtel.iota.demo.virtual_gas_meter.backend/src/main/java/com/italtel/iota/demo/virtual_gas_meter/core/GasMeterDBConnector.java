package com.italtel.iota.demo.virtual_gas_meter.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.italtel.iota.demo.virtual_gas_meter.api.Alert;
import com.italtel.iota.demo.virtual_gas_meter.api.Metric;
import io.dropwizard.lifecycle.Managed;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Pong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Created by satriani on 02/07/2017.
 */
public class GasMeterDBConnector implements Managed {

    private static final Logger log = LoggerFactory.getLogger(GasMeterDBConnector.class);

    private final String influxDBUrl;
    private InfluxDB influxDB;

    GasMeterDBConnector(Optional<String> influxDBUrl) {
        this.influxDBUrl = influxDBUrl.orElse("http://influxdb:8086");
    }

    public String getInfluxDBUrl() {
        return influxDBUrl;
    }

    public InfluxDB getInfluxDB() {
        return influxDB;
    }

    public void start() {
        influxDB = InfluxDBFactory.connect(influxDBUrl);
        influxDB.enableBatch(500, 100, TimeUnit.MILLISECONDS);
        if (!isConnectedToDB()) {
            String errMsg = "Error starting GasMeterDBConnector: DB is not reachable at " + influxDBUrl;
            log.error(errMsg);
            throw new RuntimeException(errMsg);
        }
        log.info("Started GasMeterDBConnector");
    }

    public void stop() {
        if (influxDB != null) {
            influxDB.close();
        }
        log.info("Stopped GasMeterDBConnector");
    }

    public boolean isConnectedToDB() {
        Pong pong = influxDB.ping();
        return pong != null && pong.getVersion() != null;
    }

}
