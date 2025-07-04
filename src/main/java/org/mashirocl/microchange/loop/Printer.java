package org.mashirocl.microchange.loop;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;

import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2025/04/20 17:18
 */
public class Printer {
    public static void actionPrinter(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions){
        System.out.println("===============================");
        System.out.println("action name");
        System.out.println(action.getName());
        System.out.println("action node");
        System.out.println(action.getNode());
        System.out.println("action node label");
        System.out.println(action.getNode().getLabel());
        System.out.println("mapping action node");
        System.out.println(mappings.get(action.getNode()));
        System.out.println("action children");
        System.out.println(action.getNode().getChildren());
        System.out.println("action parent");
        System.out.println(action.getNode().getParent());
        System.out.println("action parent children");
        System.out.println(action.getNode().getParent().getChildren());
        System.out.println("action parent parent");
        System.out.println(action.getNode().getParent().getParent());
        System.out.println("action parent parent children");
        System.out.println(action.getNode().getParent().getParent().getChildren());
        System.out.println("action parent parent parent children");
        System.out.println(action.getNode().getParent().getParent().getParent().getChildren());
        System.out.println("action node label");
        System.out.println(action.getNode().getLabel());
        if(mappings.containsKey(action.getNode())){
            System.out.println("mapping action node");
            System.out.println(action.getNode());
        }
        if(mappings.containsKey(action.getNode().getParent())){
            System.out.println("mapping action parent's children node");
            System.out.println(mappings.get(action.getNode().getParent()).getChildren());
        }
    }
}
