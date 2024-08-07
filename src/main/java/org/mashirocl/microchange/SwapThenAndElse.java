package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.tree.Tree;
import org.mashirocl.editscript.EditScriptStorer;
import org.mashirocl.location.RangeOperations;

import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/10 9:18
 */
public class SwapThenAndElse implements MicroChangePattern {

    /**
     * condition:
     * for one action in the edit script
     * 1. name=="move-tree" -> the code appears in both before & after changes
     * 2. mappings.get(action.node).parent()=="IfStatement" -> the node is moved to be the subtree of a "IfStatement"
     * 3. the parent node of the being moved node is the same "IfStatement" before and after move
     * @return
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings){
        if(!action.getName().equals("move-tree"))
            return false;

        if(!((Move)action).getParent().getType().name.equals("IfStatement"))
            return false;

        Tree beforeMoveNode = action.getNode();
        if(!mappings.containsKey(beforeMoveNode) || !mappings.containsKey(beforeMoveNode.getParent())) return false;
        Tree afterMoveNode = mappings.get(beforeMoveNode);
        if(!mappings.containsKey(afterMoveNode)) return false;

        return beforeMoveNode.getParent().getType().name.equals("IfStatement")
                && mappings.get(beforeMoveNode.getParent()).equals(afterMoveNode.getParent());
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        return matchConditionGumTree(action, mappings);
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


    //TODO for remove redundant else
    public boolean matchConditionGumTree2(Action action, Map<Tree, Tree> mappings){
        System.out.println("action");
        System.out.println(action);
        if(!action.getName().equals("move-tree"))
            return false;
        List<Tree> treeList = action.getNode().getParent().getChildren();
        treeList = treeList.stream().filter(p->p.getType().name.equals("IfStatement")).toList();
        if(treeList.isEmpty()) return false;
        for(Tree tree:treeList){
            System.out.println("tree.getChildren");
            System.out.println(tree.getChildren());
            System.out.println("mappings.get(tree)");
            System.out.println(mappings.get(tree));
            System.out.println("mappings.get(action.getNode()).getParent())");
            System.out.println(mappings.get(action.getNode()).getParent());
            System.out.println("mappings.get(action.getNode()).getParent().getParent)");
            System.out.println(mappings.get(action.getNode()).getParent().getParent());
            if(mappings.containsKey(action.getNode())
                    && mappings.containsKey(tree)
                    && mappings.get(action.getNode()).getParent().equals(mappings.get(tree))) return true;
        }
        return false;
    }

}
