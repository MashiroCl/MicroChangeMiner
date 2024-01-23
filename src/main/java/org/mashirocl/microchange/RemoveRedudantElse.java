package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.tree.Tree;

import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/19 15:19
 */
public class RemoveRedudantElse {

    /**
     * condition:
     * for one action in the edit script
     * TODO design
     * @param action
     * @param mappings
     * @return
     */
    public boolean matchConditionGumTree2(Action action, Map<Tree, Tree> mappings){
        if(!action.getName().equals("move-tree"))
            return false;
        return false;
    }
}
