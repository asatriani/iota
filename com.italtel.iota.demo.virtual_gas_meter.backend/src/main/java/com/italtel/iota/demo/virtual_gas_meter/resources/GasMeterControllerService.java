package com.italtel.iota.demo.virtual_gas_meter.resources;

import com.codahale.metrics.annotation.Timed;
import com.italtel.iota.demo.virtual_gas_meter.core.GasMeterController;
import com.italtel.iota.demo.virtual_gas_meter.api.GasMeter;
import com.italtel.iota.demo.virtual_gas_meter.api.Result;
import com.italtel.iota.demo.virtual_gas_meter.core.GasMeterDBConnector;
import com.italtel.iota.demo.virtual_gas_meter.core.GasMeterMqttDataCollector;
import io.dropwizard.jersey.PATCH;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

/**
 * Created by satriani on 05/07/2017.
 */
@Path("/gasMeters")
@Produces(MediaType.APPLICATION_JSON)
public class GasMeterControllerService {

    private static final Logger log = LoggerFactory.getLogger(GasMeterControllerService.class);

    private final GasMeterController controller;
    private final GasMeterDBConnector dbConnector;

    public GasMeterControllerService(GasMeterController controller, GasMeterDBConnector dbConnector) {
        this.controller = controller;
        this.dbConnector = dbConnector;
    }

    @GET
    @Timed
//    @Metered
//    @ExceptionMetered
    @Path("{id}")
    @NotNull
    @Valid
    public Result getGasMeter(@PathParam("id") String id) {
        try {
            return controller.getMeterById(id);
        } catch (Exception e) {
            log.error("Error getting meter {}: {}", id, e.getMessage(), e);
            return new Result(500, e.getMessage());
        }
    }

    @GET
    @Timed
//    @Metered
//    @ExceptionMetered
    @NotNull
    @Valid
    public Result getGasMeters(@QueryParam("fullFetch") Optional<Boolean> fullFetch) {
        try {
            return controller.getAllMeter(fullFetch.orElse(false));
        } catch (Exception e) {
            log.error("Error getting all meters (fullFetch={}): {}", fullFetch, e.getMessage(), e);
            return new Result(500, e.getMessage());
        }
    }

    @POST
    @Timed
//    @Metered
//    @ExceptionMetered
    @Consumes({MediaType.APPLICATION_JSON})
    @Path("{id}")
    @NotNull
    @Valid
    public Result addGasMeter(@PathParam("id") String id, @NotNull @Valid GasMeter meter) {
        if (!meter.getId().equals(id)) {
            String errMsg = "Invalid request: Path param id is not equal to meter id in body request";
            log.error(errMsg);
            return new Result(400, errMsg);
        }

        try {
            return controller.addMeter(meter);
        } catch (Exception e) {
            log.error("Error adding meter {}: {}", meter.getId(), e.getMessage(), e);
            return new Result(500, e.getMessage());
        }
    }

    @PATCH
    @Timed
//    @Metered
//    @ExceptionMetered
    @Consumes({MediaType.APPLICATION_JSON})
    @Path("{id}")
    @NotNull
    @Valid
    public Result updateGasMeter(@PathParam("id") String id, @NotNull @Valid GasMeter meter) {
        if (!meter.getId().equals(id)) {
            String errMsg = "Invalid request: Path param id is not equal to meter id in body request";
            log.error(errMsg);
            return new Result(400, errMsg);
        }

        try {
            return controller.updateMeter(meter);
        } catch (Exception e) {
            log.error("Error updating meter {}: {}", meter.getId(), e.getMessage(), e);
            return new Result(500, e.getMessage());
        }
    }

    @DELETE
    @Timed
//    @Metered
//    @ExceptionMetered
    @Path("{id}")
    @NotNull
    @Valid
    public Result deleteGasMeter(@PathParam("id") String id) {
        try {
            return controller.deleteMeterById(id);
        } catch (Exception e) {
            log.error("Error deleting meter {}: {}", id, e.getMessage(), e);
            return new Result(500, e.getMessage());
        }
    }

    @POST
    @Timed
//    @Metered
//    @ExceptionMetered
    @Path("massiveLoad")
    @NotNull
    @Valid
    public Result massiveLoad(@NotNull @QueryParam("size") Integer size,
                              @QueryParam("prefix") Optional<String> prefix,
                              @QueryParam("prefix") Optional<Long> startTimestamp) {
        try {
            return controller.massiveLoad(size, prefix.orElse("Gas_Meter_"), startTimestamp.orElseGet(() ->
            { Calendar c = Calendar.getInstance(); c.add(-4, Calendar.YEAR); return c.getTimeInMillis(); }));
        } catch (Exception e) {
            log.error("Error executing massive load: {}", e.getMessage(), e);
            return new Result(500, e.getMessage());
        }
    }

    @POST
    @Timed
//    @Metered
//    @ExceptionMetered
    @Path("initDB")
    @NotNull
    @Valid
    public Result initDB() {
        try {
            InfluxDB influxDB = dbConnector.getInfluxDB();
            influxDB.deleteDatabase(GasMeterMqttDataCollector.DATABASE_NAME);
            influxDB.createDatabase(GasMeterMqttDataCollector.DATABASE_NAME);
            Query query = new Query("CREATE RETENTION POLICY \""
                    + GasMeterMqttDataCollector.RETANTION_POLICY_NAME + "\" ON \""
                    + GasMeterMqttDataCollector.DATABASE_NAME + "\" DURATION "
                    + GasMeterMqttDataCollector.RETANTION_POLICY_DURATION + " REPLICATION 1 DEFAULT",
                    GasMeterMqttDataCollector.DATABASE_NAME);
            influxDB.query(query);
            return new Result(200);
        } catch (Exception e) {
            log.error("Error initializing DB: {}", e.getMessage(), e);
            return new Result(500, e.getMessage());
        }
    }
}
