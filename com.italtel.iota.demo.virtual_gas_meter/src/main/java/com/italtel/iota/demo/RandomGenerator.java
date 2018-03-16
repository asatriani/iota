package com.italtel.iota.demo;

import java.util.Random;

public class RandomGenerator {

    private VirtualGasMeterGateway gw;
    private Random random = new Random();

    public RandomGenerator(VirtualGasMeterGateway gw) {
        this.gw = gw;
    }

    public String getCoordinates() {
        Float lat = gw.getRefLatitude();
        Float lon = gw.getRefLongitude();
        if (lat == null || lon == null) {
            throw new RuntimeException(
                    "Invalid coordinates in config prop " + VirtualGasMeterGateway.REFERENCE_LOCATION_PROP_NAME);
        }

        lat += (float) ((random.nextFloat() * 0.2/* 0.03 */) * (random.nextBoolean() ? 1 : -1));
        lon += (float) ((random.nextFloat() * 0.48/* 0.06 */) * (random.nextBoolean() ? 1 : -1));
        return lat + "," + lon;
    }

    public double getConsumption() {
        return random.nextDouble() * (Double) gw.getProperties().get(VirtualGasMeterGateway.MAX_CONSUMPTION_PROP_NAME);
    }

    public double getBatteryConsumption() {
        return random.nextDouble()
                * (Double) gw.getProperties().get(VirtualGasMeterGateway.MAX_BATTERY_LEVEL_CONSUMPTION_PROP_NAME);
    }

    public double getTemperatureDeviation() {
        return random.nextDouble()
                * (Double) gw.getProperties().get(VirtualGasMeterGateway.MAX_TEMEPERATURE_DEVIATION_PROP_NAME)
                * (random.nextBoolean() ? 1 : -1);
    }

}
