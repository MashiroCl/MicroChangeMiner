package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import org.mashirocl.editscript.EditScriptStorer;
import org.mashirocl.location.RangeOperations;

import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/02/27 11:01
 */
public class RemoveConditionBlock implements MicroChangePattern{
    /**
     * The if-block is totally removed: e.g. https://github.com/bennidi/mbassador/commit/687fbb72ed2f716332e4ff08229b8566e31d4f91#diff-0ad7c0256f9c36005dd4f9e162e91a593bc5875072d38d5f23f85fb1d7dc7fc3L129
     * condition:
     * for one action in the edit script
     * 1. is `delete-tree`
     * 2. action node is IfStatement
     * @return
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
        return action.getName().equals("delete-tree")
                && (action.getNode().getType().name.equals("IfStatement")
//                || (action.getNode().getType().name.equals("Block")
//                && action.getNode().getChild(0).getType().name.equals("IfStatement"))  // if-else will be regarded as a block whose first child is ifstatement instead of a ifstatement
                                                                                                // e.g. https://github.com/bennidi/mbassador/commit/6aadcbe036b535732ef54f49cebc6498eb3f2d62#diff-f22cf66dd16218616d65318a7d619e2da1a4721f765feed134a91d1d86d59c80L355
        );
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        return matchConditionGumTree(action, mappings);
    }

    @Override
    public SrcDstRange getSrcDstRange(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        SrcDstRange srcDstRange = new SrcDstRange();
        srcDstRange.getSrcRange().add(RangeOperations.toLineRange(RangeOperations.toRange(action.getNode()),
                editScriptStorer.getSrcCompilationUnit()));
        return srcDstRange;
    }
}
