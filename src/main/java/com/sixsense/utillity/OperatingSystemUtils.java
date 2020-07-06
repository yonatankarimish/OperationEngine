package com.sixsense.utillity;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;

public class OperatingSystemUtils {
    private static final Logger logger = LogManager.getLogger(OperatingSystemUtils.class);
    static String localPartition;
    static int localPartitionBlockSize;

    private OperatingSystemUtils(){
        /*Empty private constructor - no instances of this class should be created */
    }

    //obtain block size for local installation partition to optimize buffered reads
    static{
        LocalShell localShell = new LocalShell();
        try {
            LocalShellResult result = localShell.runCommand("df -h | grep sixsense | awk '{print $1}'");
            if(result.getExitCode() == 0){
                localPartition = result.getOutput().get(0);
            }else{
                throw new RuntimeException("Failed to obtain local partition from local shell. Caused by: " + result.getErrors().get(0));
            }

            result = localShell.runCommand("blockdev --getbsz " + localPartition);
            if(result.getExitCode() == 0){
                localPartitionBlockSize = Integer.parseInt(result.getOutput().get(0));
            }else{
                throw new RuntimeException("Failed to obtain local partition block size from local shell. Caused by: " + result.getErrors().get(0));
            }
        } catch (Exception e) {
            logger.warn("Failed to obtain block size for local partition name. Caused by: " + e.getMessage());
        }
    }

    public static void finalizeCloseableResource(Closeable... closeables){
        for(Closeable resource : closeables) {
            if(resource == null){
                logger.warn("Attempted to finalize a null resource");
            }else{
                try {
                    resource.close();
                } catch (Exception e) {
                    String resourceClassName = closeables.getClass().toString();
                    logger.error("Failed to close instance of " + resourceClassName + ". Caused by: " + e.getMessage());
                }
            }
        }
    }
}
