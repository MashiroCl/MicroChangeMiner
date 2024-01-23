package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;

import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/10 9:16
 */
public interface MicroChangePattern {
    boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings);

}
