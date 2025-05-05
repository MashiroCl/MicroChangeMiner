package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import com.google.common.collect.Range;
import lombok.extern.slf4j.Slf4j;
import org.mashirocl.editscript.EditScriptStorer;
import org.mashirocl.location.RangeOperations;
import org.mashirocl.microchange.common.NodePosition;

import java.util.List;
import java.util.Map;

/**
 * a condition is moved from a nested if-statement to a higher, more outer logical level
 * @author mashirocl@gmail.com
 * @since 2024/01/26 9:26
 */
@Slf4j
public class MoveInwardCondition implements MicroChangePattern{
    /**
     * 1. is `move-tree`
     * 2. it is moving from a nested conditional-expression
     * 3. to another conditional-expression
     * 4. the if expression after move is in a higher logical level than the previous one
     * @param action
     * @param mappings
     * @return
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
        return false;
    }

    /**
     *
     * @param action
     * @param mappings
     * @param nodeActions
     * @return
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        // one condition in the if statement is moved to a higher, more outer logical level
        if(action.getName().equals("move-tree")){
            Tree beforeMoveConditionalNode = NodePosition.isIfConditionExpression(action.getNode());
            // before move, it is in conditional expression
            if(beforeMoveConditionalNode!=null){
                if(mappings.containsKey(action.getNode())){
                    // after move, it is in a conditional expression in a higher logical level than the original one
                    Tree afterMoveConditionalNode = NodePosition.isIfConditionExpression(mappings.get(action.getNode()));
                    if(afterMoveConditionalNode!=null){
                        Tree afterMoveHigherLevelIfNode = afterMoveConditionalNode.getParent();
                        Tree afterMoveLowerLevelIfNode = NodePosition.isInIf(afterMoveHigherLevelIfNode.getParent());
                        return (afterMoveLowerLevelIfNode!=null && mappings.containsKey(beforeMoveConditionalNode.getParent()) && mappings.get(beforeMoveConditionalNode.getParent()).equals(afterMoveLowerLevelIfNode));
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
        Tree beforeMoveConditionalNode = NodePosition.isIfConditionExpression(action.getNode());
        Range<Integer> movedCondition = RangeOperations.toLineRange(
                RangeOperations.toRange(beforeMoveConditionalNode), editScriptStorer.getSrcCompilationUnit()
        );
        // right side conditional expression
        Tree afterMoveConditionalNode = NodePosition.isIfConditionExpression(mappings.get(action.getNode()));
        Range<Integer> dstCondition = RangeOperations.toLineRange(
                RangeOperations.toRange(afterMoveConditionalNode),editScriptStorer.getDstCompilationUnit());

        srcDstRange.getSrcRange().add(movedCondition);
        srcDstRange.getDstRange().add(dstCondition);

        return srcDstRange;
    }

    private boolean isAnyChildNodeIsMovedToAIfStatement(List<Tree> nodes,Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, Tree skip){
        for(Tree n:nodes){
            if(n.equals(skip)) continue;
            if(nodeActions.containsKey(n)){
                List<Action> actions = nodeActions.get(n);
                for(Action a:actions){
//                    if(a.getName().equals("move-tree")){
//                        System.out.println(a.getNode());
//                        System.out.println(a.getNode().getParent());
//                        System.out.println(a.getNode().getParent().getParent());
//                        System.out.println(mappings.containsKey(a.getNode()));
//                        if(mappings.containsKey(a.getNode())){
//                            System.out.println(mappings.get(a.getNode()).getParent());
//                        }
//                    }
                    if(a.getName().equals("move-tree")
                            && a.getNode().getParent().getParent().getType().toString().equals("IfStatement")
                            && mappings.containsKey(a.getNode())
                            && mappings.get(a.getNode()).getParent().getType().toString().equals("IfStatement")){
                        return true;
                    }
                }
            }

        }
        return false;
    }
}
