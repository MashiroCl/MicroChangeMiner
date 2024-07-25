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
 * @since 2024/03/03 11:18
 */
@Slf4j
public class AddConjunctOrDisjunct implements MicroChangePattern{

    // is insert a &&/|| to a if statement condition expression
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
      return action.getName().equals("insert-node")
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
        // condition expression on the right side
        Tree rightConditionExpression = NodePosition.isConditionExpression(action.getNode());

        // left side condition expression
        if(mappings.containsKey(rightConditionExpression)){
            srcDstRange.getSrcRange().add(
                    RangeOperations.toLineRange(
                            RangeOperations.toRange(mappings.get(rightConditionExpression)), editScriptStorer.getSrcCompilationUnit()
                    )
            );
        }

        //right side
        //added condition expression
        srcDstRange.getDstRange().add(
                RangeOperations.toLineRange(
                        RangeOperations.toRange(rightConditionExpression),editScriptStorer.getDstCompilationUnit()
                ));

        return srcDstRange;
    }
}
