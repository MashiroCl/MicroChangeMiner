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
        return result;
    }

    public SrcDstRange getRanges(Action action, Map<Tree, Tree> mappings, EditScriptStorer editScriptStorer){
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

    /**
     * calculate the scopes using start line numbers and end line numbers
     * Calculated under the case that the start position and end position are in the same file, code changes across multiple
     * files are not considered
     * @param positions
     * @return
     */
    public List<Position> scopeCalculate(List<Position> positions){
        List<Position> scopes = new LinkedList<>();
        if(positions.isEmpty()){
            log.error("empty position");
            return scopes;
        }
        positions.sort((Position p1, Position p2)-> p1.getStartPosition()- p2.getStartPosition());
        scopes.add(positions.get(0));
        for(int i=1;i<positions.size();i++){
            Position curPosition = positions.get(i);
            Position lastScope = scopes.get(scopes.size()-1);

            // no coverage between the current position with the last scope
            if(lastScope.getEndPosition()<curPosition.getStartPosition()){
                scopes.add(curPosition);
            }else { // covered
                lastScope.setEndPosition(Math.max(curPosition.getEndPosition(), lastScope.getEndPosition()));
            }
        }
        return scopes;
    }
}
