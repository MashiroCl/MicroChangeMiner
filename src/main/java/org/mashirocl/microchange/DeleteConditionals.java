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
     *  the parent after move is `Block`
     * @param action
     * @param mappings
     * @return
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
//        System.out.println("**************");
//        if(action.getName().equals("move-tree")
//                && action.getNode().getParent().getParent().getType().name.equals("IfStatement")
//                && mappings.containsKey(action.getNode())){
//
//            System.out.println(mappings.get(action.getNode()));
//            System.out.println(mappings.get(action.getNode()).getParent());
//            System.out.println(mappings.get(action.getNode()).getParent().getParent().toTreeString());
//        }
//        System.out.println("-----------------------");

        return action.getName().equals("move-tree")
                && action.getNode().getParent().getParent().getType().name.equals("IfStatement")
                && mappings.containsKey(action.getNode())
                && !mappings.get(action.getNode()).getParent().getType().name.equals("IfStatement")
                && mappings.get(action.getNode()).getParent().getParent().getType().name.equals("Block");
    }
}
