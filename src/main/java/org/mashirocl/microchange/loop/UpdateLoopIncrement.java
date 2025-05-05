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
 * @since 2025/04/30 17:15
 *
 *  the loop increment is updated e.g. (for(int i=0;i<n;i++) -> for(int i=0;i<n+1;i+=2)
 *
 *  changes applied to the increment part, and the number of increment statements is not changed
 *
 */
public class UpdateLoopIncrement implements MicroChangePattern {
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
        return false;
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        Tree forChild = NodePosition.isDescendantOfForLoopHeader(action.getNode());
        if(forChild==null) return false;
        if(!NodePosition.isForLoopIncrement(action.getNode())) return false;

        if(!mappings.containsKey(forChild.getParent())) return false;

        Map<String, int[]> oldIndexes =  NodePosition.decomposeForLoopHeader(forChild.getParent());
        Map<String, int[]> newIndexes =  NodePosition.decomposeForLoopHeader(mappings.get(forChild.getParent()));
        if(!oldIndexes.containsKey("Increment") || !newIndexes.containsKey("Increment")) return false;
        int oldIncrementStatementSize = oldIndexes.get("Increment")[1]-oldIndexes.get("Increment")[0]+1;
        int newIncrementStatementSize = newIndexes.get("Increment")[1]-newIndexes.get("Increment")[0]+1;

        return oldIncrementStatementSize==newIncrementStatementSize;
    }

    @Override
    public SrcDstRange getSrcDstRange(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        SrcDstRange srcDstRange = new SrcDstRange();
        // source for-loop header
        srcDstRange.getSrcRange().add(RangeOperations.toLineRange(RangeOperations.toRange(action.getNode()),
                editScriptStorer.getSrcCompilationUnit()));

        //destination for-loop header
        srcDstRange.getDstRange().add(RangeOperations.toLineRange(RangeOperations.toRange(action.getNode()),
                editScriptStorer.getDstCompilationUnit()));
        return srcDstRange;
    }
}
