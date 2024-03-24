package org.mashirocl.match;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.tree.Tree;
import lombok.extern.slf4j.Slf4j;
import org.mashirocl.microchange.SrcDstRange;
import org.mashirocl.refactoringminer.RenameRefactoring;

import java.util.Map;


/**
 * @author mashirocl@gmail.com
 * @since 2024/02/22 16:20
 */
@Slf4j
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


    /**
     * use line index ranges to determine the line range in one lineRangeSet is the subset of line range in another lineRangeSet
     * @param
     * @return
     */
    public static boolean isSubsetOfAnotherRange(SrcDstRange smallerRange, SrcDstRange largerRange){
        boolean isSubsetSrc = false;
        boolean isSubsetDst = false;
        if(!smallerRange.getSrcRange().isEmpty() && !largerRange.getSrcRange().isEmpty()){
            isSubsetSrc = smallerRange.getSrcRange().asRanges().stream().allMatch(
                    range -> largerRange.getSrcRange().asRanges().stream().anyMatch(ifRange -> ifRange.encloses(range)));
        }
        if(!smallerRange.getDstRange().isEmpty() && !largerRange.getDstRange().isEmpty()){
            isSubsetDst = smallerRange.getDstRange().asRanges().stream().allMatch(
                    range -> largerRange.getDstRange().asRanges().stream().anyMatch(ifRange -> ifRange.encloses(range)));
        }



        return isSubsetSrc || isSubsetDst;
    }


    public static boolean hasIntersection(SrcDstRange a, SrcDstRange b){
        boolean hasIntersectionSrc = false;
        boolean hasIntersectionDst = false;
        if(!a.getSrcRange().isEmpty() && !b.getSrcRange().isEmpty()){
            hasIntersectionSrc = a.getSrcRange().asRanges().stream().anyMatch(range -> !b.getSrcRange().subRangeSet(range).isEmpty());
        }
        if(!a.getDstRange().isEmpty() && !b.getDstRange().isEmpty()){
            hasIntersectionDst = a.getDstRange().asRanges().stream().anyMatch(range -> !b.getDstRange().subRangeSet(range).isEmpty());
        }
        return hasIntersectionSrc || hasIntersectionDst;

    }


    public static SrcDstRange getIntersection(SrcDstRange a, SrcDstRange b){
        SrcDstRange res = new SrcDstRange();
        a.getSrcRange().removeAll(b.getSrcRange().complement());
        a.getDstRange().removeAll(b.getDstRange().complement());
        res.setSrcRange(a.getSrcRange());
        res.setDstRange(a.getDstRange());
        return res;
    }

    public static RenameRefactoring getRenamingRefactoring(Action a, Map<String, RenameRefactoring> renamingMap) {
        if (a.getName().equals("update-node")) {
            String updateSignature = a.getNode().getLabel() + "@" + ((Update) a).getValue();
            return renamingMap.containsKey(updateSignature) ? renamingMap.get(updateSignature) : null;
        }
        return null;
    }
}
