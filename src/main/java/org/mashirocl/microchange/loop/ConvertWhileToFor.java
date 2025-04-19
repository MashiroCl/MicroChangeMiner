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
 * @since 2025/04/18 20:22
 */
public class ConvertWhileToFor implements MicroChangePattern {
    /**
     * The while-loop is replaced by a for-loop
     *
     * The A child node (while-body) is moved from a while-statement to be the child of a for-statement,
     * the original while-statement is removed
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
        return false;
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        return action.getName().equals("move-tree")
                && action.getNode().getParent().getType().name.equals("WhileStatement")
                && nodeActions.containsKey(action.getNode())
                && nodeActions.get(action.getNode().getParent()).stream().anyMatch(p->p.getName().contains("delete"))
                && mappings.containsKey(action.getNode())
                && mappings.get(action.getNode()).getParent().getType().name.equals("ForStatement");
    }

    @Override
    public SrcDstRange getSrcDstRange(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        // being moved for-loop body
        // TODO: discussion: should we calculate the coverage of only for-statement line or only for-body or both?
        SrcDstRange srcDstRange = new SrcDstRange();
        // while-statement
        srcDstRange.getSrcRange().add(RangeOperations.toLineRange(RangeOperations.toRange(action.getNode().getParent()),
                editScriptStorer.getDstCompilationUnit()));
        // while-body
        srcDstRange.getSrcRange().add(RangeOperations.toLineRange(RangeOperations.toRange(action.getNode()),
                editScriptStorer.getDstCompilationUnit()));

        //for-statement
        Tree forBodyNode = mappings.get(action.getNode());
        Tree forStatementNode = forBodyNode.getParent();
        srcDstRange.getSrcRange().add(RangeOperations.toLineRange(RangeOperations.toRange(forStatementNode),
                editScriptStorer.getDstCompilationUnit()));
        //while-body
        srcDstRange.getDstRange().add(RangeOperations.toLineRange(RangeOperations.toRange(forBodyNode),
                editScriptStorer.getDstCompilationUnit()));
        return srcDstRange;
    }
}
