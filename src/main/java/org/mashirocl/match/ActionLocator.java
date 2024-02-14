package org.mashirocl.match;

import com.github.gumtreediff.actions.model.Action;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.mashirocl.editscript.EditScriptStorer;
import org.mashirocl.microchange.Position;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author mashirocl@gmail.com
 * @since 2024/02/11 20:07
 */
@Slf4j
public class ActionLocator {
    public List<Position> getLocation(Action action, EditScriptStorer editScriptStorer){
        List<Position> positionList = new LinkedList<>();
        CompilationUnit srcCU = editScriptStorer.getSrcCompilationUnit();
        CompilationUnit dstCU = editScriptStorer.getDstCompilationUnit();
        switch (action.getName()){
            case "insert-tree", "insert-node":
                positionList.add(new Position(dstCU.getLineNumber(action.getNode().getPos()),
                        dstCU.getLineNumber(action.getNode().getEndPos())));
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

}
