package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import lombok.extern.slf4j.Slf4j;
import org.mashirocl.editscript.EditScriptStorer;
import org.mashirocl.location.RangeOperations;
import org.mashirocl.microchange.common.NodePosition;

import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/03/03 14:46
 */
@Slf4j
public class SimplifyConditional implements MicroChangePattern{
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
        return action.getName().equals("delete-node")
                && NodePosition.isConditionExpression(action.getNode())!=null
                && (action.getNode().getLabel().equals("||")||action.getNode().getLabel().equals("&&"));
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        return matchConditionGumTree(action, mappings);
    }

    @Override
    public SrcDstRange getSrcDstRange(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        SrcDstRange srcDstRange = new SrcDstRange();
        // condition expression on the left side
        Tree leftConditionExpression = NodePosition.isConditionExpression(action.getNode());

        // left side condition expression
            srcDstRange.getSrcRange().add(
                    RangeOperations.toLineRange(
                            RangeOperations.toRange(leftConditionExpression), editScriptStorer.getSrcCompilationUnit()
                    )
            );

        //right side
        //added condition expression
        if(mappings.containsKey(leftConditionExpression)){
            srcDstRange.getDstRange().add(
                    RangeOperations.toLineRange(
                            RangeOperations.toRange(mappings.get(leftConditionExpression)),editScriptStorer.getDstCompilationUnit()
                    ));
        }

        return srcDstRange;
    }
}
