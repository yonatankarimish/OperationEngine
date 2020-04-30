package com.sixsense.model.events;

import com.sixsense.model.commands.Block;
import com.sixsense.io.Session;

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
