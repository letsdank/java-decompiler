package net.letsdank.jd.cfg;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * CFG одного метода.
 */
public final class ControlFlowGraph {
    private final List<BasicBlock> blocks;
    private final BasicBlock entryBlock;
    private final Map<Integer, BasicBlock> blocksByStartOffset;

    public ControlFlowGraph(List<BasicBlock> blocks, BasicBlock entryBlock,
                            Map<Integer, BasicBlock> blocksByStartOffset) {
        this.blocks = List.copyOf(blocks);
        this.entryBlock = entryBlock;
        this.blocksByStartOffset = Map.copyOf(blocksByStartOffset);
    }

    public List<BasicBlock> blocks() {
        return Collections.unmodifiableList(blocks);
    }

    public BasicBlock entryBlock() {
        return entryBlock;
    }

    public BasicBlock blockByStartOffset(int offset) {
        return blocksByStartOffset.get(offset);
    }
}
