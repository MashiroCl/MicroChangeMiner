package org.mashirocl.microchange.loop;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import org.mashirocl.editscript.EditScriptStorer;
import org.mashirocl.location.RangeOperations;
import org.mashirocl.microchange.MicroChangePattern;
import org.mashirocl.microchange.SrcDstRange;
import org.mashirocl.microchange.common.NodePosition;

import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2025/04/30 17:00
 */
public class RemoveForLoopIncrement implements MicroChangePattern {
    /**
     * remove an loop increment to the for-loop (for(int i=0;i<n;i++) -> for(int i=0;i<n;))
     *
     * An delete-tree of remove a increment statement into a for loop, the number increment statements decreases.
     */

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
        return false;
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        if(!action.getName().equals("delete-tree")) return false;

        // changes in the loop increment
        Tree forChild = NodePosition.isDescendantOfForLoopHeader(action.getNode());
        if(forChild==null) return false;
        if(!NodePosition.isForLoopIncrement(action.getNode())) return false;

        // increment statement is removed, not existed anymore
        if(mappings.containsKey(action.getNode())) return false;

        // number of increment statements decreased
        Tree oldForStatement = forChild.getParent();
        if(!mappings.containsKey(oldForStatement)) return false;
        Tree newForStatement = mappings.get(oldForStatement);

        Map<String, int[]> oldIndexes =  NodePosition.decomposeForLoopHeader(oldForStatement);
        Map<String, int[]> newIndexes =  NodePosition.decomposeForLoopHeader(newForStatement);

        if(!oldIndexes.containsKey("Increment") && !newIndexes.containsKey("Increment")) return false;

        int oldIncrementStatementSize = oldIndexes.get("Increment")[1]-oldIndexes.get("Increment")[0]+1;

        if(!newIndexes.containsKey("Increment")){
            return true;
        }
        int newIncrementStatementSize = newIndexes.get("Increment")[1]-newIndexes.get("Increment")[0]+1;
        return newIncrementStatementSize < oldIncrementStatementSize;
    }

    @Override
    public SrcDstRange getSrcDstRange(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        SrcDstRange srcDstRange = new SrcDstRange();

        srcDstRange.getSrcRange().add(RangeOperations.toLineRange(RangeOperations.toRange(action.getNode()),
                editScriptStorer.getSrcCompilationUnit()));
        return srcDstRange;
    }
}
