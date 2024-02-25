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
 * @since 2024/01/19 15:19
 */
public class RemoveElse implements MicroChangePattern{

    /**
     * condition:
     * for one action in the edit script
     * 1. is `delete-tree`
     * 2. action.getnode() parent is `IfStatement`
     * 3. action.getnode() is the 3rd child of the parent
     * @param action
     * @param mappings
     * @return
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings){
        return action.getName().equals("delete-tree")
                && action.getNode().getParent().getType().name.equals("IfStatement")
                && action.getNode().getParent().getChildren().size()>2
                && action.getNode().equals(action.getNode().getParent().getChild(2));
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        return matchConditionGumTree(action, mappings);
    }

    @Override
    public SrcDstRange getSrcDstRange(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        SrcDstRange srcDstRange = new SrcDstRange();
        srcDstRange.getSrcRange().add(
                RangeOperations.toLineRange(
                        RangeOperations.toRange(action.getNode()), editScriptStorer.getSrcCompilationUnit()
                ));

        return srcDstRange;
    }

}
