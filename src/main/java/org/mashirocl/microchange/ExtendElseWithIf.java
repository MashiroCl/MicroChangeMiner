package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;

import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/20 18:25
 */
public class ExtendElseWithIf implements MicroChangePattern{

    /**
     * condition
     * 1. action node is `insert-node`
     * 2. node type is `IfStatement`
     * 3. its parent is `IfStatement`
     * @param action
     * @param mappings
     * @return
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
        return (action.getName().equals("insert-node")
                && action.getNode().getType().name.equals("IfStatement")
                && action.getNode().getParent().getType().name.equals("IfStatement")
                && mappings.containsKey(action.getNode().getParent()));
    }
}
