package com.italtel.iota.demo.virtual_gas_meter.api;

import ch.hsr.geohash.GeoHash;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.italtel.iota.demo.virtual_gas_meter.api.validator.CheckCoordinates;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.Arrays;

/**
 * Created by satriani on 05/07/2017.
 */
public class GasMeter implements Comparable <GasMeter> {

    @NotEmpty
    private String id;
    @NotEmpty
    @CheckCoordinates
    private String coordinates;
    @Min(0)
    private float measure;
    @Max(100)
    @Min(0)
    private float battery;
    @Max(100)
    @Min(-100)
    private float temperature;
    private boolean locked;
    private boolean offline;
    private String[] alertMsgs;

    public GasMeter() {
    }

    public GasMeter(String id) {
        this.id = id;
    }

    @JsonProperty
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty
    public String getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(String coordinates) {
        this.coordinates = coordinates;
    }

    @JsonIgnore
    public String getGeohash() {
        String[] parts = coordinates.split(",");
        return GeoHash
                .withCharacterPrecision(Float.parseFloat(parts[0].trim()), Float.parseFloat(parts[1].trim()), 9)
                .toBase32();
    }

    @JsonProperty
    public float getMeasure() {
        return measure;
    }

    public void setMeasure(float measure) {
        this.measure = measure;
    }

    @JsonProperty
    public float getBattery() {
        return battery;
    }

    public void setBattery(float battery) {
        this.battery = battery;
    }

    @JsonProperty
    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    @JsonProperty
    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isOffline() {
        return offline;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
    }

    @JsonProperty("alerts")
    public String[] getAlertMsgs() {
        return alertMsgs;
    }

    public void setAlertMsgs(String[] alertMsgs) {
        this.alertMsgs = alertMsgs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GasMeter gasMeter = (GasMeter) o;
        return Objects.equal(id, gasMeter.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GasMeter{");
        sb.append("id='").append(id).append('\'');
        sb.append(", coordinates='").append(coordinates).append('\'');
        sb.append(", measure=").append(measure);
        sb.append(", battery=").append(battery);
        sb.append(", temperature=").append(temperature);
        sb.append(", locked=").append(locked);
        sb.append(", offline=").append(offline);
        sb.append(", alertMsgs=").append(Arrays.toString(alertMsgs));
        sb.append('}');
        return sb.toString();
    }

    @Override
    public int compareTo(GasMeter o) {
        return id.compareTo(o.getId());
    }
}
