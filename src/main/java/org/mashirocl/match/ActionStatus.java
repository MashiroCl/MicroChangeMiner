package org.mashirocl.match;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import lombok.extern.slf4j.Slf4j;
import org.mashirocl.microchange.SrcDstRange;


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
    public static boolean isInsideIfStatement(SrcDstRange lineRanges, SrcDstRange srcDstRangeOfIf){
        boolean insideSrcIf = false;
        boolean insideDstIf = false;
        if(!lineRanges.getSrcRange().isEmpty() && !srcDstRangeOfIf.getSrcRange().isEmpty()){
            log.info("src action line ranges {}", lineRanges);
            log.info("src if line ranges {}", srcDstRangeOfIf);
            insideSrcIf = lineRanges.getSrcRange().asRanges().stream().allMatch(
                    range -> srcDstRangeOfIf.getSrcRange().asRanges().stream().anyMatch(ifRange -> ifRange.encloses(range)));
            log.info("insideSrcIf {}", insideSrcIf);
        }
        if(!lineRanges.getDstRange().isEmpty() && !srcDstRangeOfIf.getDstRange().isEmpty()){
            log.info("dst action line ranges {}", lineRanges);
            log.info("dst if line ranges {}", srcDstRangeOfIf);
            insideDstIf = lineRanges.getDstRange().asRanges().stream().allMatch(
                    range -> srcDstRangeOfIf.getDstRange().asRanges().stream().anyMatch(ifRange -> ifRange.encloses(range)));
            log.info("insideDstIf {}", insideDstIf);
        }

        log.info("insideSrcIf || insideDstIf: {}", insideSrcIf || insideDstIf);
        return insideSrcIf || insideDstIf;
    }
}
