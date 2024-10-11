package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import com.google.common.collect.Range;
import org.mashirocl.editscript.EditScriptStorer;
import org.mashirocl.location.RangeOperations;
import org.mashirocl.microchange.common.NodePosition;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/25 14:16
 */
//TODO debug: if(b || c==2){ -> if(c==2){ will be misidentified as UnifyCondition; move part of the original conditional expression may lead to this
public class UnifyCondition implements MicroChangePattern{
    /**
     * is `move-tree`
     * it is moved from one conditional expression to another conditional expression
     * the two conditional expression should be in the same logical-level
     * @param action
     * @param mappings
     * @return
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
        if(action.getName().equals("move-tree")){
            Tree beforeMoveConditionNodeX = NodePosition.isConditionExpression(action.getNode());
            if(beforeMoveConditionNodeX!=null && mappings.containsKey(action.getNode())){
                Tree beforeMoveIfNodeX = beforeMoveConditionNodeX.getParent();
                Tree afterMoveConditionNodeY = NodePosition.isConditionExpression(mappings.get(action.getNode()));
              if(afterMoveConditionNodeY!=null){
                  // in the same logical level
                  if(mappings.containsKey(afterMoveConditionNodeY.getParent())){
                      Tree beforeMoveIfNodeY = mappings.get(afterMoveConditionNodeY.getParent());
                      return !NodePosition.isDescedantOf(beforeMoveIfNodeY, beforeMoveIfNodeX) && !NodePosition.isDescedantOf(beforeMoveIfNodeX, beforeMoveIfNodeX);
                  }
              }
            }


        }
        return false;
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        return matchConditionGumTree(action,mappings);
    }


    @Override
    public SrcDstRange getSrcDstRange(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        SrcDstRange srcDstRange = new SrcDstRange();
        // left side
        //before move condition X
        Tree beforeMoveConditionNodeX = NodePosition.isConditionExpression(action.getNode());
        srcDstRange.getSrcRange().add(
                RangeOperations.toLineRange(
                        RangeOperations.toRange(beforeMoveConditionNodeX), editScriptStorer.getSrcCompilationUnit()
                ));
        //before move condition Y
        Tree afterMoveConditionNodeY = NodePosition.isConditionExpression(mappings.get(action.getNode()));
        Tree beforeMoveConditionY = mappings.get(afterMoveConditionNodeY.getParent()).getChild(0);
        srcDstRange.getSrcRange().add(
                RangeOperations.toLineRange(
                        RangeOperations.toRange(beforeMoveConditionY), editScriptStorer.getSrcCompilationUnit()
                ));

        //right side
        // after move condition Y
        srcDstRange.getDstRange().add(
                RangeOperations.toLineRange(
                        RangeOperations.toRange(afterMoveConditionNodeY), editScriptStorer.getDstCompilationUnit())
        );

        return srcDstRange;
    }

}
