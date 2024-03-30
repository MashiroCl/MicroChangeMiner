package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.tree.Tree;
import com.google.common.collect.Range;
import org.mashirocl.editscript.EditScriptStorer;
import org.mashirocl.location.RangeOperations;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/24 17:06
 */
public class ConditionalToTernaryOperator implements MicroChangePattern{
    /**
     * condition:
     * for one action in the edit script
     * 1. is `move-tree`
     * 2. action.getnode() parent is `IfStatement`
     * 3. the move target is `ConditionalExpression`
     * @return
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
        return action.getName().equals("move-tree")
                && action.getNode().getParent().getType().name.equals("IfStatement")
                && mappings.containsKey(action.getNode())
                && mappings.get(action.getNode()).getParent().getType().name.equals("ConditionalExpression");

    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        return matchConditionGumTree(action, mappings);
    }

    @Override
    public SrcDstRange getSrcDstRange(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        SrcDstRange srcDstRange = new SrcDstRange();
        // left side
        // being moved condition
        Range<Integer> movedCondition = RangeOperations.toLineRange(
                RangeOperations.toRange(action.getNode()), editScriptStorer.getSrcCompilationUnit()
        );
        srcDstRange.getSrcRange().add(movedCondition);
        // being removed expressions in Then & Else
        Tree ifStatement = action.getNode().getParent();
        for(Tree node:ifStatement.getChildren()){
            if(!nodeActions.containsKey(node)) {
                continue;
            }
            for(Action a: nodeActions.get(node)){
                srcDstRange.getSrcRange().add(
                        RangeOperations.toLineRange(
                                RangeOperations.toRange(a.getNode()), editScriptStorer.getSrcCompilationUnit()
                        ));
            }
        }
        //right side
        // added ternary expression
        srcDstRange.getDstRange().add(
                RangeOperations.toLineRange(
                        RangeOperations.toRange((mappings.get(action.getNode())).getParent()), editScriptStorer.getDstCompilationUnit()
                )
        );

        return srcDstRange;
    }
}
