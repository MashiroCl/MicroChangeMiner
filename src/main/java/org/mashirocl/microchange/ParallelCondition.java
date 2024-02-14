package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import org.mashirocl.editscript.EditScriptStorer;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/26 9:26
 */
public class ParallelCondition implements MicroChangePattern{
    /**
     * 1. is `move-tree`
     * 2. before move, it is in an IfStatement (action.getnode.parent is InfixExpreesion)
     * 3. before move it is the first child (condition) of the IfStatement
     * 4. after move, it is in an IfStatement
     * 5. after move, the IfStatement it is in is a child of another IfStatement (after move parent
     * is InfixExpreesion, parent parent is block, parent parent parent is thd another IfStatement)
     * 6. the IfStatement it is in before move should map to the after move parent IfStatement
     * @param action
     * @param mappings
     * @return
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
        if(action.getName().equals("move-tree")
                && action.getNode().getParent().getParent().getType().name.equals("IfStatement")
                && action.getNode().getParent().getChild(0).equals(action.getNode())
                && mappings.containsKey(action.getNode())
                && mappings.get(action.getNode()).getParent().getType().name.equals("IfStatement")
                && mappings.get(action.getNode()).getParent().getParent().getParent().getType().name.equals("IfStatement")
                && mappings.containsKey(action.getNode().getParent().getParent())
                && mappings.get(action.getNode().getParent().getParent()).equals((mappings.get(action.getNode()).getParent().getParent().getParent()))){
//            System.out.println(action);
//            System.out.println(action.getNode());
//            System.out.println("start position");
//            System.out.println(action.getNode().getPos());
//            System.out.println("end position");
//            System.out.println(action.getNode().getEndPos());
//            System.out.println("end position");
//            System.out.println("length");
//            System.out.println(action.getNode().getLength());
        }
        return action.getName().equals("move-tree")
                && action.getNode().getParent().getParent().getType().name.equals("IfStatement")
                && action.getNode().getParent().getChild(0).equals(action.getNode())
                && mappings.containsKey(action.getNode())
                && mappings.get(action.getNode()).getParent().getType().name.equals("IfStatement")
                && mappings.get(action.getNode()).getParent().getParent().getParent().getType().name.equals("IfStatement")
                && mappings.containsKey(action.getNode().getParent().getParent())
                && mappings.get(action.getNode().getParent().getParent()).equals((mappings.get(action.getNode()).getParent().getParent().getParent()));
    }

    /**
     *
     * @param action
     * @param mappings
     * @param nodeActions
     * @return
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
//        System.out.println("---------------This is an action---------------");
//        System.out.println(action);
//        System.out.println("parent");
//        System.out.println(action.getNode().getParent());
//        if(mappings.containsKey(action.getNode())){
//            System.out.println(mappings.get(action.getNode()).getParent());
//        }
//        System.out.println("The followings are the nodeActions: "+nodeActions.get(action.getNode()).size());
//        for(Action a:nodeActions.get(action.getNode())){
//            System.out.println(a);
//        }

        // one condition in the if statement is moved to another if statement
        if(action.getName().equals("move-tree")
//                && (action.getNode().getParent().getType().toString().equals("IfStatement") || (action.getNode().getParent().getParent().getType().toString().equals("IfStatement")) // a boolean variable/ a Infix/Prefix expression
                && action.getNode().getParent().getParent().getType().toString().equals("IfStatement") // a Infix/Prefix expression //TODO a boolean variable/
                && action.getNode().getParent().equals(action.getNode().getParent().getParent().getChild(0))// the Infix/Prefix expression is in the condition part of the if
                && mappings.containsKey(action.getNode())
                && mappings.get(action.getNode()).getParent().getType().toString().equals("IfStatement")){
            System.out.println("here");
//            System.out.println(action.getNode().getParent());
//            System.out.println(action.getNode().getParent().getParent());
//            System.out.println(nodeActions.containsKey(action.getNode().getParent().getParent().getChild(0)));
//            System.out.println(action.getNode().getParent().getParent().getChild(0).toTreeString());

           return isAnyChildNodeIsMovedToAIfStatement(action.getNode().getParent().getParent().getChild(0).getChildren(),
                   mappings,
                   nodeActions,
                   action.getNode());  //skip itself to avoid misdetect if(A&B) -> if(A) as ParallelCondition
        }


        return false;
    }

    @Override
    public List<Position> getPosition(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        List<Position> positions = new LinkedList<>();
        // left side
        Position movedCondition = new Position(
                editScriptStorer.getSrcCompilationUnit().getLineNumber(action.getNode().getPos()),
                editScriptStorer.getSrcCompilationUnit().getLineNumber(action.getNode().getEndPos()));
//        System.out.println("left side position");
//        System.out.println(movedCondition);
        // right side
        Position dstCondition = new Position(
                editScriptStorer.getDstCompilationUnit().getLineNumber(mappings.get(action.getNode()).getParent().getChild(0).getPos()),
                editScriptStorer.getDstCompilationUnit().getLineNumber(mappings.get(action.getNode()).getParent().getChild(0).getEndPos()));
//        System.out.println("dst side position");
//        System.out.println(dstCondition);
        positions.add(movedCondition);
        positions.add(dstCondition);
        return positions;
    }

    private boolean isAnyChildNodeIsMovedToAIfStatement(List<Tree> nodes,Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, Tree skip){
        for(Tree n:nodes){
            if(n.equals(skip)) continue;
            if(nodeActions.containsKey(n)){
                List<Action> actions = nodeActions.get(n);
                for(Action a:actions){
//                    if(a.getName().equals("move-tree")){
//                        System.out.println(a.getNode());
//                        System.out.println(a.getNode().getParent());
//                        System.out.println(a.getNode().getParent().getParent());
//                        System.out.println(mappings.containsKey(a.getNode()));
//                        if(mappings.containsKey(a.getNode())){
//                            System.out.println(mappings.get(a.getNode()).getParent());
//                        }
//                    }
                    if(a.getName().equals("move-tree")
                            && a.getNode().getParent().getParent().getType().toString().equals("IfStatement")
                            && mappings.containsKey(a.getNode())
                            && mappings.get(a.getNode()).getParent().getType().toString().equals("IfStatement")){
                        return true;
                    }
                }
            }

        }
        return false;
    }
}
