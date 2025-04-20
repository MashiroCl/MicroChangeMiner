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
 * @since 2025/04/20 14:41
 */
public class RenameLoopInitializer implements MicroChangePattern {
    /**
     * The name of loop initializer is changed (e.g. for(int i=0;i<n;i++) -> for(int j =0; j<n;j++))
     *
     * An update happens to the node which is the name of the initializer, and the lable of the node changed
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
        return false;
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        if(action.getName().equals("update-node")){
            Tree udpateNode = action.getNode();
            Tree parent = action.getNode().getParent();  // parent VariableDeclarationFragment -> VariableExpression -> ForStatement
            Tree grandParent = parent!=null ? parent.getParent() : null;
            Tree forStatement = grandParent!=null? grandParent.getParent(): null;

            if(forStatement==null) return false;

            if(!parent.getType().name.equals("VariableDeclarationFragment") || !forStatement.getType().name.equals("ForStatement")) return false;


            if(!udpateNode.equals(parent.getChild(0))) return false;   // is initializer name


            if(!parent.equals(grandParent.getChild(1))) return false;// is the variable declaration (type is the index 0 child)

            System.out.println(mappings.containsKey(action.getNode()) && !mappings.get(action.getNode()).getLabel().equals(action.getNode().getLabel()));

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
