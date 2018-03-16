package com.italtel.iota.demo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.message.KuraPayload;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class VirtualGasMeterGatewayTest {

    private VirtualGasMeterGateway testObj = new VirtualGasMeterGateway();

    @Before
    public void activate() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(VirtualGasMeterGateway.INITIAL_METER_SIZE_PROP_NAME, new Integer(1));
        properties.put(VirtualGasMeterGateway.INITIAL_MEASURE_PROP_NAME, new Double(500));
        properties.put(VirtualGasMeterGateway.INITIAL_TEMEPERATURE_PROP_NAME, new Double(15));
        properties.put(VirtualGasMeterGateway.INITIAL_BATTERY_LEVEL_PROP_NAME, new Double(100));
        properties.put(VirtualGasMeterGateway.AUTO_RELOAD_BATTERY_LEVEL_PROP_NAME, new Double(2));
        properties.put(VirtualGasMeterGateway.LOW_BATTERY_LEVEL_PROP_NAME, new Double(10));
        properties.put(VirtualGasMeterGateway.MAX_BATTERY_LEVEL_CONSUMPTION_PROP_NAME, new Double(0.01));
        properties.put(VirtualGasMeterGateway.MAX_CONSUMPTION_PROP_NAME, new Double(0.16));
        properties.put(VirtualGasMeterGateway.MAX_TEMEPERATURE_DEVIATION_PROP_NAME, new Double(0.5));
        properties.put(VirtualGasMeterGateway.PUBLISH_ALERT_TOPIC_PROP_NAME, "alert");
        properties.put(VirtualGasMeterGateway.PUBLISH_CRON_EXPR_PROP_NAME, "0 0 0/1 1/1 * ? *");
        properties.put(VirtualGasMeterGateway.PUBLISH_QOS_PROP_NAME, new Integer(0));
        properties.put(VirtualGasMeterGateway.PUBLISH_RETAIN_PROP_NAME, false);
        properties.put(VirtualGasMeterGateway.PUBLISH_TOPIC_PROP_NAME, "measure");
        properties.put(VirtualGasMeterGateway.REFERENCE_LOCATION_PROP_NAME, "45.474979 9.034319");

        testObj.setCloudService(new CloudService() {

            @Override
            public CloudClient newCloudClient(final String applicationId) throws KuraException {
                return new CloudClient() {

                    @Override
                    public void unsubscribe(String arg0, String arg1) throws KuraException {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void unsubscribe(String arg0) throws KuraException {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void subscribe(String arg0, String arg1, int arg2) throws KuraException {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void subscribe(String arg0, int arg1) throws KuraException {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void removeCloudClientListener(CloudClientListener arg0) {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void release() {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public int publish(String arg0, String arg1, byte[] arg2, int arg3, boolean arg4, int arg5)
                            throws KuraException {
                        // TODO Auto-generated method stub
                        return 0;
                    }

                    @Override
                    public int publish(String arg0, String arg1, KuraPayload arg2, int arg3, boolean arg4, int arg5)
                            throws KuraException {
                        // TODO Auto-generated method stub
                        return 0;
                    }

                    @Override
                    public int publish(String arg0, byte[] arg1, int arg2, boolean arg3, int arg4)
                            throws KuraException {
                        // TODO Auto-generated method stub
                        return 0;
                    }

                    @Override
                    public int publish(String arg0, KuraPayload arg1, int arg2, boolean arg3, int arg4)
                            throws KuraException {
                        // TODO Auto-generated method stub
                        return 0;
                    }

                    @Override
                    public int publish(String arg0, String arg1, KuraPayload arg2, int arg3, boolean arg4)
                            throws KuraException {
                        // TODO Auto-generated method stub
                        return 0;
                    }

                    @Override
                    public int publish(String arg0, KuraPayload arg1, int arg2, boolean arg3) throws KuraException {
                        // TODO Auto-generated method stub
                        return 0;
                    }

                    @Override
                    public boolean isConnected() {
                        return true;
                    }

                    @Override
                    public List<Integer> getUnpublishedMessageIds() throws KuraException {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public List<Integer> getInFlightMessageIds() throws KuraException {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public List<Integer> getDroppedInFlightMessageIds() throws KuraException {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public String getApplicationId() {
                        return applicationId;
                    }

                    @Override
                    public void controlUnsubscribe(String arg0, String arg1) throws KuraException {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void controlUnsubscribe(String arg0) throws KuraException {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void controlSubscribe(String arg0, String arg1, int arg2) throws KuraException {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void controlSubscribe(String arg0, int arg1) throws KuraException {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public int controlPublish(String arg0, String arg1, byte[] arg2, int arg3, boolean arg4, int arg5)
                            throws KuraException {
                        // TODO Auto-generated method stub
                        return 0;
                    }

                    @Override
                    public int controlPublish(String arg0, String arg1, KuraPayload arg2, int arg3, boolean arg4,
                            int arg5) throws KuraException {
                        // TODO Auto-generated method stub
                        return 0;
                    }

                    @Override
                    public int controlPublish(String arg0, KuraPayload arg1, int arg2, boolean arg3, int arg4)
                            throws KuraException {
                        // TODO Auto-generated method stub
                        return 0;
                    }

                    @Override
                    public void addCloudClientListener(CloudClientListener arg0) {
                        // TODO Auto-generated method stub

                    }
                };
            }

            @Override
            public boolean isConnected() {
                return true;
            }

            @Override
            public String[] getCloudApplicationIdentifiers() {
                return null;
            }
        });

        testObj.activate(null, properties);
    }

    @After
    public void deactivate() {
        testObj.deactivate(null);
    }

    @Test
    public void test() {
        
    }
}