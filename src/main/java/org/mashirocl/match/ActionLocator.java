package org.mashirocl.match;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.mashirocl.editscript.EditScriptStorer;
import org.mashirocl.location.RangeOperations;
import org.mashirocl.microchange.Position;
import org.mashirocl.microchange.SrcDstRange;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/02/11 20:07
 */
@Slf4j
public class ActionLocator {


    /**
     * obtaining a range for a node with excluding its children range
     * @return
     */
    private RangeSet<Integer> toRangeOfRoot(Tree node) {
        RangeSet<Integer> result = TreeRangeSet.create();
        result.add(RangeOperations.toRange(node));
        node.getChildren().forEach(c -> result.remove(RangeOperations.toRange(c)));
        // prevent the excluding result is null
        if(result.isEmpty()){
            result.add(RangeOperations.firstPositiontoRange(node));
        }
        return result;
    }

    public SrcDstRange getLineRanges(Action action, Map<Tree, Tree> mappings, EditScriptStorer editScriptStorer){
        RangeSet<Integer> srcRangeSet = TreeRangeSet.create();
        RangeSet<Integer> dstRangeSet = TreeRangeSet.create();
        CompilationUnit srcCU = editScriptStorer.getSrcCompilationUnit();
        CompilationUnit dstCU = editScriptStorer.getDstCompilationUnit();
        switch (action.getName()){
            case "insert-tree":
                dstRangeSet.add(RangeOperations.toLineRange(RangeOperations.toRange(action.getNode()), dstCU));
                break;
            case "insert-node":
                dstRangeSet.addAll(RangeOperations.toLineRange(toRangeOfRoot(action.getNode()), dstCU));
                break;
            case "delete-tree":
                srcRangeSet.add(RangeOperations.toLineRange(RangeOperations.toRange(action.getNode()), srcCU));
                break;
            case "delete-node":
                srcRangeSet.addAll(RangeOperations.toLineRange(toRangeOfRoot(action.getNode()), srcCU));
                break;
            case "update-node":
                srcRangeSet.addAll(RangeOperations.toLineRange(toRangeOfRoot(action.getNode()), srcCU));
                dstRangeSet.addAll(RangeOperations.toLineRange(toRangeOfRoot(mappings.get(action.getNode())), dstCU));
                break;
            case "move-tree":
                srcRangeSet.add(RangeOperations.toLineRange(RangeOperations.toRange(action.getNode()), srcCU));
                dstRangeSet.add(RangeOperations.toLineRange(RangeOperations.toRange(mappings.get(action.getNode())), dstCU));
                break;
        }
        return new SrcDstRange(srcRangeSet,dstRangeSet);
    }

}
