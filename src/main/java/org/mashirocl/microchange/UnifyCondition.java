package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import com.google.common.collect.Range;
import org.mashirocl.editscript.EditScriptStorer;
import org.mashirocl.location.RangeOperations;

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

            // two conditions concacted with && or ||
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
        srcDstRange.getSrcRange().add(
                RangeOperations.toLineRange(
                        RangeOperations.toRange(action.getNode()), editScriptStorer.getSrcCompilationUnit()
                ));
        srcDstRange.getDstRange().add(
                RangeOperations.toLineRange(
                        RangeOperations.toRange(mappings.get(action.getNode())), editScriptStorer.getDstCompilationUnit())
        );

        return srcDstRange;
    }

}
