package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import lombok.extern.slf4j.Slf4j;
import org.mashirocl.editscript.EditScriptStorer;
import org.mashirocl.location.RangeOperations;

import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/03/15 12:28
 */
@Slf4j
public class UnwrapStatementFromBlock implements MicroChangePattern{
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
        if(action.getName().equals("move-tree")
                && action.getNode().getParent().toString().contains("Block")
                && action.getNode().getParent().getParent().getType().toString().equals("IfStatement")
                && action.getNode().getParent().getChildren().size()==1
                && mappings.containsKey(action.getNode())
                && mappings.get(action.getNode()).getParent().getType().toString().equals("IfStatement")
                && action.getNode().getParent().getParent().getChild(0).isIsomorphicTo(mappings.get(action.getNode()).getParent().getChild(0))
                && action.getNode().getParent().getChild(0).isIsomorphicTo(mappings.get(action.getNode()).getParent().getChild(1)))
            return true;

        if(action.getName().equals("move-tree")
                && action.getNode().toString().contains("Block")
                && action.getNode().getParent().getType().toString().equals("IfStatement")
                && action.getNode().getChildren().size()==1
                && mappings.containsKey(action.getNode().getParent())
                && mappings.get(action.getNode().getParent()).getType().toString().equals("IfStatement")
                && action.getNode().getParent().getChild(0).isIsomorphicTo(mappings.get(action.getNode().getParent()).getChild(0))
                && mappings.get(action.getNode().getParent()).getChild(1).isIsomorphicTo(action.getNode().getChild(0)))
            return true;

        return false;
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        return matchConditionGumTree(action,mappings);
    }

    @Override
    public SrcDstRange getSrcDstRange(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        SrcDstRange srcDstRange = new SrcDstRange();
        if(action.getName().equals("move-tree")
                && action.getNode().getParent().toString().contains("Block")
                && action.getNode().getParent().getParent().getType().toString().equals("IfStatement")
                && action.getNode().getParent().getChildren().size()==1
                && mappings.containsKey(action.getNode())
                && mappings.get(action.getNode()).getParent().getType().toString().equals("IfStatement")
                && action.getNode().getParent().getChild(0).isIsomorphicTo(mappings.get(action.getNode()).getParent().getChild(1)))
        {
            // if-block on the left side
            srcDstRange.getSrcRange().add(
                    RangeOperations.toLineRange(
                            RangeOperations.toRange(
                                    action.getNode().getParent().getParent()),editScriptStorer.getSrcCompilationUnit()
                    )
            );


            // if-block on the right side
            srcDstRange.getDstRange().add(
                    RangeOperations.toLineRange(
                            RangeOperations.toRange(
                                    mappings.get(action.getNode()).getParent()), editScriptStorer.getDstCompilationUnit()
                    )
            );
        }
        else {
            // if-block on the left side
            srcDstRange.getSrcRange().add(
                    RangeOperations.toLineRange(
                            RangeOperations.toRange(
                                    action.getNode().getParent()),editScriptStorer.getSrcCompilationUnit()
                    )
            );


            // if-block on the right side
            srcDstRange.getDstRange().add(
                    RangeOperations.toLineRange(
                            RangeOperations.toRange(
                                    mappings.get(action.getNode().getParent())), editScriptStorer.getDstCompilationUnit()
                    )
            );
        }


        return srcDstRange;
    }
}
