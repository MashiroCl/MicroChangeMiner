package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;

import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/21 14:50
 */
public class ConditionalToBooleanReturn implements MicroChangePattern{

    /**
     * condition
     * 1. action node is `move-tree`
     * 2. node is the 1st child of its parent, and its parent is the `IfStatement`
     * 3. target is `ReturnStatement `
     * @param action
     * @param mappings
     * @return
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
//        System.out.println(action.getNode().getType());
//        System.out.println(action.getNode().getParent().getType());
//        System.out.println(action);
        return action.getName().equals("move-tree")
                && action.getNode().getParent().getType().name.equals("IfStatement")
                && action.getNode().getParent().getChild(0).equals(action.getNode())
                && mappings.containsKey(action.getNode())
                && mappings.get(action.getNode()).getParent().getType().name.equals("ReturnStatement");

//        System.out.println(action);
//        System.out.println("IfStatementChild");
//        System.out.println(action.getNode().getParent().getChildren());
//        System.out.println("first child");
//        System.out.println(action.getNode().getParent().getChild(0));
//        System.out.println("IfStatementChild mapping");
//        System.out.println(mappings.get(action.getNode().getParent().getChild(0)));
    }
}
