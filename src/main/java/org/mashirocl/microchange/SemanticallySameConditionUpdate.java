package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import lombok.extern.slf4j.Slf4j;
import org.mashirocl.editscript.EditScriptStorer;
import org.mashirocl.location.RangeOperations;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/21 12:29
 */
@Slf4j
public class SemanticallySameConditionUpdate implements MicroChangePattern{

    /**
     * condition
     * 1. action node is in the if condition expression of an IfStatement
     * 2. Except the condition expression, the Then & Else parts of the IfStatement are not changed
     * @param action
     * @param mappings
     * @return
     */

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings){
        Tree curNode = action.getNode().getParent();
        Tree preNode = action.getNode();
        while (curNode!=null && !curNode.isRoot()){
            if(curNode.getType().name.equals("IfStatement") && preNode.equals(curNode.getChild(0))){ //it is ifstatement and the node is in the condition (the 1st child of if)
                if(mappings.containsKey(curNode) && mappings.get(curNode).getChildren().size()==curNode.getChildren().size()){
                    for(int i=1;i<mappings.get(curNode).getChildren().size();i++){
                        if(!mappings.get(curNode).getChild(i).isIsomorphicTo(curNode.getChild(i))){
                            return false;
                        }
                    }
                    return true;
                }
            }
            preNode = curNode;
            curNode = curNode.getParent();
        }

        return false;
    }


    public boolean matchConditionGumTree2(Action action, Map<Tree, Tree> mappings) {
//        System.out.println("insert node");
//        System.out.println(action.getName().equals("insert-node"));
//        System.out.println("parent");
//        System.out.println(action.getNode().getParent().getType().name);

        if(!action.getName().equals("insert-node")
                || !action.getNode().getParent().getType().name.equals("IfStatement"))
            return false;

//        System.out.println("parent mappings");
        Tree ifStatementBefore = action.getNode().getParent();
        if(!mappings.containsKey(action.getNode().getParent())
                || !mappings.get(action.getNode().getParent()).getType().name.equals("IfStatement"))
            return false;

//        System.out.println(mappings.get(action.getNode().getParent()).getType().name);
//        System.out.println("children");
//        System.out.println(ifStatementBefore.getChildren());
        Tree ifStatementAfter = mappings.get(action.getNode().getParent());
        if(ifStatementBefore.getChildren().isEmpty()
                ||ifStatementBefore.getChildren().size()!=ifStatementAfter.getChildren().size())
            return false;

//        System.out.println(ifStatementAfter.getChildren());
        for(int i=1;i<ifStatementBefore.getChildren().size();i++){
            if(!mappings.containsKey(ifStatementBefore.getChild(i))
            || !ifStatementBefore.getChild(i).isIsomorphicTo(ifStatementAfter.getChild(i)))
                return false;
        }


        return true;
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        return matchConditionGumTree(action, mappings);
    }

    @Override
    public SrcDstRange getSrcDstRange(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        SrcDstRange srcDstRange = new SrcDstRange();
        Tree ifConditionExpressionNode = action.getNode();

        while(!ifConditionExpressionNode.getType().name.equals("IfStatement")){
            ifConditionExpressionNode = ifConditionExpressionNode.getParent();
        }

        if(action.getName().equals("insert-node") || action.getName().equals("insert-tree")){
            // right side: condition
            srcDstRange.getDstRange().add(
                    RangeOperations.toLineRange(
                            RangeOperations.toRange(ifConditionExpressionNode.getChild(0)), editScriptStorer.getDstCompilationUnit()));

            // left side: condition
            srcDstRange.getSrcRange().add(
                    RangeOperations.toLineRange(
                            RangeOperations.toRange(mappings.get(ifConditionExpressionNode).getChild(0)), editScriptStorer.getSrcCompilationUnit()));
        }
        else{
            // left side: condition
            srcDstRange.getSrcRange().add(
                    RangeOperations.toLineRange(
                            RangeOperations.toRange(ifConditionExpressionNode.getChild(0)), editScriptStorer.getSrcCompilationUnit()));

            // right side: condition
            srcDstRange.getDstRange().add(
                    RangeOperations.toLineRange(
                            RangeOperations.toRange(mappings.get(ifConditionExpressionNode).getChild(0)), editScriptStorer.getDstCompilationUnit()));
        }

        return srcDstRange;
    }
}
