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
public class ConvertForToForEach implements MicroChangePattern {
    /**
     * The for-loop is replaced by a foreach
     *
     * A child node (for-body) is moved from a for-statement to be the child of a foreach-statement,
     * the original for-statement is removed
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
        return false;
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        return action.getName().equals("move-tree")
                && action.getNode().getParent().getType().name.equals("ForStatement")
                && nodeActions.containsKey(action.getNode())
                && nodeActions.get(action.getNode().getParent()).stream().anyMatch(p->p.getName().contains("delete"))
                && mappings.containsKey(action.getNode())
                && mappings.get(action.getNode()).getParent().getType().name.equals("EnhancedForStatement");
    }

    @Override
    public SrcDstRange getSrcDstRange(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        // TODO: discussion: should we calculate the coverage  of only foreach-statement line or only foreach-body or both?
        SrcDstRange srcDstRange = new SrcDstRange();
        // for-statement
        srcDstRange.getSrcRange().add(RangeOperations.toLineRange(RangeOperations.toRange(action.getNode().getParent()),
                editScriptStorer.getDstCompilationUnit()));

        //foeach-statement
        Tree foreachBodyNode = mappings.get(action.getNode());
        Tree forStatementNode = foreachBodyNode.getParent();
        srcDstRange.getDstRange().add(RangeOperations.toLineRange(RangeOperations.toRange(forStatementNode),
                editScriptStorer.getDstCompilationUnit()));
        return srcDstRange;
    }
}
