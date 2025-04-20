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
 * @since 2025/04/20 15:13
 */
public class ChangeLoopInitializationExpression implements MicroChangePattern {
    /**
     * The initialization expression of loop initializer is changed (e.g. for(int i=0;i<n;i++) -> for(int i =init(); i<n;i++))
     *
     * The node/tree of the initialization expression is deleted, and a new expression is inserted into it
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
        return false;
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        if(action.getName().equals("delete-node") || action.getName().equals("delete-tree")){
            Tree deleteNode = action.getNode();
            Tree parent = action.getNode().getParent();
            Tree grandParent = parent!=null ? parent.getParent() : null;
            Tree forStatement = grandParent!=null? grandParent.getParent(): null;

            if(forStatement==null) return false;

            if(!grandParent.getType().name.equals("VariableDeclarationExpression") || !forStatement.getType().name.equals("ForStatement")) return false;

            if(!deleteNode.equals(parent.getChild(1))) return false;   // is the expression

            if(!grandParent.equals(forStatement.getChild(0))) return false;// is the variable declaration in for statement

            return mappings.containsKey(parent) && !mappings.get(parent).getChild(1).isIsomorphicTo(deleteNode);   // expression changed
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
        srcDstRange.getDstRange().add(RangeOperations.toLineRange(RangeOperations.toRange(mappings.get(action.getNode().getParent()).getChild(1)),
                editScriptStorer.getDstCompilationUnit()));
        return srcDstRange;
    }
}
