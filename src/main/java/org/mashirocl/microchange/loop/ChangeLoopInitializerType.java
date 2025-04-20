package org.mashirocl.microchange.loop;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import org.mashirocl.editscript.EditScriptStorer;
import org.mashirocl.location.RangeOperations;
import org.mashirocl.microchange.MicroChangePattern;
import org.mashirocl.microchange.SrcDstRange;

import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2025/04/19 16:40
 */
public class ChangeLoopInitializerType implements MicroChangePattern {
    /**
     * The type of loop initializer is changed (e.g. for(int i=0;i<n;i++) -> for(Integer i =0; i<n;i++))
     *
     * An update happens to the node which is the type of the initializer, and the lable of the node changed
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
        return false;
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        if(action.getName().equals("update-node")){
            Tree udpateNode = action.getNode();
            Tree parent = action.getNode().getParent();
            Tree grandParent = parent!=null ? parent.getParent() : null;
            Tree forStatement = grandParent!=null? grandParent.getParent(): null;

            if(forStatement==null) return false;

            if(!grandParent.getType().name.equals("VariableDeclarationExpression") || !forStatement.getType().name.equals("ForStatement")) return false;


            if(!udpateNode.equals(parent.getChild(0))) return false;   // is the type of variable


            if(!grandParent.equals(forStatement.getChild(0))) return false;// is the variable declaration in for statement

            return mappings.containsKey(action.getNode()) && !mappings.get(action.getNode()).getLabel().equals(action.getNode().getLabel());   // type changed
        }


    return false;
    }

    @Override
    public SrcDstRange getSrcDstRange(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        SrcDstRange srcDstRange = new SrcDstRange();
        // source for-statement
        srcDstRange.getSrcRange().add(RangeOperations.toLineRange(RangeOperations.toRange(action.getNode()),
                editScriptStorer.getDstCompilationUnit()));

        //destination for-statement
        srcDstRange.getDstRange().add(RangeOperations.toLineRange(RangeOperations.toRange(mappings.get(action.getNode())),
                editScriptStorer.getDstCompilationUnit()));
        return srcDstRange;
    }
}
