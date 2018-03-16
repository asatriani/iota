package com.italtel.iota.demo.virtual_gas_meter.api.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Created by satriani on 24/07/2017.
 */
public class CheckCoordinatesValidator implements ConstraintValidator<CheckCoordinates, String> {

    @Override
    public void initialize(CheckCoordinates constraintAnnotation) {
    }

    @Override
    public boolean isValid(String coordinates, ConstraintValidatorContext constraintContext) {
        if (coordinates == null) {
            return true;
        }

        if (!coordinates.trim().isEmpty()) {
            String[] parts = coordinates.split(",");
            if (parts.length == 2) {
                float lat = Float.parseFloat(parts[0].trim());
                float lon = Float.parseFloat(parts[1].trim());
                if (lat >= -90 && lat <= 90 && lon >=-180 && lon <= 180) {
                    return true;
                }
            }
        }
        return false;
    }


}
