package com.italtel.iota.demo;

import java.util.concurrent.TimeUnit;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.ConsistencyLevel;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;

public class OverwriteTest {

    public static void main(String[] args) {
        InfluxDB influxDB = InfluxDBFactory.connect("http://iota01.italtel.com:8086");

        String dbName = "aTimeSeries";
        influxDB.deleteDatabase(dbName);
        influxDB.createDatabase(dbName);

        String rpName = "aRetentionPolicy";
        Query query = new Query("DROP RETENTION POLICY \"" + rpName + "\" ON \"" + dbName + "\"", dbName);
        influxDB.query(query);
        query = new Query("CREATE RETENTION POLICY \"" + rpName + "\" ON \"" + dbName + "\" DURATION " + "30d"
                + " REPLICATION 1 DEFAULT", dbName);
        influxDB.query(query);

        long timestamp = System.currentTimeMillis();
        BatchPoints batchPoints = BatchPoints.database(dbName).tag("async", "true").retentionPolicy(rpName)
                .consistency(ConsistencyLevel.ALL).build();
        Point point1 = Point.measurement("cpu").time(timestamp, TimeUnit.MILLISECONDS).addField("idle", 90L)
                .addField("user", 9L).addField("system", 1L).tag("name", "prova").build();
        Point point2 = Point.measurement("cpu").time(timestamp, TimeUnit.MILLISECONDS).addField("idle", 90L)
                .addField("user", 9L).addField("system", 1L).tag("name", "prova2").build();
        batchPoints.point(point1);
        batchPoints.point(point2);
        influxDB.write(batchPoints);

        query = new Query("SELECT * FROM cpu", dbName);
        QueryResult res = influxDB.query(query);
        System.out.println(res);
    }

}
