package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import org.mashirocl.editscript.EditScriptStorer;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/20 17:51
 */
public class ExtendIfWithElse implements MicroChangePattern{

    /**
     * condition
     * 1. action node is `insert-tree`
     * 2. its parent is `IfStatement`
     * 3. action node is the 3rd childrent of its parent
     * @param action
     * @param mappings
     * @return
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
//        System.out.println(action);
//        System.out.println("node");
//        System.out.println(action.getNode());
//        System.out.println("tree");
//        System.out.println(action.getNode().toTreeString());
//        System.out.println("startPos");
//        System.out.println(action.getNode().getPos());
//        System.out.println("endPos");
//        System.out.println(action.getNode().getEndPos());
//        System.out.println("2nd child");
//        System.out.println(action.getNode().getParent().getChild(1));
//        System.out.println(action.getNode().equals(action.getNode().getParent().getChild(1)));
//        System.out.println("3rd child");
//        System.out.println(action.getNode().getParent().getChild(2));
//        System.out.println(action.getNode().equals(action.getNode().getParent().getChild(2)));
        return action.getName().equals("insert-tree")
        && action.getNode().getParent().getType().name.equals("IfStatement")
                // note that the action.getNode() is the node after change
        && action.getNode().getParent().getChildren().size()>2
        && action.getNode().equals(action.getNode().getParent().getChild(2));
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        return matchConditionGumTree(action, mappings);
    }

    @Override
    public List<Position> getPosition(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        List<Position> positions = new LinkedList<>();
        positions.add(new Position(
                editScriptStorer.getDstCompilationUnit().getLineNumber(action.getNode().getPos()),
                editScriptStorer.getDstCompilationUnit().getLineNumber(action.getNode().getEndPos())
        ));
        return positions;
    }
}
