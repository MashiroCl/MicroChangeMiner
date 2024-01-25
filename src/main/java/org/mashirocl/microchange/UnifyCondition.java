package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;

import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/25 14:16
 */
public class UnifyCondition implements MicroChangePattern{
    /**
     * is `move-tree`
     * it is moved to a `InfixExpression` or `PrefixExpression`
     * the parent of the moved target is a `IfStatement`
     * @param action
     * @param mappings
     * @return
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
        return action.getName().equals("move-tree")
                && action.getNode().getParent().getType().name.equals("IfStatement")
                && mappings.containsKey(action.getNode())
                && (mappings.get(action.getNode()).getParent().getType().name.equals("InfixExpression")
                || mappings.get(action.getNode()).getParent().getType().name.equals("PrefixExpression"))
                && mappings.get(action.getNode()).getParent().getParent().getType().name.equals("IfStatement");
    }
}
