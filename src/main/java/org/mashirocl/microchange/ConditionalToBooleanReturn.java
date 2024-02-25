package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import org.mashirocl.editscript.EditScriptStorer;
import org.mashirocl.location.RangeOperations;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/21 14:50
 */
public class ConditionalToBooleanReturn implements MicroChangePattern{

    /**
     * condition
     * 1. action node is `move-tree`
     * 2. node is the 1st child of its parent, and its parent is the `IfStatement`
     * 3. target is `ReturnStatement`
     * @param action
     * @param mappings
     * @return
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
//        System.out.println(action.getNode().getType());
//        System.out.println(action.getNode().getParent().getType());
//        System.out.println(action);
        return action.getName().equals("move-tree")
                && action.getNode().getParent().getType().name.equals("IfStatement")
                && action.getNode().getParent().getChild(0).equals(action.getNode())
                && mappings.containsKey(action.getNode())
                && mappings.get(action.getNode()).getParent().getType().name.equals("ReturnStatement");

//        System.out.println(action);
//        System.out.println("IfStatementChild");
//        System.out.println(action.getNode().getParent().getChildren());
//        System.out.println("first child");
//        System.out.println(action.getNode().getParent().getChild(0));
//        System.out.println("IfStatementChild mapping");
//        System.out.println(mappings.get(action.getNode().getParent().getChild(0)));
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
                RangeOperations.toRange(action.getNode()), editScriptStorer.getSrcCompilationUnit());
        srcDstRange.getSrcRange().add(movedCondition);
        // being removed return statement in Then & Else
        Tree ifStatement = action.getNode().getParent();
        for(Tree node:ifStatement.getChildren()){
            for(Action a: nodeActions.get(node)){
                srcDstRange.getSrcRange().add(RangeOperations.toLineRange(
                        RangeOperations.toRange(a.getNode()),
                        editScriptStorer.getSrcCompilationUnit()
                ));
            }
        }

        //right side
        // added return statement
        Tree addedStatement = mappings.get(action.getNode()).getParent();
        srcDstRange.getDstRange().add(
                RangeOperations.toLineRange(RangeOperations.toRange(addedStatement), editScriptStorer.getDstCompilationUnit()));

        return srcDstRange;
    }

    public List<Position> getPosition(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        List<Position> positions = new LinkedList<>();
        // left side
        // being moved condition
        Position movedCondition = new Position(
                editScriptStorer.getSrcCompilationUnit().getLineNumber(action.getNode().getPos()),
                editScriptStorer.getSrcCompilationUnit().getLineNumber(action.getNode().getEndPos()));
        positions.add(movedCondition);
        // being removed return statement in Then & Else
        Tree ifStatement = action.getNode().getParent();
        for(Tree node:ifStatement.getChildren()){
            for(Action a: nodeActions.get(node)){
                positions.add(new Position(
                        editScriptStorer.getSrcCompilationUnit().getLineNumber(a.getNode().getPos()),
                        editScriptStorer.getSrcCompilationUnit().getLineNumber(a.getNode().getEndPos())));
            }
        }
        //right side
        // added return statement
        Tree addedStatement = mappings.get(action.getNode()).getParent();
        positions.add(new Position(
                        editScriptStorer.getSrcCompilationUnit().getLineNumber(addedStatement.getPos()),
                        editScriptStorer.getSrcCompilationUnit().getLineNumber(addedStatement.getEndPos())
        ));
        return positions;
    }
}
