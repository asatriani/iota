package com.italtel.iota.demo.virtual_gas_meter.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Date;

/**
 * Created by satriani on 11/07/2017.
 */
public class KuraGasMeterResponse {

    public static class Response {

        @JsonProperty(value = "response.code", required = true)
        private int code;

        @JsonProperty("response.exception.message")
        private String exMessage;

        @JsonProperty("response.exception.stack")
        private String exStack;

        public Response() {

        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getExMessage() {
            return exMessage;
        }

        public void setExMessage(String exMessage) {
            this.exMessage = exMessage;
        }

        public String getExStack() {
            return exStack;
        }

        public void setExStack(String exStack) {
            this.exStack = exStack;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Response{");
            sb.append("code=").append(code);
            sb.append(", exMessage='").append(exMessage).append('\'');
            sb.append(", exStack='").append(exStack).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }

    @JsonProperty("sentOn")
    private long timestamp;

    @JsonProperty(value = "metrics", required = true)
    private Response response;

    @JsonProperty
    private byte[] body;

    public KuraGasMeterResponse() {

    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("KuraGasMeterResponse{");
        sb.append("timestamp=").append(timestamp);
        sb.append(", response=").append(response);
        sb.append(", body=").append(Arrays.toString(body));
        sb.append('}');
        return sb.toString();
    }
}
