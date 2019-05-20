package com.SixSense.data;

import com.SixSense.SixSenseBaseTest;
import com.SixSense.data.commands.Block;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.Test;

@Test(groups = {"data"})
public class BlockTests extends SixSenseBaseTest {
    private static final Logger logger = LogManager.getLogger(BlockTests.class);


    public void testChaining(){
        Block a = new Block();
        Block b = new Block();

        logger.info("test chaining of commands");
    }
}
