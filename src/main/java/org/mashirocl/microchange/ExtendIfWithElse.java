package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;

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
     * 3. the # children nodes of a `IfStatement` increased
     * @param action
     * @param mappings
     * @return
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
//        System.out.println(action);
//        System.out.println("parent");
//        System.out.println(action.getNode().getParent());
//        System.out.println("mappings.containsKey(action.getNode().getParent())");
//        System.out.println(mappings.containsKey(action.getNode().getParent()));
//        System.out.println("after size");
//        System.out.println(action.getNode().getParent().getChildren().size());
//        System.out.println("before size");
//        System.out.println(mappings.get(action.getNode().getParent()).getChildren().size());

        return (action.getName().equals("insert-tree")
        && action.getNode().getParent().getType().name.equals("IfStatement")
        && mappings.containsKey(action.getNode().getParent())
                // note that the action.getNode() is the node after change
        && action.getNode().getParent().getChildren().size()>mappings.get(action.getNode().getParent()).getChildren().size());
    }
}
