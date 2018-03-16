package com.italtel.iota.demo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.ConsistencyLevel;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Pong;
import org.influxdb.dto.Query;

public class MassiveLoader {

    private static final Logger log = Logger.getLogger(MassiveLoader.class.getName());

    private int counterSize = 1;
    private String counterPrefix = "Gas_Meter_";

    private String influxDBUrl = "http://iota01.italtel.com:8086";
    private String rpName = "5years";
    private String rpDuration = "1826d";
    private String databaseName = "gas_metering";
    private String measureName1H = "gas_metrics_1h";
    private String measureName1D = "gas_metrics_1d";
    private String measureName1M = "gas_metrics_1M";
    private String measureName1Y = "gas_metrics_1Y";
    private String startDateString = "01/01/2014 00:00:00";
    private String endDateString = null;
    private static double startMeasure = 0;
    private static double startBattery = 100.00;

    private InfluxDB influxDB;
    private Calendar startDate;
    private Calendar endDate;

    public MassiveLoader() {
        init();
    }

    private void init() {
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        if (startDateString == null || startDateString.trim().isEmpty()) {
            log.log(Level.SEVERE, "startDateString is null or empty");
            System.exit(1);
        }
        startDate = Calendar.getInstance(Locale.ITALY);
        try {
            startDate.setTime(format.parse(startDateString));
        } catch (ParseException ex) {
            log.log(Level.SEVERE, "Error parsing startDateString: " + ex.getMessage(), ex);
            System.exit(1);
        }

        endDate = Calendar.getInstance(Locale.ITALY);
        if (endDateString != null) {
            try {
                endDate.setTime(format.parse(endDateString));
            } catch (ParseException ex) {
                log.log(Level.SEVERE, "Error parsing endDateString: " + ex.getMessage(), ex);
                System.exit(1);
            }
        }

        influxDB = InfluxDBFactory.connect(influxDBUrl);
        Pong pong = influxDB.ping();
        if (pong.getVersion() == null) {
            log.log(Level.SEVERE, "DB is not reachable at " + influxDBUrl);
            System.exit(1);
        }

    }

    public void load() {
        influxDB.deleteDatabase(databaseName);
        influxDB.createDatabase(databaseName);

        createRP(rpName, rpDuration);

        for (int i = 0; i < counterSize; i++) {
            // Not use batching functionality
            BatchPoints batchPoints = BatchPoints.database(databaseName).tag("meter", counterPrefix + i)
                    .retentionPolicy(rpName).consistency(ConsistencyLevel.ALL).build();

            Point point;
            double consumption = 0;
            double batteryConsumption = 0;
            double measure1H = startMeasure;
            double battery = startBattery;

            Calendar date = Calendar.getInstance(Locale.ITALY);
            date.setTime(startDate.getTime());

            while (date.before(endDate)) {
                measure1H = round(measure1H + consumption, 2);
                battery = round(battery - batteryConsumption, 2);
                if (battery < 9) {
                    battery = startBattery;
                }

                point = Point.measurement(measureName1H).time(date.getTimeInMillis(), TimeUnit.MILLISECONDS)
                        .addField("measure", measure1H).addField("battery", battery).addField("temperature", 15.2)
                        .build();
                batchPoints.point(point);

                if (date.get(Calendar.HOUR_OF_DAY) == 0) {
                    point = Point.measurement(measureName1D).time(date.getTimeInMillis(), TimeUnit.MILLISECONDS)
                            .addField("measure", measure1H).build();
                    batchPoints.point(point);

                    if (date.get(Calendar.DAY_OF_MONTH) == 1) {
                        point = Point.measurement(measureName1M).time(date.getTimeInMillis(), TimeUnit.MILLISECONDS)
                                .addField("measure", measure1H).build();
                        batchPoints.point(point);
                    }

                    if (date.get(Calendar.DAY_OF_YEAR) == 1) {
                        point = Point.measurement(measureName1Y).time(date.getTimeInMillis(), TimeUnit.MILLISECONDS)
                                .addField("measure", measure1H).build();
                        batchPoints.point(point);
                    }
                }

                date.add(Calendar.HOUR_OF_DAY, 1);
                consumption = Math.random() * 0.16;
                batteryConsumption = Math.random() * 0.01;
            }

            // Write pending measures
            date.add(Calendar.HOUR_OF_DAY, -1);
            if (date.get(Calendar.HOUR_OF_DAY) != 0) {
                point = Point.measurement(measureName1D).time(date.getTimeInMillis(), TimeUnit.MILLISECONDS)
                        .addField("measure", measure1H).build();
                batchPoints.point(point);

                point = Point.measurement(measureName1M).time(date.getTimeInMillis(), TimeUnit.MILLISECONDS)
                        .addField("measure", measure1H).build();
                batchPoints.point(point);

                point = Point.measurement(measureName1Y).time(date.getTimeInMillis(), TimeUnit.MILLISECONDS)
                        .addField("measure", measure1H).build();
                batchPoints.point(point);
            }

            influxDB.write(batchPoints);
        }

    }

    private void createRP(String rp_name, String duration) {
        Query query = new Query("CREATE RETENTION POLICY \"" + rp_name + "\" ON \"" + databaseName + "\" DURATION "
                + duration + " REPLICATION 1 DEFAULT", databaseName);
        influxDB.query(query);
    }

    private double round(double value, int decNum) {
        double temp = Math.pow(10, decNum);
        return Math.round(value * temp) / temp;
    }

    public static void main(String[] args) {
        MassiveLoader loader = new MassiveLoader();
        loader.load();
    }
}
