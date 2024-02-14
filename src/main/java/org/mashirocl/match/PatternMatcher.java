package org.mashirocl.match;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import org.mashirocl.editscript.EditScriptStorer;
import org.mashirocl.microchange.MicroChange;
import org.mashirocl.microchange.MicroChangePattern;

import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/10 10:02
 */
public interface PatternMatcher {
    void addMicroChange(MicroChangePattern microChangePattern);
   List<MicroChange> match(Action action, Map<Tree, Tree> mapping);

    List<MicroChange> match(Action action, Map<Tree, Tree> mapping, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer);
}
