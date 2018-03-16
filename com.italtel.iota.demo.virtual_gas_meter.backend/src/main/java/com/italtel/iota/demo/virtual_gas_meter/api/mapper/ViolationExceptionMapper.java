package com.italtel.iota.demo.virtual_gas_meter.api.mapper;

import com.italtel.iota.demo.virtual_gas_meter.api.Result;
import io.dropwizard.jersey.validation.ConstraintMessage;
import io.dropwizard.jersey.validation.JerseyViolationException;
import org.glassfish.jersey.server.model.Invocable;

import javax.validation.ConstraintViolation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by satriani on 28/07/2017.
 */
public class ViolationExceptionMapper implements ExceptionMapper<JerseyViolationException> {

    @Override
    public Response toResponse(final JerseyViolationException exception) {
        final Set<ConstraintViolation<?>> violations = exception.getConstraintViolations();
        final Invocable invocable = exception.getInvocable();
        final int status = ConstraintMessage.determineStatus(violations, invocable);
        String message = violations.stream().map(violation -> ConstraintMessage.getMessage(violation, invocable))
                .findFirst().orElse("Unknown");

        Result result = new Result(status, message);
        return Response.status(Response.Status.OK)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(result)
                .build();
    }
}
