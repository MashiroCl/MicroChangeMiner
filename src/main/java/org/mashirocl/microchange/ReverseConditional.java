package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.tree.Tree;
import com.google.common.collect.Range;
import lombok.extern.slf4j.Slf4j;
import org.mashirocl.editscript.EditScriptStorer;
import org.mashirocl.location.RangeOperations;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/23 21:51
 */
@Slf4j
public class ReverseConditional implements MicroChangePattern{

    public float matchConditionGumTreeCL(Action action,  Map<Tree, Tree> mappings){
        float confidenceLevel = 0.0f;
        if(action.getName().equals("update-node")
                && action.getNode().getLabel().equals("==")
                && ((Update) action).getValue().equals("!=")){
                confidenceLevel += 0.7f;
                // check element same
                if((action.getNode().getParent().getChild(0).toString()
                        .equals(mappings.get(action.getNode().getParent()).getChild(0).toString())
                        && action.getNode().getParent().getChild(2).toString()
                        .equals(mappings.get(action.getNode().getParent()).getChild(2).toString()))
                        || (action.getNode().getParent().getChild(0).toString()
                        .equals(mappings.get(action.getNode().getParent()).getChild(2).toString())
                        && action.getNode().getParent().getChild(2).toString()
                        .equals(mappings.get(action.getNode().getParent()).getChild(0).toString()))){
                    confidenceLevel += 0.3f;
                }
        }
        return confidenceLevel;
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
//        System.out.println("***************************");
//        System.out.println(action);
//        System.out.println(action.getNode().getParent());
//        System.out.println(mappings.get(action.getNode().getParent()));
//        if(isInsertExclamationInfix(action, mappings)){
//            if(action.getNode().getParent().getChild(0).toString()
//                    .equals(mappings.get(action.getNode().getParent()).getChild(0).toString())
//            && action.getNode().getParent().getChild(2).toString()
//                    .equals(mappings.get(action.getNode().getParent()).getChild(2).toString())){
//                System.out.println("yes");
//            }
//            System.out.println("**********************");
//            System.out.println(action.getNode().getParent().getChild(0)); //X
//            System.out.println(action.getNode().getParent().getChild(2)); //Y
//            if(mappings.containsKey(action.getNode().getParent())){
//                System.out.println(mappings.get(action.getNode().getParent()).getChild(0));
//                System.out.println(mappings.get(action.getNode().getParent()).getChild(2));
//            }
//            System.out.println("-------------------------");
//        }


        return isInsertExclamationInfix(action, mappings)
                || isRemoveExclamationInfix(action, mappings)
                || isInverseSmallerOrEqualThan(action, mappings)
                || isInverseSmallerOrEqualThanOrderChange(action, mappings)
                || isInverseSmallerThan(action, mappings)
                || isInverseSmallerThanOrderChange(action, mappings)
                || isInverseGreaterOrEqualThan(action, mappings)
                || isInverseGreaterOrEqualThanOrderChange(action, mappings)
                || isInverseGreaterThan(action, mappings)
                || isInverseGreaterThanOrderChange(action, mappings)
                || isAddExclamationPrefix(action,mappings)
                || isDeleteExclamationPrefix(action, mappings);
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        return matchConditionGumTree(action, mappings);
    }

    @Override
    public SrcDstRange getSrcDstRange(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        SrcDstRange srcDstRange = new SrcDstRange();
        //TODO calculate according to the type of action

        switch (action.getName()){
            case "insert-node":
                // insert ! in prefix
                srcDstRange.getDstRange().add(
                        RangeOperations.toLineRange(
                                RangeOperations.toRange(action.getNode()),editScriptStorer.getDstCompilationUnit()
                        )
                );
                srcDstRange.getSrcRange().add(
                        RangeOperations.toLineRange(
                                RangeOperations.toRange(mappings.get(action.getNode().getParent().getChild(1))),editScriptStorer.getSrcCompilationUnit()
                        )
                );
                break;
            case "delete-node":
                // delete ! in prefix
                srcDstRange.getSrcRange().add(
                        RangeOperations.toLineRange(
                                RangeOperations.toRange(action.getNode()), editScriptStorer.getSrcCompilationUnit()
                        ));
                srcDstRange.getDstRange().add(
                        RangeOperations.toLineRange(
                                RangeOperations.toRange(mappings.get(action.getNode().getParent().getChild(1))), editScriptStorer.getDstCompilationUnit()
                        ));
                break;
            default:
                srcDstRange.getSrcRange().add(
                        RangeOperations.toLineRange(
                                RangeOperations.toRange(action.getNode()), editScriptStorer.getSrcCompilationUnit()
                        ));
                srcDstRange.getDstRange().add(
                        RangeOperations.toLineRange(
                                RangeOperations.toRange(mappings.get(action.getNode())),editScriptStorer.getDstCompilationUnit()
                        )
                );
        }
        return srcDstRange;
    }


