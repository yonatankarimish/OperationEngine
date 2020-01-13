package com.SixSense.data.events;

import com.SixSense.data.commands.Block;
import com.SixSense.data.logic.ExpressionResult;
import com.SixSense.io.Session;

public class BlockEndEvent extends AbstractEngineEvent {
    private Block block;
    private ExpressionResult result;

    public BlockEndEvent(Session session, Block block, ExpressionResult result) {
        super(EngineEventType.BlockEnd, session);
        this.block = block;
        this.result = result;
    }

    public Block getBlock() {
        return block;
    }

    public ExpressionResult getResult() {
        return result;
    }
}
