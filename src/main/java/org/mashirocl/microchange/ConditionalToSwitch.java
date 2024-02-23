package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import com.google.common.collect.Range;
import org.mashirocl.editscript.EditScriptStorer;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/21 19:05
 */
public class ConditionalToSwitch implements MicroChangePattern{
    /**
     * condition
     * 1. action node is `move-tree`
     * 2. parent of the action node is  `xxExpression`, and parent of its parent is `IfStatement`
     * 3.move target is `SwitchStatement`
     * @param action
     * @param mappings
     * @return
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
//        System.out.println("---------------------");
//        System.out.println(action);
//        System.out.println(action.getName().equals("move-tree"));
//        System.out.println(action.getNode().getParent().getParent().getType().name);
//        System.out.println(action.getNode().equals(action.getNode().getParent().getChild(0)));
//        if(mappings.containsKey(action.getNode()))
//            System.out.println(mappings.get(action.getNode()).getParent().getType().name);
//        System.out.println("**********************");
        return action.getName().equals("move-tree")
                && action.getNode().getParent().getParent().getType().name.equals("IfStatement")
                && action.getNode().equals(action.getNode().getParent().getChild(0))
                && mappings.containsKey(action.getNode())
                && mappings.get(action.getNode()).getParent().getType().name.equals("SwitchStatement");
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        return matchConditionGumTree(action, mappings);
    }

    @Override
    public SrcDstRange getSrcDstRange(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        SrcDstRange srcDstRange = new SrcDstRange();
        // left side
        // being moved expression
        Range<Integer> movedExpression = Range.closed(
                editScriptStorer.getSrcCompilationUnit().getLineNumber(action.getNode().getPos()),
                editScriptStorer.getSrcCompilationUnit().getLineNumber(action.getNode().getEndPos())
        );
        // the if-statement
        Range<Integer> ifStatement = Range.closed(
                editScriptStorer.getSrcCompilationUnit().getLineNumber(action.getNode().getParent().getPos()),
                editScriptStorer.getSrcCompilationUnit().getLineNumber(action.getNode().getParent().getEndPos())
        );

        //right side
        // switch case
        Range<Integer> switchCase = Range.closed(
                editScriptStorer.getDstCompilationUnit().getLineNumber(mappings.get(action.getNode()).getParent().getPos()),
                editScriptStorer.getDstCompilationUnit().getLineNumber(mappings.get(action.getNode()).getParent().getEndPos())
        );

        srcDstRange.getSrcRange().add(movedExpression);
        srcDstRange.getSrcRange().add(ifStatement);
        srcDstRange.getDstRange().add(switchCase);
        return srcDstRange;
    }

    public List<Position> getPosition(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        List<Position> positions = new LinkedList<>();

        // left side
        // being moved expression
        Position movedExpression = new Position(
                editScriptStorer.getSrcCompilationUnit().getLineNumber(action.getNode().getPos()),
                editScriptStorer.getSrcCompilationUnit().getLineNumber(action.getNode().getEndPos())
        );
        // the if-statement
        Position ifStatement = new Position(
                editScriptStorer.getSrcCompilationUnit().getLineNumber(action.getNode().getParent().getPos()),
                editScriptStorer.getSrcCompilationUnit().getLineNumber(action.getNode().getParent().getEndPos())
        );

        //right side
        // switch case
        Position switchCase = new Position(
                editScriptStorer.getDstCompilationUnit().getLineNumber(mappings.get(action.getNode()).getParent().getPos()),
                editScriptStorer.getDstCompilationUnit().getLineNumber(mappings.get(action.getNode()).getParent().getEndPos())
        );

        System.out.printf("positions for the switchcase %d %d\n", switchCase.getStartPosition(), switchCase.getEndPosition());
        positions.add(movedExpression);
        positions.add(ifStatement);
        positions.add(switchCase);
        return positions;
    }
}
