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
 * @since 2024/03/15 11:34
 */

@Slf4j
public class AddCurlyBrace implements MicroChangePattern{
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
//        log.info("action is :{}",action);
//        log.info("action.getName(): {}", action.getName());
//        log.info("action.getNode(): {}", action.getNode());
//        log.info("aciton.getNode().getChild(0): {}",action.getNode().getChild(0));
//        log.info("action.getNode().getParent(): {}", action.getNode().getParent());
//        log.info("action.getNode().getChildren(): {}", action.getNode().getChildren().size());
//        if(mappings.containsKey(action.getNode().getParent())){
//            log.info("mappings.get(action.getNode().getParent()).getChild(0): {}", mappings.get(action.getNode().getParent()).getChild(0));
//            log.info("mappings.get(action.getNode().getParent()).getChild(1): {}", mappings.get(action.getNode().getParent()).getChild(1));
//            log.info("mappings.get(action.getNode().getParent()).getChild(1).isIsomorphicTo(action.getNode().getChild(0)): {}", mappings.get(action.getNode().getParent()).getChild(1).isIsomorphicTo(action.getNode().getChild(0)));
//        }

        return action.getName().equals("insert-node")
                && action.getNode().toString().contains("Block")
                && action.getNode().getChildren().size() == 1
                && action.getNode().getParent().getType().name.equals("IfStatement")
                && mappings.containsKey(action.getNode().getParent())
                && mappings.get(action.getNode().getParent()).getChild(1).isIsomorphicTo(action.getNode().getChild(0));
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        return matchConditionGumTree(action, mappings);
    }

    @Override
    public SrcDstRange getSrcDstRange(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        SrcDstRange srcDstRange = new SrcDstRange();
        // the whole if-block can be explained
        // right side
        srcDstRange.getDstRange().add(
                RangeOperations.toLineRange(
                        RangeOperations.toRange(action.getNode().getParent()), editScriptStorer.getDstCompilationUnit()
                )
        );

        //left side
        srcDstRange.getSrcRange().add(
                RangeOperations.toLineRange(
                        RangeOperations.toRange(mappings.get(action.getNode().getParent())), editScriptStorer.getSrcCompilationUnit()
                )
        );

        return srcDstRange;
    }
}
