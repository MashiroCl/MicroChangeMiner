package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.tree.Tree;

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
}
