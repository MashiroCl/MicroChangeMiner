package org.mashirocl.match;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;

/**
 * @author mashirocl@gmail.com
 * @since 2024/02/22 16:20
 */
public class ActionStatus {

    /**
     * action node is descendant of an IfStatement or itself is ifstatement
     * @param a
     * @return
     */
    public static boolean isDescendantOfIfStatement(Action a){
        Tree curNode = a.getNode();
        //TODO: using hashmap may accelerate the speed, depends on the density of actions in a single file
        while(curNode!=null && !curNode.isRoot()){
            if(isIfStatement(curNode)) return true;
            curNode = curNode.getParent();
        }
        return false;
    }

    /**
     * action node itself is IfStatement
     * @param
     * @return
     */
    public static boolean isIfStatement(Tree node){
        return node.getType().name.equals("IfStatement");
    }

    /**
     * action node is child of an IfStatement or itself is ifstatement
     * @param a
     * @return
     */
    public static  boolean isChildOfIfStatement(Action a){
        return isIfStatement(a.getNode()) || isIfStatement(a.getNode().getParent());
    }
}
