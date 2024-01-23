package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;

import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/21 19:05
 */
public class ConditionalToSwitch implements MicroChangePattern{
    /**
     * condition
     * 1. action node is `insert-node`
     * 2. target is `IfStatement`
     * 3. except the first child (condition), children of `IfStatement` are the same before and after change
     * @param action
     * @param mappings
     * @return
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
//        System.out.println("---------------------");
//        System.out.println(action);
//        System.out.println(action.getName().equals("move-tree"));
//        System.out.println(action.getNode().getParent().getParent().getType().name);
//        System.out.println(action.getNode().equals(action.getNode().getParent().getChild(0)));
//        if(mappings.containsKey(action.getNode()))
//            System.out.println(mappings.get(action.getNode()).getParent().getType().name);
//        System.out.println("**********************");
        return action.getName().equals("move-tree")
                && action.getNode().getParent().getParent().getType().name.equals("IfStatement")
                && action.getNode().equals(action.getNode().getParent().getChild(0))
                && mappings.containsKey(action.getNode())
                && mappings.get(action.getNode()).getParent().getType().name.equals("SwitchStatement");
    }
}
