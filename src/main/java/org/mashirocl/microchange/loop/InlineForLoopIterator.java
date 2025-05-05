package org.mashirocl.microchange.loop;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import org.mashirocl.editscript.EditScriptStorer;
import org.mashirocl.location.RangeOperations;
import org.mashirocl.microchange.MicroChangePattern;
import org.mashirocl.microchange.SrcDstRange;
import org.mashirocl.microchange.common.NodePosition;

import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2025/04/19 16:11
 */
public class InlineForLoopIterator implements MicroChangePattern {
    /**
     *
     * Inline for-loop variable, e.g. int i=0 in the for(int i=0;i<n;i++)
     *
     * A variable declaration/ initialization is moved from outside into the Initialization of a for loop header
     *
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
        return false;
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        // move-tree, the node is originally not the descendant of a loop header, and then it is moved into the loop header to be the initialization
        return action.getName().equals("move-tree")
                && NodePosition.isDescendantOfForLoopHeader(action.getNode())==null
                && mappings.containsKey(action.getNode())
                && NodePosition.isForLoopInitialization(mappings.get(action.getNode()));
    }

    @Override
    public SrcDstRange getSrcDstRange(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        SrcDstRange srcDstRange = new SrcDstRange();
        // src range, is out of for-statement
        srcDstRange.getSrcRange().add(RangeOperations.toLineRange(RangeOperations.toRange(action.getNode()),
                editScriptStorer.getSrcCompilationUnit()));

        //for-loop header
        Tree forLoopHeader = mappings.get(action.getNode());
        srcDstRange.getDstRange().add(RangeOperations.toLineRange(RangeOperations.toRange(forLoopHeader),
                editScriptStorer.getDstCompilationUnit()));
        return srcDstRange;
    }
}
