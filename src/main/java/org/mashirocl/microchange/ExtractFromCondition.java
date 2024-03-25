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
 * Remove the if that encapsulate the statements.
 * @author mashirocl@gmail.com
 * @since 2024/01/25 15:57
 */

@Slf4j
public class ExtractFromCondition implements MicroChangePattern{
    /**
     * is `move-tree`
     * action.getnode() is in the then/else part of a if-statement
     *  the ifstatement is removed
     * @param action
     * @param mappings
     * @return
     */
    @Override
    @Deprecated
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
        return false;
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        if(!action.getName().equals("move-tree") || !mappings.containsKey(action.getNode()) || !action.getNode().isIsomorphicTo(mappings.get(action.getNode()))) return false;
        Tree leftIfNode = NodePosition.isInIf(action.getNode());
        if(NodePosition.isConditionExpression(action.getNode())!=null) return false; //not in the conditional expression

        return leftIfNode!=null && nodeActions.containsKey(leftIfNode) && nodeActions.get(leftIfNode).stream().anyMatch(a -> a.getName().equals("delete-tree") || a.getName().equals("delete-node"));
    }


    /**
     * if the node is in the condition expression of an IfStatement, return the condition expression node, else return null
     * @param node
     * @return
     */
    private Tree isInIfCondition(Tree node){
        Tree curNode = node.getParent();
        Tree preNode = node;
        while (curNode!=null && !curNode.isRoot()){
            if(curNode.getType().name.equals("IfStatement") && preNode.equals(curNode.getChild(0))){ //it is ifstatement and the node is in the condition (the 1st child of if)
                return preNode;
            }
            preNode = curNode;
            curNode = curNode.getParent();
        }
        return null;
    }

    @Override
    public SrcDstRange getSrcDstRange(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        SrcDstRange srcDstRange = new SrcDstRange();
        // left side
        // the removed if block
        Tree leftIfNode = NodePosition.isInIf(action.getNode());
        Range<Integer> ifblock = RangeOperations.toLineRange(
                RangeOperations.toRange(leftIfNode), editScriptStorer.getSrcCompilationUnit()
        );

        srcDstRange.getSrcRange().add(ifblock);

        // right side
        // the being moved expression, which used to be in the if-block
        srcDstRange.getDstRange().add(
                RangeOperations.toLineRange(
                        RangeOperations.toRange(mappings.get(action.getNode())),editScriptStorer.getDstCompilationUnit()
                )
        );

        return srcDstRange;
    }

}
