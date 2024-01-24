package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.tree.Tree;

import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/23 21:51
 */
public class ReverseConditional implements MicroChangePattern{
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
        return isInsertExclamationInfix(action)
                || isInsertExclamationPrefix(action)
                || isRemoveExclamationInfix(action)
                || isInverseSmallerOrEqualThan(action)
                || isInverseSmallerOrEqualThanOrderChange(action, mappings)
                || isInverseSmallerThan(action)
                || isInverseSmallerThanOrderChange(action, mappings)
                || isInverseGreaterOrEqualThan(action)
                || isInverseGreaterOrEqualThanOrderChange(action, mappings)
                || isInverseGreaterThan(action)
                || isInverseGreaterThanOrderChange(action, mappings);
    }

    /**
     * X==Y -> X!=Y
     * X==Y -> Y!=X
     */
    private boolean isInsertExclamationInfix(Action action){
        return action.getName().equals("update-node")
                && action.getNode().getLabel().equals("==")
                && ((Update) action).getValue().equals("!=");
    }

    /**
     * X!=Y -> X==Y
     * X==Y -> Y!=X
     */
    private boolean isRemoveExclamationInfix(Action action){
        return action.getName().equals("update-node")
                && action.getNode().getLabel().equals("!=")
                && ((Update) action).getValue().equals("==");
    }

    /**
     * X -> !X
     */
    private boolean isInsertExclamationPrefix(Action action){
        return action.getName().equals("insert-node")
                && action.getNode().toString().contains("PREFIX_EXPRESSION_OPERATOR: !")
                && action.getNode().getParent().getParent().getType().name.equals("IfStatement");
    }

    /**
     * X>Y -> X<=Y
     */
    private boolean isInverseGreaterThan(Action action){
        return action.getName().equals("update-node")
                && action.getNode().getLabel().equals(">")
                && ((Update) action).getValue().equals("<=");
    }

    /**
     * X>Y -> Y>=X
     */
    private boolean isInverseGreaterThanOrderChange(Action action, Map<Tree, Tree> mappings){
        return action.getName().equals("update-node")
                && mappings.get(action.getNode()).getLabel().equals(((Update) action).getValue())
                && action.getNode().getLabel().equals(">")
                && ((Update) action).getValue().equals(">=");
    }

    /**
     * X<Y -> X>=Y
     */
    private boolean isInverseSmallerThan(Action action){
        return action.getName().equals("update-node")
                && action.getNode().getLabel().equals("<")
                && ((Update) action).getValue().equals(">=");
    }

    /**
     * X<Y -> Y<=X
     */
    private boolean isInverseSmallerThanOrderChange(Action action, Map<Tree, Tree> mappings){
        return action.getName().equals("update-node")
                && mappings.get(action.getNode()).getLabel().equals(((Update) action).getValue())
                && action.getNode().getLabel().equals("<")
                && ((Update) action).getValue().equals("<=");
    }

    /**
     *
     * X>=Y -> X<Y
     */
    private boolean isInverseGreaterOrEqualThan(Action action){
        return action.getName().equals("update-node")
                && action.getNode().getLabel().equals(">=")
                && ((Update) action).getValue().equals("<");
    }

    /**
     * X>=Y -> Y<X
     */
    private boolean isInverseGreaterOrEqualThanOrderChange(Action action, Map<Tree, Tree> mappings){
        return action.getName().equals("update-node")
                && mappings.get(action.getNode()).getLabel().equals(((Update) action).getValue())
                && action.getNode().getLabel().equals(">=")
                && ((Update) action).getValue().equals("<");
    }

    /**
     *
     * X<=Y -> X>Y
     */
    private boolean isInverseSmallerOrEqualThan(Action action){
        return action.getName().equals("update-node")
                && action.getNode().getLabel().equals("<=")
                && ((Update) action).getValue().equals(">");
    }

    /**
     * X<=Y -> Y<X
     */
    private boolean isInverseSmallerOrEqualThanOrderChange(Action action, Map<Tree, Tree> mappings){
        return action.getName().equals("update-node")
                && mappings.get(action.getNode()).getLabel().equals(((Update) action).getValue())
                && action.getNode().getLabel().equals("<=")
                && ((Update) action).getValue().equals("<");
    }

}
