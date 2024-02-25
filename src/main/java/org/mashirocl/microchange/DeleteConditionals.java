package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import com.google.common.collect.Range;
import org.mashirocl.editscript.EditScriptStorer;
import org.mashirocl.location.RangeOperations;

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
    public SrcDstRange getSrcDstRange(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        SrcDstRange srcDstRange = new SrcDstRange();
        // left side
        // the expression moved out from the if-block
        Range<Integer> expression = RangeOperations.toLineRange(
                RangeOperations.toRange(action.getNode()), editScriptStorer.getSrcCompilationUnit()
        );

        srcDstRange.getSrcRange().add(expression);
        // being removed if
        if(nodeActions.containsKey(action.getNode().getParent())){
            for(Action a:nodeActions.get(action.getNode().getParent())){
                if(a.getName().equals("delete-node") || a.getName().equals("delete-tree")){
                    srcDstRange.getSrcRange().add(
                            RangeOperations.toLineRange(
                                    RangeOperations.toRange(a.getNode()), editScriptStorer.getSrcCompilationUnit()
                            )
                    );
                }
            }
        }

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
