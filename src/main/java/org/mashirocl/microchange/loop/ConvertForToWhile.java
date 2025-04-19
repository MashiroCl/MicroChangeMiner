package org.mashirocl.microchange.loop;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.mashirocl.editscript.EditScriptStorer;
import org.mashirocl.location.RangeOperations;
import org.mashirocl.microchange.MicroChangePattern;
import org.mashirocl.microchange.Position;
import org.mashirocl.microchange.SrcDstRange;

import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2025/04/17 13:39
 */
public class ConvertForToWhile implements MicroChangePattern {
    /**
     * The for-loop is replaced by a while-loop
     *
     * A child node (for-body) is moved from a for-statement to be the child of a while-statement,
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
                && mappings.get(action.getNode()).getParent().getType().name.equals("WhileStatement");
    }

    @Override
    public SrcDstRange getSrcDstRange(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        // being moved for-loop body
        // TODO: discussion: should we calculate the coverage  of only for-statement line or only for-body or both?
        SrcDstRange srcDstRange = new SrcDstRange();
        // for-statement
        srcDstRange.getSrcRange().add(RangeOperations.toLineRange(RangeOperations.toRange(action.getNode().getParent()),
                editScriptStorer.getDstCompilationUnit()));
        // for-body
//        srcDstRange.getSrcRange().add(RangeOperations.toLineRange(RangeOperations.toRange(action.getNode()),
//                editScriptStorer.getDstCompilationUnit()));

        //while-statement
        Tree whileBodyNode = mappings.get(action.getNode());
        Tree whileStatementNode = whileBodyNode.getParent();
        srcDstRange.getSrcRange().add(RangeOperations.toLineRange(RangeOperations.toRange(whileStatementNode),
                editScriptStorer.getDstCompilationUnit()));
        //while-body
//        srcDstRange.getDstRange().add(RangeOperations.toLineRange(RangeOperations.toRange(whileBodyNode),
//                editScriptStorer.getDstCompilationUnit()));
        return srcDstRange;
    }
}
