package com.SixSense.mocks;

import com.SixSense.util.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TestingMocks {
    private static final Logger logger = LogManager.getLogger(TestingMocks.class);

    public static void testCompressionAndDecompression(){
        String filename = "";
        try {
            filename = "/tmp/one_megabyte.txt";
            FileUtils.compress(filename);
            logger.info("File " + filename + " compressed successfully");
        }catch (Exception e){
            logger.error("Failed to compress file "+filename, e);
        }

        try {
            filename = "/tmp/one_megabyte.txt";
            String filenameCompressed = filename+".lz4";
            FileUtils.decompress(filenameCompressed, "/tmp/one_megabyte_restored.txt");
            logger.info("File " + filename + " compressed successfully");
        }catch (Exception e){
            logger.error("Failed to decompress file "+filename, e);
        }
    }
}
