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
 * @since 2024/01/25 14:16
 */
public class UnifyCondition implements MicroChangePattern{
    /**
     * is `move-tree`
     * it is moved to a `InfixExpression` or `PrefixExpression`
     * the parent of the moved target is a `IfStatement`
     * the conditions in the InfixExpression should exist before the unify
     * @param action
     * @param mappings
     * @return
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
        if(action.getName().equals("move-tree")
                && action.getNode().getParent().getType().name.equals("IfStatement")
                && mappings.containsKey(action.getNode())){

//        if(mappings.containsKey(action.getNode())) {
//            System.out.println(action.getNode());
//            System.out.println(mappings.get(action.getNode()).getParent().getType().name);
//            System.out.println(mappings.get(action.getNode()).getParent().getChild(0));
//            System.out.println(mappings.get(action.getNode()).getParent().getChild(1));
//            System.out.println(mappings.get(action.getNode()).getParent().getChild(2));
//            System.out.println(mappings.containsKey(mappings.get(action.getNode()).getParent().getChild(0)));
//            System.out.println(mappings.containsKey(mappings.get(action.getNode()).getParent().getChild(1)));
//            System.out.println(mappings.containsKey(mappings.get(action.getNode()).getParent().getChild(2)));
//        }

            // two conditions concated with && or ||
            if((mappings.get(action.getNode()).getParent().getType().name.equals("InfixExpression"))){
                return mappings.containsKey(mappings.get(action.getNode()).getParent().getChild(0))
                        && mappings.containsKey(mappings.get(action.getNode()).getParent().getChild(2))
                        && mappings.get(action.getNode()).getParent().getParent().getType().name.equals("IfStatement");
            }

            // exist prefix condition?
//            if((mappings.get(action.getNode()).getParent().getType().name.equals("PrefixExpression"))){
//                return mappings.containsKey(mappings.get(action.getNode()).getParent().getChild(0))
//                        && mappings.containsKey(mappings.get(action.getNode()).getParent().getChild(2))
//                        && mappings.get(action.getNode()).getParent().getParent().getType().name.equals("IfStatement");
//            }
        }


        return false;
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        return matchConditionGumTree(action,mappings);
    }


    @Override
    public SrcDstRange getSrcDstRange(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        SrcDstRange srcDstRange = new SrcDstRange();
        srcDstRange.getSrcRange().add(Range.closed(
                        editScriptStorer.getSrcCompilationUnit().getLineNumber(action.getNode().getPos()),
                        editScriptStorer.getSrcCompilationUnit().getLineNumber(action.getNode().getEndPos())
                )
        );
        srcDstRange.getDstRange().add(Range.closed(
                editScriptStorer.getDstCompilationUnit().getLineNumber(mappings.get(action.getNode()).getPos()),
                editScriptStorer.getDstCompilationUnit().getLineNumber(mappings.get(action.getNode()).getEndPos()))
        );

        return srcDstRange;
    }

    public List<Position> getPosition(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        List<Position> positions = new LinkedList<>();
        // left side
        // the not changed part condition/ the being moved condition
        Position notChangedContidion = new Position(
                editScriptStorer.getSrcCompilationUnit().getLineNumber(action.getNode().getPos()),
                editScriptStorer.getSrcCompilationUnit().getLineNumber(action.getNode().getEndPos())
        );
        positions.add(notChangedContidion);
        // the being unified if-block
        if(nodeActions.containsKey(action.getNode().getParent())){
            for(Action a:nodeActions.get(action.getNode().getParent())){
                if(a.getName().equals("delete-node") || a.getName().equals("delete-tree")){
                    positions.add(new Position(
                            editScriptStorer.getSrcCompilationUnit().getLineNumber(a.getNode().getPos()),
                            editScriptStorer.getSrcCompilationUnit().getLineNumber(a.getNode().getEndPos())
                    ));
                }
            }
        }
        return positions;
    }
}
