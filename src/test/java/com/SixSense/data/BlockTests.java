package com.SixSense.data;

import com.SixSense.SixSenseBaseTest;
import com.SixSense.data.commands.*;
import com.SixSense.util.CommandUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

//It might be worth to add test cases for the cartesian product of chaining any two ICommands
@Test(groups = {"data"})
public class BlockTests extends SixSenseBaseTest {
    private static final Logger logger = LogManager.getLogger(BlockTests.class);

    public void testCommandChaining(){
        Command c1 = new Command();
        Command c2 = new Command();

        //chaining two commands wraps them in a block
        ICommand wrapper = CommandUtils.chainCommands(c1, c2);
        Assert.assertTrue(wrapper instanceof Block);

        //the wrapping block must contain both child commands
        List<ICommand> childBlocks = ((Block)wrapper).getChildBlocks();
        Assert.assertTrue(childBlocks.contains(c1));
        Assert.assertTrue(childBlocks.contains(c2));
    }

    public void testBlockChaining(){
        Block original = new Block();
        Block additional = new Block();
        Block b1 = new Block();
        Block b2 = new Block();
        Block b3 = new Block();
        Block b4 = new Block();

        original.addChildBlock(b1).addChildBlock(b2);
        additional.addChildBlock(b3).addChildBlock(b4);

        //chaining blocks adds any additional child blocks to the original, returning the original
        Block merged = (Block)CommandUtils.chainCommands(original, additional);
        Assert.assertEquals(merged, original);
        Assert.assertNotEquals(merged, additional);

        //the original block must contain all child blocks
        List<ICommand> childBlocks = merged.getChildBlocks();
        Assert.assertTrue(childBlocks.contains(b1));
        Assert.assertTrue(childBlocks.contains(b2));
        Assert.assertTrue(childBlocks.contains(b3));
        Assert.assertTrue(childBlocks.contains(b4));
    }

    public void testOperationChaining(){
        Operation o1 = new Operation();
        Operation o2 = new Operation();
        Block eb1 = new Block();
        Block eb2 = new Block();

        Block b1 = new Block();
        Block b2 = new Block();
        Block b3 = new Block();
        Block b4 = new Block();

        eb1.addChildBlock(b1).addChildBlock(b2);
        eb2.addChildBlock(b3).addChildBlock(b4);

        o1.setExecutionBlock(eb1);
        o2.setExecutionBlock(eb2);

        //chaining operations returns the original operation,
        Operation merged = (Operation)CommandUtils.chainCommands(o1, o2);
        Assert.assertEquals(merged, o1);
        Assert.assertNotEquals(merged, o2);

        //the chained operation's execution block is the result of chainCommands(o1, o2)
        Block executionBlock = (Block)merged.getExecutionBlock();
        Assert.assertEquals(executionBlock, eb1);
        Assert.assertNotEquals(executionBlock, eb2);

        //the execution block must contain all child blocks, (due to the nature of block chaining o1, o2)
        List<ICommand> childBlocks = executionBlock.getChildBlocks();
        Assert.assertTrue(childBlocks.contains(b1));
        Assert.assertTrue(childBlocks.contains(b2));
        Assert.assertTrue(childBlocks.contains(b3));
        Assert.assertTrue(childBlocks.contains(b4));
    }

    public void testWorkflowChaining(){
        ParallelWorkflow pw = new ParallelWorkflow();
        Command stub = new Command();

        boolean p1Throwed = false;
        boolean p2Throwed = false;

        try{
            //this is expected to throw an UnsupportedOperationException
            CommandUtils.chainCommands(pw, stub);
        }catch (Exception e){
            if(e instanceof UnsupportedOperationException){
                p1Throwed = true;
            }else{
                Assert.fail("Workflows should always fail to chain; workflows can only be merged");
            }
        }

        try{
            //this is expected to throw an UnsupportedOperationException
            CommandUtils.chainCommands(stub, pw);
        }catch (Exception e){
            if(e instanceof UnsupportedOperationException){
                p2Throwed = true;
            }else{
                Assert.fail("Workflows should always fail to chain; workflows can only be merged");
            }
        }

        Assert.assertTrue(p1Throwed && p2Throwed);
    }
}
