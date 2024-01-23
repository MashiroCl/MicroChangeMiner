package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;

import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/21 12:29
 */
public class SemantiacllySameConditionUpdate implements MicroChangePattern{

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
//        System.out.println("insert node");
//        System.out.println(action.getName().equals("insert-node"));
//        System.out.println("parent");
//        System.out.println(action.getNode().getParent().getType().name);

        if(!action.getName().equals("insert-node")
                || !action.getNode().getParent().getType().name.equals("IfStatement"))
            return false;

//        System.out.println("here");
//        System.out.println("parent mappings");
        Tree ifStatementBefore = action.getNode().getParent();
        if(!mappings.containsKey(action.getNode().getParent())
                || !mappings.get(action.getNode().getParent()).getType().name.equals("IfStatement"))
            return false;

//        System.out.println(mappings.get(action.getNode().getParent()).getType().name);
//        System.out.println("here2");
//        System.out.println("children");
//        System.out.println(ifStatementBefore.getChildren());
        Tree ifStatementAfter = mappings.get(action.getNode().getParent());
        if(ifStatementBefore.getChildren().isEmpty()
                ||ifStatementBefore.getChildren().size()!=ifStatementAfter.getChildren().size())
            return false;

//        System.out.println("here3");
//        System.out.println(ifStatementAfter.getChildren());
        for(int i=1;i<ifStatementBefore.getChildren().size();i++){
            if(!mappings.containsKey(ifStatementBefore.getChild(i))
            || !mappings.get(ifStatementBefore.getChild(i)).equals(ifStatementAfter.getChild(i)))
                return false;
        }
        return true;
    }
}
