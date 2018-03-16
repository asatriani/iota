package com.italtel.iota.demo;

import java.util.List;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;

public class ReadTest {

    public static void main(String[] args) {
        InfluxDB influxDB = InfluxDBFactory.connect("http://iota01.italtel.com:8086");
        String dbName = "gas_metering";

        Query query = new Query(
                "SELECT last(\"alertingCount\") as alertingCount FROM \"5years\".\"gas_alerts\" WHERE \"meter\" = 'Gas_Meter_0'",
                dbName);
        QueryResult res = influxDB.query(query);

        System.out.println(res);

        if (res != null && !res.hasError() && res.getResults() != null && !res.getResults().isEmpty()) {
            QueryResult.Result result = res.getResults().get(0);
            if (result != null && !result.hasError() && result.getSeries() != null && !result.getSeries().isEmpty()) {
                QueryResult.Series series = result.getSeries().get(0);
                if (series != null && series.getValues() != null && !series.getValues().isEmpty()) {
                    List<Object> os = series.getValues().get(0);
                    if (os != null) {
                        System.out.println(os.get(1).getClass().getSimpleName() + " " + os.get(1));
                    }
                }
            }
        }

    }

}