    /**
     * X==Y -> X!=Y
     * X==Y -> Y!=X
     *
     */
    private boolean isInsertExclamationInfix(Action action, Map<Tree, Tree> mappings){

        if(action.getName().equals("update-node")
                && action.getNode().getLabel().equals("==")
                && ((Update) action).getValue().equals("!=")){

            return isBeforeAfterEqualForInfix(action, mappings) || isBeforeAfterReverseForInfix(action, mappings);

        }

        return false;
    }

    /**
     * X!=Y -> X==Y
     * X==Y -> Y!=X
     */
    private boolean isRemoveExclamationInfix(Action action, Map<Tree, Tree> mappings){
        if(action.getName().equals("update-node")
                && action.getNode().getLabel().equals("!=")
                && ((Update) action).getValue().equals("==")){

            return isBeforeAfterEqualForInfix(action, mappings) || isBeforeAfterReverseForInfix(action, mappings);
        }
        return false;
    }

    /**
     * X ->!X
     *
     * @param action
     * @param mappings
     * @return
     */
    private boolean isAddExclamationPrefix(Action action, Map<Tree, Tree> mappings){
        return action.getName().equals("insert-node")
                && action.getNode().getParent().getParent().getType().name.equals("IfStatement")
                && action.getNode().toString().contains("PREFIX_EXPRESSION_OPERATOR: !")
                && mappings.containsKey(action.getNode().getParent().getChild(1)) // the expression of the prefix expression on the left side exists
                && mappings.get(action.getNode().getParent().getChild(1)).getParent().getType().name.equals("IfStatement") // the expression on the left is the child of if
                && mappings.get(action.getNode().getParent().getChild(1)).isIsomorphicTo(action.getNode().getParent().getChild(1)); // the expression on the left side and right side are equal to each other
    }

