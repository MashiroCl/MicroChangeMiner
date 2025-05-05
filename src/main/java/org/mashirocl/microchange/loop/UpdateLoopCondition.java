package org.mashirocl.microchange.loop;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import org.mashirocl.editscript.EditScriptStorer;
import org.mashirocl.location.RangeOperations;
import org.mashirocl.microchange.MicroChangePattern;
import org.mashirocl.microchange.SrcDstRange;
import org.mashirocl.microchange.common.NodePosition;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2025/04/21 10:32
 */
public class UpdateLoopCondition implements MicroChangePattern {
    /**
     * Changes being applied to the Condition of a for loop header or a while header
     *
     * Changes are applied to the Condition of a for loop header or a while header, and the number of condition statements keep unchanged
     *
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
        return false;
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        // for loop header
        Tree forChild = NodePosition.isDescendantOfForLoopHeader(action.getNode());
        if(forChild!=null && NodePosition.isForLoopCondition(action.getNode()) && mappings.containsKey(forChild.getParent())){
            // number of condition statements keep unchanged
            List<Integer> conditionIndexes = NodePosition.locateForLoopCondition(forChild.getParent());
            int oldConditionStatementsSize = Collections.max(conditionIndexes) - Collections.min(conditionIndexes);

            conditionIndexes = NodePosition.locateForLoopCondition(mappings.get(forChild.getParent()));
            int newConditionStatementsSize = Collections.max(conditionIndexes) - Collections.min(conditionIndexes);

            return oldConditionStatementsSize == newConditionStatementsSize;
        }

        // while loop header
        Tree whileChild = NodePosition.isDescendantOfWhileLoopHeader(action.getNode());
        if(whileChild!=null && mappings.containsKey(whileChild.getParent())){
            // number of condition statements keep unchanged
            int oldConditionStatementSize = whileChild.getParent().getChildren().size()-1;  // the last child of while statement is the body of loop
            int newConditionStatementSize = mappings.get(whileChild.getParent()).getChildren().size()-1;
            return oldConditionStatementSize == newConditionStatementSize;
        }

        return false;

    }


    @Override
    public SrcDstRange getSrcDstRange(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        SrcDstRange srcDstRange = new SrcDstRange();
        // for-statement
        Tree forChild = NodePosition.isDescendantOfForLoopHeader(action.getNode());
        if(forChild!=null){
            // source for-statement
            srcDstRange.getSrcRange().add(RangeOperations.toLineRange(RangeOperations.toRange(forChild.getParent()),
                    editScriptStorer.getSrcCompilationUnit()));

            //destination for-statement
            srcDstRange.getDstRange().add(RangeOperations.toLineRange(RangeOperations.toRange(mappings.get(forChild.getParent())),
                    editScriptStorer.getDstCompilationUnit()));
        }else{
            Tree whileChild = NodePosition.isDescendantOfWhileLoopHeader(action.getNode());
            // source while-statement
            srcDstRange.getSrcRange().add(RangeOperations.toLineRange(RangeOperations.toRange(whileChild.getParent()),
                    editScriptStorer.getSrcCompilationUnit()));
            // destination while-statement
            srcDstRange.getDstRange().add(RangeOperations.toLineRange(RangeOperations.toRange(mappings.get(whileChild.getParent())),
                    editScriptStorer.getDstCompilationUnit()));
        }


        return srcDstRange;
    }
}
