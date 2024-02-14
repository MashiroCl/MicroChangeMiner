package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.tree.Tree;
import org.mashirocl.editscript.EditScriptStorer;

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
                && ((Move)action).getParent().getType().name.equals("ConditionalExpression");

    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        return matchConditionGumTree(action, mappings);
    }

    @Override
    public List<Position> getPosition(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        List<Position> positions = new LinkedList<>();
        // left side
        // being moved condition
        Position movedCondition = new Position(
                editScriptStorer.getSrcCompilationUnit().getLineNumber(action.getNode().getPos()),
                editScriptStorer.getSrcCompilationUnit().getLineNumber(action.getNode().getEndPos()));
        positions.add(movedCondition);
        // being removed expressions in Then & Else
        Tree ifStatement = action.getNode().getParent();
        for(Tree node:ifStatement.getChildren()){
            for(Action a: nodeActions.get(node)){
                positions.add(new Position(
                        editScriptStorer.getSrcCompilationUnit().getLineNumber(a.getNode().getPos()),
                        editScriptStorer.getSrcCompilationUnit().getLineNumber(a.getNode().getEndPos())));
            }
        }
        //right side
        // added ternary expression
        positions.add(new Position(
                editScriptStorer.getSrcCompilationUnit().getLineNumber(((Move)action).getParent().getPos()),
                editScriptStorer.getSrcCompilationUnit().getLineNumber(((Move)action).getParent().getEndPos())
        ));
        return positions;
    }
}
