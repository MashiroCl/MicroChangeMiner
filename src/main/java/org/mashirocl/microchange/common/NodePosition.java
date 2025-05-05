package org.mashirocl.microchange.common;

import com.github.gumtreediff.tree.Tree;

import java.util.*;

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
    public static Tree isIfConditionExpression(Tree node) {
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
     * @param node
     * @return the farthest parent of the node, which is the direct child of the ForStatement (should be one of ForInitialization, ForCondition or ForIncrement)
     */
    public static Tree isDescendantOfForLoopHeader(Tree node){
        Tree p  = node;
        while(p!=null && !p.isRoot() && !p.getType().name.equals("Block") && !p.getParent().getType().name.equals("ForStatement") && !p.getParent().getType().name.equals("EnhancedForStatement")){
            p = p.getParent();
        }
        if(p==null || p.isRoot() || p.getType().name.equals("Block")) return null;
        return p;
    }

    /**
     * Is the descendant of a while-statement's loop header (e.g. while(i<n), is the descendant of either `i<n`).
     * @param node
     * @return the farthest parent of the node, which is the direct child of the WhileStatement (should be Condition of while)
     */
    public static Tree isDescendantOfWhileLoopHeader(Tree node){
        Tree p  = node;
        while(p!=null && !p.isRoot() && !p.getType().name.equals("Block") && !p.getParent().getType().name.equals("WhileStatement")){
            p = p.getParent();
        }
        if(p==null || p.isRoot() || p.getType().name.equals("Block")) return null;
        return p;
    }


    /**
     * Is the for loop initialization expression or its descendant (e.g. for(int i=0;i<n;i++) int i=0 is the initialization)
     * Through locating the condition-indexes first, the indexes of children of a for loop header smaller than the condition-indexes are the loop initialization.
     * @param node
     * @return
     */
    public static boolean isForLoopInitialization(Tree node){
        Tree forChild = isDescendantOfForLoopHeader(node);
        if(forChild==null) return false;
        List<Integer> conditionIndexes = locateForLoopCondition(forChild.getParent());
        return !conditionIndexes.isEmpty() && forChild.getParent().getChildren().indexOf(forChild)< Collections.min(conditionIndexes);
}

    /**
     * Is the for loop initialization expression or its descendant (e.g. for(int i=0;i<n;i++) int i=0 is the initialization)
     * Through locating the condition-indexes first, the indexes of children of a for loop header smaller than the condition-indexes are the loop initialization.
     * @param node
     * @return
     */
    public static boolean isForLoopCondition(Tree node){
        Tree forChild = isDescendantOfForLoopHeader(node);
        if(forChild==null) return false;
        List<Integer> conditionIndexes = locateForLoopCondition(forChild.getParent());
    return !conditionIndexes.isEmpty() && forChild.getParent().getChildren().indexOf(forChild)>=Collections.min(conditionIndexes)
            && forChild.getParent().getChildren().indexOf(forChild)<=Collections.max(conditionIndexes);
    }



    //TODO: what if the locating failed?
    /**
     * Locate the condition node index in a for loop header (e.g. for(int i=0,int i=4;i<3,i<4;i++,j++), return [2,3] )
     *
     * @param forStatement ForStatement node
     * @return
     */
    public static List<Integer> locateForLoopCondition(Tree forStatement){
        List<Integer> indexes = new ArrayList<>();
        if(!forStatement.getType().name.equals("ForStatement")) return indexes;
        List<Tree> children = forStatement.getChildren();
        for(int i=0;i<children.size()-1;i++){
            Tree child = children.get(i);
               if(child!=null
                       && !child.getType().name.equals("Block")
                       && child.getType().name.contains("InfixExpression")
                       || child.getType().name.contains("MethodInvocation")){
                   indexes.add(i);
               }
        }
        return indexes;
    }

    /**
     * Return indexes of Initialization, Condition and Increment.
     * @param node
     * @return
     */
    public static Map<String, int[]> decomposeForLoopHeader(Tree node){
        Map<String, int[]> indexesMap = new HashMap<>();
        Tree forStatement = node;
        if(!node.getType().name.equals("ForStatement")){
            Tree forChild = isDescendantOfForLoopHeader(node);
            if(forChild==null) return indexesMap;
            forStatement = forChild.getParent();
            // Is not a forstatement node
            if(!forStatement.getType().name.equals("ForStatement")) return indexesMap;
        }

        List<Integer> conditionIndexes = locateForLoopCondition(forStatement);
        int [] condition  = new int[]{Collections.min(conditionIndexes), Collections.max(conditionIndexes)};
        indexesMap.put("Condition", condition);
        if(condition[0]>0){
            indexesMap.put("Initialization", new int[]{0, condition[0]-1});
        }
        if(condition[1]<forStatement.getChildren().size()-1){
            indexesMap.put("Increment", new int[]{condition[1]+1, forStatement.getChildren().size()-1});
        }
        return indexesMap;
    }

    /**
     * Is the for loop increment or its descendant (e.g. for(int i=0;i<n;i++) i++ is the increment)
     * Through locating the condition-indexes first, the indexes of children of a for loop header larger than the
     * condition-indexes AND smaller than the BLOCK index (for loop header children size) are the loop increment.
     * @param node
     * @return
     */
    public static boolean isForLoopIncrement(Tree node){
        Tree forChild = isDescendantOfForLoopHeader(node);
        if(forChild==null) return false;
        List<Integer> conditionIndexes = locateForLoopCondition(forChild.getParent());
        int index = forChild.getParent().getChildren().indexOf(forChild);
        return index > Collections.min(conditionIndexes) && index < forChild.getParent().getChildren().size()-1;
    }

}
