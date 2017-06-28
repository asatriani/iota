package com.italtel.iota.demo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.ConsistencyLevel;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ReadWriteTest {

    private static final Logger log = Logger.getLogger(ReadWriteTest.class.getName());

    private int counterSize = 10;
    private String counterPrefix = "Gas_Meter_";
    private String retentionPolicy = "5years";
    private String databaseName = "gas_metering_new";
    private String measureName = "gas_metrics_1h";
    private String startDateString = "01/01/2014 03:00:00";
    private static double startMeasure = 0;

    private static InfluxDB influxDB;

    public ReadWriteTest() {
    }

    @Before
    public void init() {
        influxDB = InfluxDBFactory.connect("http://138.132.28.138:8086");
        influxDB.createDatabase(databaseName);
    }

    @After
    public void clean() {

    }

    @Test
    public void createDBTest() {

    }

    @Test
    public void createPointsTest() {
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Calendar startDate = Calendar.getInstance();
        try {
            startDate.setTime(format.parse(startDateString));
        } catch (ParseException ex) {
            log.log(Level.SEVERE, null, ex);
            return;
        }
        Calendar currentDate = Calendar.getInstance();

        for (int i = 0; i < counterSize; i++) {
            // creation a group of points
            BatchPoints batchPoints = BatchPoints.database(databaseName).tag("meter", counterPrefix + i)
                    .retentionPolicy(retentionPolicy).consistency(ConsistencyLevel.ALL).build();
            Point point;
            double consumption;
            double measure = startMeasure;
            Calendar date = Calendar.getInstance();
            date.setTime(startDate.getTime());
            while (date.before(currentDate)) {
                consumption = Math.random() * 4;
                measure += consumption;

                point = Point.measurement(measureName).time(date.getTimeInMillis(), TimeUnit.MILLISECONDS)
                        .addField("measure", measure).build();
                batchPoints.point(point);
                date.add(Calendar.HOUR_OF_DAY, 1);
            }
            influxDB.write(batchPoints);
        }
    }

    @Test
    public void deleteDBTest() {
        influxDB.deleteDatabase(databaseName);
    }
}
