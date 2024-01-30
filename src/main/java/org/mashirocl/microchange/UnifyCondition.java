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
     * the conditions in the InfixExpression should exist before the unify
     * @param action
     * @param mappings
     * @return
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
        if(action.getName().equals("move-tree")
                && action.getNode().getParent().getType().name.equals("IfStatement")
                && mappings.containsKey(action.getNode())){

//        if(mappings.containsKey(action.getNode())) {
//            System.out.println(action.getNode());
//            System.out.println(mappings.get(action.getNode()).getParent().getType().name);
//            System.out.println(mappings.get(action.getNode()).getParent().getChild(0));
//            System.out.println(mappings.get(action.getNode()).getParent().getChild(1));
//            System.out.println(mappings.get(action.getNode()).getParent().getChild(2));
//            System.out.println(mappings.containsKey(mappings.get(action.getNode()).getParent().getChild(0)));
//            System.out.println(mappings.containsKey(mappings.get(action.getNode()).getParent().getChild(1)));
//            System.out.println(mappings.containsKey(mappings.get(action.getNode()).getParent().getChild(2)));
//        }

            // two conditions concated with && or ||
            if((mappings.get(action.getNode()).getParent().getType().name.equals("InfixExpression"))){
                return mappings.containsKey(mappings.get(action.getNode()).getParent().getChild(0))
                        && mappings.containsKey(mappings.get(action.getNode()).getParent().getChild(2))
                        && mappings.get(action.getNode()).getParent().getParent().getType().name.equals("IfStatement");
            }

            // exist prefix condition?
//            if((mappings.get(action.getNode()).getParent().getType().name.equals("PrefixExpression"))){
//                return mappings.containsKey(mappings.get(action.getNode()).getParent().getChild(0))
//                        && mappings.containsKey(mappings.get(action.getNode()).getParent().getChild(2))
//                        && mappings.get(action.getNode()).getParent().getParent().getType().name.equals("IfStatement");
//            }
        }


        return false;
    }
}
