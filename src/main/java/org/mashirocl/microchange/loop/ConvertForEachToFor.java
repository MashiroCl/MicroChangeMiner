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
 * @since 2025/04/19 10:53
 */
public class ConvertForEachToFor implements MicroChangePattern {
    /**
     * The foreach-loop is replaced by a for
     *
     * A child node (foreach-body) is moved from a foreach-statement to be the child of a for-statement,
     * the original foreach-statement is removed
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
        return false;
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        return action.getName().equals("move-tree")
                && action.getNode().getParent().getType().name.equals("EnhancedForStatement")
                && nodeActions.containsKey(action.getNode())
                && nodeActions.get(action.getNode().getParent()).stream().anyMatch(p->p.getName().contains("delete"))
                && mappings.containsKey(action.getNode())
                && mappings.get(action.getNode()).getParent().getType().name.equals("ForStatement");
    }

    @Override
    public SrcDstRange getSrcDstRange(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        // TODO: discussion: should we calculate the coverage  of only foreach-statement line or only foreach-body or both?
        SrcDstRange srcDstRange = new SrcDstRange();
        // foreach-statement
        srcDstRange.getSrcRange().add(RangeOperations.toLineRange(RangeOperations.toRange(action.getNode().getParent()),
                editScriptStorer.getDstCompilationUnit()));
        // foreach-body
//        srcDstRange.getSrcRange().add(RangeOperations.toLineRange(RangeOperations.toRange(action.getNode()),
//                editScriptStorer.getDstCompilationUnit()));

        //for-statement
        Tree forBodyNode = mappings.get(action.getNode());
        Tree forStatementNode = forBodyNode.getParent();
        srcDstRange.getSrcRange().add(RangeOperations.toLineRange(RangeOperations.toRange(forStatementNode),
                editScriptStorer.getDstCompilationUnit()));
        //for-body
//        srcDstRange.getDstRange().add(RangeOperations.toLineRange(RangeOperations.toRange(whileBodyNode),
//                editScriptStorer.getDstCompilationUnit()));
        return srcDstRange;
    }
}
