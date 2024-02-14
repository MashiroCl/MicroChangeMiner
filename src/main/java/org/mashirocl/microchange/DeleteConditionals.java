package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import org.mashirocl.editscript.EditScriptStorer;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/25 15:57
 */
public class DeleteConditionals implements MicroChangePattern{
    /**
     * is `move-tree`
     * action.getnode() parent is `IfStatement`
     *  the parent is removed
     * @param action
     * @param mappings
     * @return
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
//        System.out.println("**************");
//        if(action.getName().equals("move-tree")
//                && action.getNode().getParent().getParent().getType().name.equals("IfStatement")
//                && mappings.containsKey(action.getNode())){
//
//            System.out.println(mappings.get(action.getNode()));
//            System.out.println(mappings.get(action.getNode()).getParent());
//            System.out.println(mappings.get(action.getNode()).getParent().getParent().toTreeString());
//        }
//        System.out.println("-----------------------");

        return action.getName().equals("move-tree")
                && action.getNode().getParent().getParent().getType().name.equals("IfStatement")
                && mappings.containsKey(action.getNode())
                && !mappings.get(action.getNode()).getParent().getType().name.equals("IfStatement")
                && mappings.get(action.getNode()).getParent().getParent().getType().name.equals("Block");
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        if(action.getName().equals("move-tree")
                && action.getNode().getParent().getParent().getType().name.equals("IfStatement")
                && mappings.containsKey(action.getNode())
                && !mappings.get(action.getNode()).getParent().getType().name.equals("IfStatement")){
            if(nodeActions.containsKey(action.getNode().getParent())){  // the then/else block
                for(Action a:nodeActions.get(action.getNode().getParent())){
                    if(a.getName().equals("delete-node") || a.getName().equals("delete-tree")){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public List<Position> getPosition(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        List<Position> positions = new LinkedList<>();
        // left side
        // the expression moved out from the if-block
        Position expression = new Position(
                editScriptStorer.getSrcCompilationUnit().getLineNumber(action.getNode().getPos()),
                editScriptStorer.getSrcCompilationUnit().getLineNumber(action.getNode().getEndPos())
        );
        positions.add(expression);
        // being removed if
        if(nodeActions.containsKey(action.getNode().getParent())){
            for(Action a:nodeActions.get(action.getNode().getParent())){
                System.out.println("action");
                System.out.println(a);
                if(a.getName().equals("delete-node") || a.getName().equals("delete-tree")){
                    positions.add(new Position(
                            editScriptStorer.getSrcCompilationUnit().getLineNumber(a.getNode().getPos()),
                            editScriptStorer.getSrcCompilationUnit().getLineNumber(a.getNode().getEndPos())
                    ));
                }
            }
        }

        positions.add(new Position(
                editScriptStorer.getSrcCompilationUnit().getLineNumber(action.getNode().getPos()),
                editScriptStorer.getSrcCompilationUnit().getLineNumber(action.getNode().getEndPos())
        ));
        return positions;
    }
}
