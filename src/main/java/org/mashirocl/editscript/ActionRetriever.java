package org.mashirocl.editscript;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/02/08 20:47
 */
public class ActionRetriever {
    public static List<Action> retrieveThroughNode(Tree node, EditScript editScript){
        List<Action> actions = new LinkedList<>();
        for(Action action: editScript){
            if(action.getNode().equals(node)){
                actions.add(action);
            }
        }
        return actions;
    }

    public static Map<Tree,List<Action>> retrieveMap(EditScript editScript){
        Map<Tree, List<Action>> map = new HashMap<>();
        for(Action action: editScript){
            Tree curNode = action.getNode();
            if(!map.containsKey(curNode)){
                map.put(curNode, new LinkedList<>());
            }
            map.get(curNode).add(action);
        }
        return map;
    }
}
