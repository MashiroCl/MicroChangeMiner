package org.mashirocl.match;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.mashirocl.editscript.EditScriptStorer;
import org.mashirocl.microchange.Position;
import org.mashirocl.microchange.SrcDstRange;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author mashirocl@gmail.com
 * @since 2024/02/11 20:07
 */
@Slf4j
public class ActionLocator {


    public SrcDstRange getRanges(Action action, Map<Tree, Tree> mappings, EditScriptStorer editScriptStorer){
        RangeSet<Integer> srcRangeSet = TreeRangeSet.create();
        RangeSet<Integer> dstRangeSet = TreeRangeSet.create();
        CompilationUnit srcCU = editScriptStorer.getSrcCompilationUnit();
        CompilationUnit dstCU = editScriptStorer.getDstCompilationUnit();
        switch (action.getName()){
            case "insert-tree":
//                System.out.println("locating insert tree");
                dstRangeSet.add(
                        Range.closed(
                                dstCU.getLineNumber(action.getNode().getPos()),
                                dstCU.getLineNumber(action.getNode().getEndPos())));
                break;
            case "insert-node":
//                System.out.println("locating insert node");
//                System.out.println(action);
                RangeSet<Integer> insertedRootNode = getRangeForRootNode(action);
                for(Range<Integer> range: insertedRootNode.asRanges()){
                    dstRangeSet.add(
                            Range.closed(
                                    dstCU.getLineNumber(range.lowerEndpoint()),
                                    dstCU.getLineNumber(range.upperEndpoint())
                            )
                    );
                }
//                System.out.println("insertedRootNode");
//                System.out.println(insertedRootNode);
                break;
            case "delete-tree":
//                System.out.println("locating delete tree");
                srcRangeSet.add(Range.closed(
                        srcCU.getLineNumber(action.getNode().getPos()),
                        srcCU.getLineNumber(action.getNode().getEndPos())
                ));
                break;
            case "delete-node":
//                System.out.println("locating delete node");
                RangeSet<Integer> deletedRootNode = getRangeForRootNode(action);
                for(Range<Integer> range: deletedRootNode.asRanges()){
                    srcRangeSet.add(
                            Range.closed(
                                    srcCU.getLineNumber(range.lowerEndpoint()),
                                    srcCU.getLineNumber(range.upperEndpoint())
                            )
                    );
                }
                break;
            case "update-node":
//                System.out.println("locating update-node");
                srcRangeSet.add(
                        Range.closed(
                                srcCU.getLineNumber(action.getNode().getPos()),
                                srcCU.getLineNumber(action.getNode().getEndPos())
                ));
                dstRangeSet.add(
                        Range.closed(
                                dstCU.getLineNumber(mappings.get(action.getNode()).getPos()),
                                dstCU.getLineNumber(mappings.get(action.getNode()).getEndPos())));
                break;
            case "move-tree":
//                System.out.println("locating move-tree");
//                System.out.println(action);
                srcRangeSet.add(Range.closed(
                        srcCU.getLineNumber(action.getNode().getPos()),
                        srcCU.getLineNumber(action.getNode().getEndPos())
                ));
                dstRangeSet.add(
                        Range.closed(
                                dstCU.getLineNumber(mappings.get(action.getNode()).getPos()),
                                dstCU.getLineNumber(mappings.get(action.getNode()).getEndPos())));
        }
        return new SrcDstRange(srcRangeSet,dstRangeSet);
    }

    /**
     * obtaining a range for a node with excluding its children range
     * @return
     */
    private RangeSet<Integer> getRangeForRootNode(Action action){
        RangeSet<Integer> rootNode = TreeRangeSet.create();
        rootNode.add(Range.closed(action.getNode().getPos(), action.getNode().getEndPos()));
        for(Tree child:action.getNode().getChildren()){
            rootNode.remove(Range.closed(child.getPos(),child.getEndPos()));
        }
        return rootNode;
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
