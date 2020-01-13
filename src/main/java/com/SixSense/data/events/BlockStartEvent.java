package com.SixSense.data.events;

import com.SixSense.data.commands.Block;
import com.SixSense.io.Session;

public class BlockStartEvent extends AbstractEngineEvent {
    private Block block;

    public BlockStartEvent(Session session, Block block) {
        super(EngineEventType.BlockStart, session);
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }
}
