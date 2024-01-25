package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;

import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/25 15:57
 */
public class DeleteConditionals implements MicroChangePattern{
    /**
     * is `move-tree`
     * action.getnode() parent is `IfStatement`
     *  the parent after move is not `IfStatement`
     * @param action
     * @param mappings
     * @return
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
        return action.getName().equals("move-tree")
                && action.getNode().getParent().getParent().getType().name.equals("IfStatement")
                && mappings.containsKey(action.getNode())
                && !mappings.get(action.getNode()).getParent().getParent().getType().name.equals("IfStatement");
    }
}
