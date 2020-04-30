package com.sixsense.model.events;

import com.sixsense.model.commands.Block;
import com.sixsense.model.logic.ExpressionResult;
import com.sixsense.io.Session;

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
