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
 * @since 2025/04/19 16:12
 */
public class ExtractForLoopIterator implements MicroChangePattern {
    /**
     *
     * Extract for-loop variable to outside of for-loop, e.g. int i=0 in the for(int i=0;i<n;i++)
     *
     *  A variable declaration/ initialization is moved from the Initialization of a for loop header to outside of it
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
        return false;
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        // move-tree, the node is originally the descendant of loop header, and then it is moved out of the loop header to be the 1st child
        return action.getName().equals("move-tree")
                && NodePosition.isDescendantOfForLoopHeader(action.getNode())!=null
                && mappings.containsKey(action.getNode())
                && !NodePosition.isForLoopInitialization(mappings.get(action.getNode()));
    }

    @Override
    public SrcDstRange getSrcDstRange(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        SrcDstRange srcDstRange = new SrcDstRange();
        // for-loop header
        srcDstRange.getSrcRange().add(RangeOperations.toLineRange(RangeOperations.toRange(action.getNode()),
                editScriptStorer.getSrcCompilationUnit()));

        //outside variable initialization
        srcDstRange.getDstRange().add(RangeOperations.toLineRange(RangeOperations.toRange(mappings.get(action.getNode())),
                editScriptStorer.getDstCompilationUnit()));
        return srcDstRange;
    }
}
