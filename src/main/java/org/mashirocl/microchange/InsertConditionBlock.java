package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import org.mashirocl.editscript.EditScriptStorer;
import org.mashirocl.location.RangeOperations;

import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/02/27 11:10
 */
public class InsertConditionBlock implements MicroChangePattern{
    /**
     * The if-block is purely added: e.g. https://github.com/bennidi/mbassador/commit/d6aa291b8662849033a1d8ec0772babd6e3ef166#diff-38069dbc75fc0de230b3daec833ab771c1461df4ebabffe06411e75d828b846fR48
     * condition:
     * for one action in the edit script
     * 1. is `delete-tree`
     * 2. action node is IfStatement
     * @return
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
        return action.getName().equals("insert-tree") && action.getNode().getType().name.equals("IfStatement");
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        return matchConditionGumTree(action, mappings);
    }

    @Override
    public SrcDstRange getSrcDstRange(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        SrcDstRange srcDstRange = new SrcDstRange();
        srcDstRange.getDstRange().add(RangeOperations.toLineRange(RangeOperations.toRange(action.getNode()),
                editScriptStorer.getDstCompilationUnit()));
        return srcDstRange;
    }
}
