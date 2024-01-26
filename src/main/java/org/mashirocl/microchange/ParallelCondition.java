package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;

import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/26 9:26
 */
public class ParallelCondition implements MicroChangePattern{
    /**
     * 1. is `move-tree`
     * 2. before move, it is in an IfStatement (action.getnode.parent is InfixExpreesion)
     * 3. before move it is the first child (condition) of the IfStatement
     * 4. after move, it is in an IfStatement
     * 5. after move, the IfStatement it is in is a child of another IfStatement (after move parent
     * is InfixExpreesion, parent parent is block, parent parent parent is thd another IfStatement)
     * 6. the IfStatement it is in before move should map to the after move parent IfStatement
     * @param action
     * @param mappings
     * @return
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
        return action.getName().equals("move-tree")
                && action.getNode().getParent().getParent().getType().name.equals("IfStatement")
                && action.getNode().getParent().getChild(0).equals(action.getNode())
                && mappings.containsKey(action.getNode())
                && mappings.get(action.getNode()).getParent().getType().name.equals("IfStatement")
                && mappings.get(action.getNode()).getParent().getParent().getParent().getType().name.equals("IfStatement")
                && mappings.containsKey(action.getNode().getParent().getParent())
                && mappings.get(action.getNode().getParent().getParent()).equals((mappings.get(action.getNode()).getParent().getParent().getParent()));
    }
}