    /**
     * !X ->X
     *
     * @param action
     * @param mappings
     * @return
     */
    private boolean isDeleteExclamationPrefix(Action action, Map<Tree, Tree> mappings){
        //TODO:  action.getNode().getParent().getParent() can be a InfixExpression, e.g., if A&B&C -> if A&!B&C
        return action.getName().equals("delete-node")
                && action.getNode().getParent()!=null
                && action.getNode().getParent().getParent()!=null
                && action.getNode().getParent().getParent().getType().name.equals("IfStatement")
                && action.getNode().toString().contains("PREFIX_EXPRESSION_OPERATOR: !")
                && mappings.containsKey(action.getNode().getParent().getChild(1)) // the expression of the prefix expression still exists
                && mappings.get(action.getNode().getParent().getChild(1)).getParent().getType().name.equals("IfStatement") // the expression on the left side is the child of a if
                && mappings.get(action.getNode().getParent().getChild(1)).isIsomorphicTo(action.getNode().getParent().getChild(1)); // the expression before and after is the same
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

    private boolean isBeforeAfterReverseForInfix(Action action, Map<Tree, Tree> mappings){
        if(action.getNode().getParent().getChildren().size()>2
                && mappings.containsKey(action.getNode().getParent())
                && mappings.get(action.getNode().getParent()).getChildren().size()>2){
            String beforeLeftElement = action.getNode().getParent().getChild(0).getLabel();
            String beforeRightElement = action.getNode().getParent().getChild(2).getLabel();
            String afterLeftElement = mappings.get(action.getNode().getParent()).getChild(0).getLabel();
            String afterRightElement = mappings.get(action.getNode().getParent()).getChild(2).getLabel();
            return beforeLeftElement.equals(afterRightElement) && beforeRightElement.equals(afterLeftElement);
        }
        return false;
    }

    /**
     * X -> !X
     */
    private boolean isInsertExclamationPrefix(Action action, Map<Tree, Tree> mappings){
        if(action.getName().equals("insert-node")
                && action.getNode().toString().contains("PREFIX_EXPRESSION_OPERATOR: !")
                && action.getNode().getParent().getParent().getType().name.equals("IfStatement")){

            if(action.getNode().getParent().getChildren().size()>1
                    && mappings.containsKey(action.getNode().getParent().getChild(1))
                    && action.getNode().getParent().getChild(1).getType().toString()     // condition before and after are boolean variable/  method invocation
                    .equals(mappings.get(action.getNode().getParent().getChild(1)).toString())){ //TODO: anyway to unify the boolean and method invocation?
                // the condition is a boolean variable
                if(action.getNode().getParent().getChild(1).getType().toString().equals("SimpleName")){
                    if(action.getNode().getParent().getChild(1).getLabel()
                            .equals(mappings.get(action.getNode().getParent().getChild(1)).getLabel())){
                        return true;
                    }
                }

                // the condition is a method invocation
                if(action.getNode().getParent().getChild(1).getType().toString().equals("MethodInvocation")){
                    return action.getNode().getParent().getChild(1).getChild(1).getLabel()  // use getlabel or use to string?
                            .equals(mappings.get(action.getNode().getParent().getChild(1)).getChild(1).getLabel());

//                    System.out.println("in method invocation");
//                    System.out.println(action.getNode().getParent().getChild(1).getChild(0));
//                    System.out.println(action.getNode().getParent().getChild(1).getChild(1));
//                    System.out.println(mappings.get(action.getNode()));
//                    System.out.println(mappings.get(action.getNode().getParent()));
//                    System.out.println(mappings.get(action.getNode().getParent().getChild(1)));
//                    System.out.println(mappings.get(action.getNode().getParent().getChild(1)).getChild(1));
////                    System.out.println(mappings.get(action.getNode().getParent()).getChild(1).getChild(1));
                }
            }
        }

        return false;
    }



    /**
     * X>Y -> X<=Y
     */
    private boolean isInverseGreaterThan(Action action, Map<Tree, Tree> mappings){
        return action.getName().equals("update-node")
                && action.getNode().getLabel().equals(">")
                && ((Update) action).getValue().equals("<=")
                && isBeforeAfterEqualForInfix(action, mappings);
    }



    /**
     * X>Y -> Y>=X
     */
    private boolean isInverseGreaterThanOrderChange(Action action, Map<Tree, Tree> mappings){
        return action.getName().equals("update-node")
                && mappings.get(action.getNode()).getLabel().equals(((Update) action).getValue())
                && action.getNode().getLabel().equals(">")
                && ((Update) action).getValue().equals(">=")
                && isBeforeAfterReverseForInfix(action, mappings);

    }

    /**
     * X<Y -> X>=Y
     */
    private boolean isInverseSmallerThan(Action action, Map<Tree, Tree> mappings){
        return action.getName().equals("update-node")
                && action.getNode().getLabel().equals("<")
                && ((Update) action).getValue().equals(">=")
                && isBeforeAfterEqualForInfix(action, mappings);
    }

    /**
     * X<Y -> Y<=X
     */
    private boolean isInverseSmallerThanOrderChange(Action action, Map<Tree, Tree> mappings){
        return action.getName().equals("update-node")
                && mappings.get(action.getNode()).getLabel().equals(((Update) action).getValue())
                && action.getNode().getLabel().equals("<")
                && ((Update) action).getValue().equals("<=")
                && isBeforeAfterReverseForInfix(action, mappings);
    }

    /**
     *
     * X>=Y -> X<Y
     */
    private boolean isInverseGreaterOrEqualThan(Action action, Map<Tree, Tree> mappings){
        return action.getName().equals("update-node")
                && action.getNode().getLabel().equals(">=")
                && ((Update) action).getValue().equals("<")
                && isBeforeAfterEqualForInfix(action, mappings);
    }

    /**
     * X>=Y -> Y<X
     */
    private boolean isInverseGreaterOrEqualThanOrderChange(Action action, Map<Tree, Tree> mappings){
        return action.getName().equals("update-node")
                && mappings.get(action.getNode()).getLabel().equals(((Update) action).getValue())
                && action.getNode().getLabel().equals(">=")
                && ((Update) action).getValue().equals("<")
                && isBeforeAfterReverseForInfix(action, mappings);
    }

    /**
     *
     * X<=Y -> X>Y
     */
    private boolean isInverseSmallerOrEqualThan(Action action, Map<Tree, Tree> mappings){
        return action.getName().equals("update-node")
                && action.getNode().getLabel().equals("<=")
                && ((Update) action).getValue().equals(">")
                && isBeforeAfterEqualForInfix(action, mappings);
    }

    /**
     * X<=Y -> Y<X
     */
    private boolean isInverseSmallerOrEqualThanOrderChange(Action action, Map<Tree, Tree> mappings){
        return action.getName().equals("update-node")
                && mappings.get(action.getNode()).getLabel().equals(((Update) action).getValue())
                && action.getNode().getLabel().equals("<=")
                && ((Update) action).getValue().equals("<")
                && isBeforeAfterReverseForInfix(action, mappings);
    }

}
