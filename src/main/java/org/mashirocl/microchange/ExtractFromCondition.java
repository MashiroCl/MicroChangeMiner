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
 * @author mashirocl@gmail.com
 * @since 2024/01/25 15:57
 */

@Slf4j
public class ExtractFromCondition implements MicroChangePattern{
    /**
     * is `move-tree`
     * action.getnode() is in the if condition expression of an ifstatement
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
        if(!action.getName().equals("move-tree")) return false;
        Tree leftIfNode = NodePosition.isInIf(action.getNode());

        if(leftIfNode!=null){
            if(nodeActions.containsKey(leftIfNode)) log.info("nodeActions.get(leftIfNode) {}", nodeActions.get(leftIfNode));
            // the condition on the left side is removed
            // Note the GumTree misIdentify the following case. To increase accuracy, not consider the following case
            // e.g. https://github.com/MashiroCl/my-refactoring-toy-example/commit/2bab7ef422f5a211224dc303d800068abe93e448: the !flag will be regarded as matched
            //                    else {     // the moved Expression is in if, but different with the original one
            //                        if(!mappings.containsKey(leftIfNode)){
            //                            return true;
            //                        }else {
            //                            return !mappings.get(leftIfNode).equals(rightIfNode);
            //                        }
            //                    }
            return nodeActions.containsKey(leftIfNode) && nodeActions.get(leftIfNode).stream().anyMatch(a -> a.getName().equals("delete-tree") || a.getName().equals("delete-node"));
            }


//        Tree leftConditionExpressionNode = isInIfCondition(action.getNode());
//        if(leftConditionExpressionNode!=null){
//            Tree leftIfNode = leftConditionExpressionNode.getParent();
//            if(nodeActions.containsKey(leftIfNode) && nodeActions.get(leftIfNode).stream().anyMatch(a -> a.getName().equals("delete-tree")||a.getName().equals("delete-node"))) // the condition on the left side is removed
//            {
//                if(mappings.containsKey(action.getNode())){
//                    Tree movedExpression = mappings.get(action.getNode());
//                    // movedExpression is not the condition expression of an If
//                    return isInIfCondition(movedExpression) == null;
//                }
//            }
//        }

//        if(action.getName().equals("move-tree")
//                && action.getNode().getParent().getParent().getType().name.equals("IfStatement")
//                && mappings.containsKey(action.getNode())
//                && !mappings.get(action.getNode()).getParent().getType().name.equals("IfStatement")){
//            if(nodeActions.containsKey(action.getNode().getParent())){  // the then/else block
//                for(Action a:nodeActions.get(action.getNode().getParent())){
//                    if(a.getName().equals("delete-node") || a.getName().equals("delete-tree")){
//                        return true;
//                    }
//                }

//            }
//        }
        return false;
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
