package com.SixSense.engine;

import com.SixSense.SixSenseBaseTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.Test;

@Test(groups = {"engine"})
public class RemoteOperationTests extends SixSenseBaseTest {
    private static final Logger logger = LogManager.getLogger(RemoteOperationTests.class);

    public void f5BigIpBackup(){
        logger.info("this should test f5 backups sometime...");
    }
}
