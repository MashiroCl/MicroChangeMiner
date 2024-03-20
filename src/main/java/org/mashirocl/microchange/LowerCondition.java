package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import com.google.common.collect.Range;
import org.mashirocl.editscript.EditScriptStorer;
import org.mashirocl.location.RangeOperations;
import org.mashirocl.microchange.common.NodePosition;

import java.util.List;
import java.util.Map;

/**
 *  A condition is moved from a higher logical level to a lower if-statement
 *  In contrast to LiftCondition
 * @author mashirocl@gmail.com
 * @since 2024/03/20 13:39
 */
public class LowerCondition implements MicroChangePattern{

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
        return false;
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        if(action.getName().equals("move-tree")){
            Tree beforeMoveConditionalNode = NodePosition.isConditionExpression(action.getNode());
            // before move, it is in conditional expression
            if(beforeMoveConditionalNode!=null){
                Tree beforeMoveIfNode = beforeMoveConditionalNode.getParent();
                Tree beforeMoveLowerLevelIfNode = NodePosition.isInIf(beforeMoveIfNode.getParent());
                if(mappings.containsKey(action.getNode()) && beforeMoveLowerLevelIfNode!=null){
                    // after move, it is in a conditional expression in a lower logical level than the original one
                    Tree afterMoveConditionalNode = NodePosition.isConditionExpression(mappings.get(action.getNode()));
                    if(afterMoveConditionalNode!=null){
                        Tree afterMoveIfNode = NodePosition.isInIf(afterMoveConditionalNode);
                        return (afterMoveIfNode!=null && mappings.containsKey(beforeMoveLowerLevelIfNode) && mappings.get(beforeMoveLowerLevelIfNode).equals(afterMoveIfNode));
                    }
                }
            }
        }

        return false;
    }

    @Override
    public SrcDstRange getSrcDstRange(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        SrcDstRange srcDstRange = new SrcDstRange();
        // left side conditional expression
        Tree beforeMoveConditionalNode = NodePosition.isConditionExpression(action.getNode());
        Range<Integer> movedCondition = RangeOperations.toLineRange(
                RangeOperations.toRange(beforeMoveConditionalNode), editScriptStorer.getSrcCompilationUnit()
        );
        // right side conditional expression
        Tree afterMoveConditionalNode = NodePosition.isConditionExpression(mappings.get(action.getNode()));
        Range<Integer> dstCondition = RangeOperations.toLineRange(
                RangeOperations.toRange(afterMoveConditionalNode),editScriptStorer.getDstCompilationUnit());

        srcDstRange.getSrcRange().add(movedCondition);
        srcDstRange.getDstRange().add(dstCondition);

        return srcDstRange;
    }
}
