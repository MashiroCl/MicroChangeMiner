package org.mashirocl.match;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import org.mashirocl.microchange.MicroChange;
import org.mashirocl.microchange.MicroChangePattern;

import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/10 10:02
 */
public interface PatternMatcher {
    public void addMicroChange(MicroChangePattern microChangePattern);
    public List<MicroChange> match(Action action, Map<Tree, Tree> mapping);
}
