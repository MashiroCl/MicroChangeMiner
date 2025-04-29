package org.mashirocl.microchange.common;

import com.github.gumtreediff.tree.Tree;

import java.util.HashSet;
import java.util.Set;

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


    public static boolean isDescedantOf(Tree child, Tree parent){
        return parent.getDescendants().contains(child);
    }
    /**
     * Is the descendant of a for-statement's loop header (e.g. for(int i=0;i<n;i++), is the descendant of either `int i=0` or `i<n` or `i++`).
     * Note: It is not the descendant of the for-body
     *
     * @param node
     * @return
     */
    public static boolean isDescendantOfForLoopHeader(Tree node){
        Tree p  = node;
        Set<Tree> record = new HashSet<>();
        while(p!=null && !p.isRoot() && !p.getType().name.equals("ForStatement")){
            record.add(p);
            p = p.getParent();
        }
        if(p!=null && p.getType().name.equals("ForStatement")){
            // ensure the for has for-body, which is the last child of for-node
            Tree forBody = p.getChild(p.getChildren().size()-1);
            if(!forBody.getType().name.equals("Block")){  // for-loop does not have a for-body. Exists?
                return false;
            }
            return !record.contains(forBody);
            }
        // Block
        return false;
    }
}
