package org.mashirocl.microchange.common;

import com.github.gumtreediff.tree.Tree;

/**
 * @author mashirocl@gmail.com
 * @since 2024/03/01 14:48
 */
public class NodePosition {

    /**
     * if the node is in if condition expression, return the condition expression node
     * @param node
     * @return
     */
    public static Tree isConditionExpression(Tree node) {
        Tree curNode = node.getParent();
        Tree preNode = node;
        while (curNode != null && !curNode.isRoot()) {
            if (curNode.getType().name.equals("IfStatement") && preNode.equals(curNode.getChild(0))) { //it is ifstatement and the node is in the condition (the 1st child of if)
                    return preNode;
            }
            preNode = curNode;
            curNode = curNode.getParent();
        }
        return null;
    }

    /**
     * if the node is in if (in conditional expression/then/else), return the IfStatement node
     * @param node
     * @return
     */
    public static Tree isInIf(Tree node){
        while(node!=null && !node.isRoot()){
            if(node.getType().name.equals("IfStatement")) return node;
            node = node.getParent();
        }
        return null;
    }
}
