package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.tree.Tree;
import lombok.extern.slf4j.Slf4j;
import org.mashirocl.editscript.EditScriptStorer;
import org.mashirocl.location.RangeOperations;

import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/02/02 15:17
 */
@Slf4j
public class ChangeBoundaryCondition implements MicroChangePattern{
    /**
     * condition:
     * for one action in the edit script
     * 1. is `update-node`
     * 2. `>`/`<` is updated to `>=`/`<=`, or vice versa
     * 3. the left side and right side of the relational operators are the same before and after change
     * @return
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
        return isSmallerThanIncludeEqual(action,mappings) || isSmallerThanExcludeEqual(action, mappings)
                || isGreaterThanIncludeEqual(action, mappings) || isGreaterThanExcludeEqual(action, mappings);
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        return matchConditionGumTree(action, mappings);
    }

    @Override
    public SrcDstRange getSrcDstRange(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        SrcDstRange srcDstRange = new SrcDstRange();
        srcDstRange.getSrcRange().add(RangeOperations.toLineRange(RangeOperations.toRange(action.getNode()),
                editScriptStorer.getSrcCompilationUnit()));
        srcDstRange.getDstRange().add(RangeOperations.toLineRange(RangeOperations.toRange(mappings.get(action.getNode())),
                editScriptStorer.getDstCompilationUnit()));
        return srcDstRange;
    }

    private boolean isBeforeAfterEqualForInfix(Action action, Map<Tree, Tree> mappings){
        if(action.getNode().getParent().getChildren().size()>2
                && mappings.containsKey(action.getNode().getParent())
                && mappings.get(action.getNode().getParent()).getChildren().size()>2){
            String beforeLeftElement = action.getNode().getParent().getChild(0).getLabel();
            String beforeRightElement = action.getNode().getParent().getChild(2).getLabel();
            String afterLeftElement = mappings.get(action.getNode().getParent()).getChild(0).getLabel();
            String afterRightElement = mappings.get(action.getNode().getParent()).getChild(2).getLabel();
            return beforeLeftElement.equals(afterLeftElement) && beforeRightElement.equals(afterRightElement);
        }
        return false;
    }
    private boolean isGreaterThanIncludeEqual(Action action, Map<Tree, Tree> mappings){
        return action.getName().equals("update-node")
                && action.getNode().getLabel().equals(">")
                && ((Update) action).getValue().equals(">=")
                && isBeforeAfterEqualForInfix(action, mappings);
    }

    private boolean isSmallerThanIncludeEqual(Action action, Map<Tree, Tree> mappings){
        return action.getName().equals("update-node")
                && action.getNode().getLabel().equals("<")
                && ((Update) action).getValue().equals("<=")
                && isBeforeAfterEqualForInfix(action, mappings);
    }

    private boolean isGreaterThanExcludeEqual(Action action, Map<Tree, Tree> mappings){
        return action.getName().equals("update-node")
                && action.getNode().getLabel().equals(">=")
                && ((Update) action).getValue().equals(">")
                && isBeforeAfterEqualForInfix(action, mappings);
    }

    private boolean isSmallerThanExcludeEqual(Action action, Map<Tree, Tree> mappings){
        if(action.getName().equals("update-node")
                && action.getNode().getLabel().equals("<=")
                && ((Update) action).getValue().equals("<")){
        }
        return action.getName().equals("update-node")
                && action.getNode().getLabel().equals("<=")
                && ((Update) action).getValue().equals("<")
                && isBeforeAfterEqualForInfix(action, mappings);
    }
}
