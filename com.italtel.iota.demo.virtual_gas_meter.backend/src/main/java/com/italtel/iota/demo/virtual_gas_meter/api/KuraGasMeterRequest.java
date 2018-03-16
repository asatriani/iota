package com.italtel.iota.demo.virtual_gas_meter.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Date;

/**
 * Created by satriani on 11/07/2017.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KuraGasMeterRequest {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Request {

        @JsonProperty(value = "request.id", required = true)
        private String id;

        @JsonProperty(value = "requester.client.id", required = true)
        private String requesterId;

        @JsonProperty("meter.id")
        private String meterId;

        @JsonProperty("full.fetch")
        private boolean fullFetch;

        @JsonProperty("meter.size")
        private Integer meterSize;

        @JsonProperty("meter.prefix.name")
        private String meterPrefixName;

        @JsonProperty("start.timestamp")
        private Long startTimestamp;

        public Request() {
        }

        public Request(String id, String requesterId) {
            this.id = id;
            this.requesterId = requesterId;
        }

        public Request(String id, String requesterId, String meterId) {
            this(id, requesterId);
            this.meterId = meterId;
        }

        public Request(String id, String requesterId, Integer meterSize,
                       String meterPrefixName, Long startTimestamp) {
            this(id, requesterId);
            this.meterSize = meterSize;
            this.meterPrefixName = meterPrefixName;
            this.startTimestamp = startTimestamp;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getRequesterId() {
            return requesterId;
        }

        public void setRequesterId(String requesterId) {
            this.requesterId = requesterId;
        }

        public String getMeterId() {
            return meterId;
        }

        public void setMeterId(String meterId) {
            this.meterId = meterId;
        }

        public boolean isFullFetch() {
            return fullFetch;
        }

        public void setFullFetch(boolean fullFetch) {
            this.fullFetch = fullFetch;
        }

        public Integer getMeterSize() {
            return meterSize;
        }

        public void setMeterSize(Integer meterSize) {
            this.meterSize = meterSize;
        }

        public String getMeterPrefixName() {
            return meterPrefixName;
        }

        public void setMeterPrefixName(String meterPrefixName) {
            this.meterPrefixName = meterPrefixName;
        }

        public Long getStartTimestamp() {
            return startTimestamp;
        }

        public void setStartTimestamp(Long startTimestamp) {
            this.startTimestamp = startTimestamp;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Request{");
            sb.append("id='").append(id).append('\'');
            sb.append(", requesterId='").append(requesterId).append('\'');
            sb.append(", meterId='").append(meterId).append('\'');
            sb.append(", fullFetch=").append(fullFetch);
            sb.append(", meterSize=").append(meterSize);
            sb.append(", meterPrefixName='").append(meterPrefixName).append('\'');
            sb.append(", startTimestamp=").append(startTimestamp);
            sb.append('}');
            return sb.toString();
        }
    }

    @JsonProperty("sentOn")
    private long timestamp;

    @JsonProperty(value = "metrics", required = true)
    private Request request;

    @JsonProperty
    private byte[] body;

    public KuraGasMeterRequest() {
        this.timestamp = new Date().getTime();
    }

    public KuraGasMeterRequest(Request request) {
        this();
        this.request = request;
    }

    public KuraGasMeterRequest(Request request, byte[] body) {
        this(request);
        this.body = body;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("KuraGasMeterRequest{");
        sb.append("timestamp=").append(timestamp);
        sb.append(", request=").append(request);
        sb.append(", body=").append(Arrays.toString(body));
        sb.append('}');
        return sb.toString();
    }

}
