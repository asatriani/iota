package com.italtel.iota.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataStoreUtils {

    private static final Logger s_logger = LoggerFactory.getLogger(DataStoreUtils.class);

    private static final String METERS_STORE_FILE = "/opt/eclipse/kura/load/meters.store";

    @SuppressWarnings("unchecked")
    public static Map<String, VirtualGasMeter> restoreMeterMapFromFile(VirtualGasMeterGateway gw) {
        Map<String, VirtualGasMeter> result = Collections.synchronizedMap(new HashMap<>());

        File storeFile = new File(METERS_STORE_FILE);
        if (storeFile.exists()) {
            s_logger.info("Found meters store file");
            FileInputStream inputStream = null;
            ObjectInputStream objectInputStream = null;
            try {
                inputStream = new FileInputStream(storeFile);
                objectInputStream = new ObjectInputStream(inputStream);
                result = (Map<String, VirtualGasMeter>) objectInputStream.readObject();
                for (VirtualGasMeter m : result.values()) {
                    m.setVirtualGasMeterGateway(gw);
                }
                s_logger.info("Restored meters from file");
            } catch (Exception e) {
                s_logger.error("Error restoring meters from file: {}", e.getMessage(), e);
            } finally {
                if (objectInputStream != null) {
                    try {
                        objectInputStream.close();
                    } catch (Exception ex) {
                    }
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception ex) {
                    }
                }
            }
        }
        return result;
    }

    public static void storeMeterMapInFile(Map<String, VirtualGasMeter> meters) {
        try {
            File meterStoreFileTmp = new File(METERS_STORE_FILE + ".temp");
            if (meterStoreFileTmp.exists()) {
                meterStoreFileTmp.delete();
            }
            meterStoreFileTmp.createNewFile();

            FileOutputStream outputStream = new FileOutputStream(meterStoreFileTmp);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(meters);
            objectOutputStream.flush();
            outputStream.close();

            File meterStoreFile = new File(METERS_STORE_FILE);
            if (meterStoreFile.exists()) {
                meterStoreFile.delete();
            }
            meterStoreFileTmp.renameTo(meterStoreFile);
            s_logger.info("Meters stored in file");
        } catch (Exception e) {
            s_logger.error("Error storing meters in file: {}", e.getMessage(), e);
        }
    }
}
