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
//        return action.getName().equals("delete-node")
//                && NodePosition.isConditionExpression(action.getNode())!=null
//                && (action.getNode().getLabel().equals("||")||action.getNode().getLabel().equals("&&"));
        return false;
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        if(action.getName().equals("move-tree")){
            Tree leftConditionExpression = NodePosition.isConditionExpression(action.getNode());
            if(leftConditionExpression!=null){
                if(mappings.containsKey(action.getNode()) &&  NodePosition.isConditionExpression(mappings.get(action.getNode()))!=null){
                    for(Tree conditionNode: leftConditionExpression.getParent().getChild(0).getChildren()){
                        if(conditionNode.toString().contains("||") || conditionNode.toString().contains("&&")){
                            return nodeActions.containsKey(conditionNode) && nodeActions.get(conditionNode).stream().anyMatch(p->p.getName().contains("delete"));
                        }
                    }
                }
            }
        }

        return false;
    }

    @Override
    public SrcDstRange getSrcDstRange(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        SrcDstRange srcDstRange = new SrcDstRange();

        // left side condition expression
            srcDstRange.getSrcRange().add(
                    RangeOperations.toLineRange(
                            RangeOperations.toRange(action.getNode()), editScriptStorer.getSrcCompilationUnit()
                    )
            );

        //right side
        //condition expression
        if(mappings.containsKey(action.getNode())){
            srcDstRange.getDstRange().add(
                    RangeOperations.toLineRange(
                            RangeOperations.toRange(mappings.get(action.getNode())),editScriptStorer.getDstCompilationUnit()
                    ));
        }

        return srcDstRange;
    }
}
