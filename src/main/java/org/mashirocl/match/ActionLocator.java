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
import org.mashirocl.microchange.SeperatedPosition;
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
     * get locations for src and dst seperately
     * @param action
     * @param editScriptStorer
     * @return
     *
     */
    public SeperatedPosition getSeperatedLocations(Action action, Map<Tree, Tree> mappings, EditScriptStorer editScriptStorer){
        List<Position> srcPositionList = new LinkedList<>();
        List<Position> dstPositionList = new LinkedList<>();
        CompilationUnit srcCU = editScriptStorer.getSrcCompilationUnit();
        CompilationUnit dstCU = editScriptStorer.getDstCompilationUnit();
        switch (action.getName()){
            case "insert-tree":
                dstPositionList.add(
                        new Position(dstCU.getLineNumber(action.getNode().getPos()),
                                dstCU.getLineNumber(action.getNode().getEndPos()))
                );
                break;
            case "insert-node":
//                System.out.println("insert node action");
//                System.out.println("action");
//                System.out.println(action);
//                System.out.println("action node");
//                System.out.println(action.getNode().toTreeString());
                if(action.getNode().getType().toString().equals("IfStatement")){
                    dstPositionList.add(
                            new Position(dstCU.getLineNumber(action.getNode().getChild(0).getPos()),
                                    dstCU.getLineNumber(action.getNode().getChild(0).getEndPos()))
                    );
//                    System.out.println("insert node"+dstPositionList.toString());
                }
                else{
                    dstPositionList.add(new Position(dstCU.getLineNumber(action.getNode().getPos()),
                            dstCU.getLineNumber(action.getNode().getEndPos())));
                }
                break;
            case "delete-tree":
                srcPositionList.add(new Position(srcCU.getLineNumber(action.getNode().getPos()),
                        srcCU.getLineNumber(action.getNode().getEndPos())));
                break;
            case "delete-node":
                if(action.getNode().getType().toString().equals("IfStatement")){
                    srcPositionList.add(
                            new Position(srcCU.getLineNumber(action.getNode().getChild(0).getPos()),
                                    srcCU.getLineNumber(action.getNode().getChild(0).getEndPos()))
                    );
                }
                else{
                    srcPositionList.add(new Position(srcCU.getLineNumber(action.getNode().getPos()),
                            srcCU.getLineNumber(action.getNode().getEndPos())));
                }
                break;
            case "update-node":
                srcPositionList.add(new Position(srcCU.getLineNumber(action.getNode().getPos()),
                        srcCU.getLineNumber(action.getNode().getEndPos())));
                dstPositionList.add(new Position(dstCU.getLineNumber(mappings.get(action.getNode()).getPos()),
                        dstCU.getLineNumber(mappings.get(action.getNode()).getEndPos())));
                break;
            case "move-tree":
                srcPositionList.add(new Position(srcCU.getLineNumber(action.getNode().getPos()),
                        srcCU.getLineNumber(action.getNode().getEndPos())));
                dstPositionList.add(new Position(dstCU.getLineNumber(mappings.get(action.getNode()).getPos()),
                        dstCU.getLineNumber(mappings.get(action.getNode()).getEndPos())));
        }

        return new SeperatedPosition(srcPositionList, dstPositionList);
    }

    public List<Position> getLocation(Action action, EditScriptStorer editScriptStorer){
        List<Position> positionList = new LinkedList<>();
        CompilationUnit srcCU = editScriptStorer.getSrcCompilationUnit();
        CompilationUnit dstCU = editScriptStorer.getDstCompilationUnit();
        switch (action.getName()){
            case "insert-tree", "insert-node":
//                srcPositionList.add(new Position(dstCU.getLineNumber(action.getNode().getPos()),
//                        dstCU.getLineNumber(action.getNode().getEndPos())));
                positionList.add(
                        new Position(dstCU.getLineNumber(action.getNode().getPos()),
                                dstCU.getLineNumber(action.getNode().getEndPos()))
                );
//                System.out.println("it is insert-tree or insert-node");
//                System.out.printf("start position: %d\n", dstCU.getLineNumber(action.getNode().getPos()));
//                System.out.printf("end position: %d\n", dstCU.getLineNumber(action.getNode().getEndPos()));
//                System.out.println(action.getNode().toTreeString());
                break;
            case "delete-tree","delete-node":
                positionList.add(new Position(srcCU.getLineNumber(action.getNode().getPos()),
                        srcCU.getLineNumber(action.getNode().getEndPos())));
//                System.out.println("it is delete-tree or delete-node");
//                System.out.printf("start position: %d\n", srcCU.getLineNumber(action.getNode().getPos()));
//                System.out.printf("end position: %d\n", srcCU.getLineNumber(action.getNode().getEndPos()));
//                System.out.println(action.getNode().toTreeString());
                break;
            case "update-node":
//                System.out.println("it is update-node");
//                System.out.printf("start position: %d\n", srcCU.getLineNumber(action.getNode().getPos()));
//                System.out.printf("end position: %d\n", srcCU.getLineNumber(action.getNode().getEndPos()));
//                System.out.println(action.getNode().toTreeString());
                positionList.add(new Position(srcCU.getLineNumber(action.getNode().getPos()),
                        srcCU.getLineNumber(action.getNode().getEndPos())));
                break;
            case "move-tree":
//                System.out.println("it is move-tree");
//                System.out.printf("start position: %d\n", srcCU.getLineNumber(action.getNode().getPos()));
//                System.out.printf("end position: %d\n", srcCU.getLineNumber(action.getNode().getEndPos()));
//                System.out.println(action.getNode().toTreeString());
                positionList.add(new Position(srcCU.getLineNumber(action.getNode().getPos()),
                        srcCU.getLineNumber(action.getNode().getEndPos())));
        }
        return positionList;
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

    public int coveredLines(List<Position> positions){
        List<Position> positionList = scopeCalculate(positions);
        return positionList.stream().map(p->p.getEndPosition()-p.getStartPosition()+1).reduce(0, Integer::sum);
    }

    public int[] coveredLines(SeperatedPosition seperatedPosition){

        List<Position> srcPositionList = scopeCalculate(seperatedPosition.getSrcPositions());
        List<Position> dstPositionList = scopeCalculate(seperatedPosition.getDstPositions());
        return new int [] {srcPositionList.stream().map(p->p.getEndPosition()-p.getStartPosition()+1).reduce(0, Integer::sum),
                dstPositionList.stream().map(p->p.getEndPosition()-p.getStartPosition()+1).reduce(0, Integer::sum)};
    }

}
