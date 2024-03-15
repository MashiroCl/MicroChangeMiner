package org.mashirocl.microchange;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import org.mashirocl.editscript.EditScriptStorer;

import java.util.List;
import java.util.Map;


// TODO: unfinished design
//  how to use GumTree action to determine one element is a variable?
/**
 * @author mashirocl@gmail.com
 * @since 2024/02/06 10:09
 */
public class ReplaceVariableWithExpression implements MicroChangePattern{

    /**
     * condition:
     * for one action in the edit script
     * a variable is repalced by a method_invocation_receiver
     * what is a method_invocation_receiver,
     *
     * 1. is `update-node`
     * 2. action.getNode.getType is SimpleName
     * 3. action.getNode.getParent is not METHOD_INVOCATION_RECEIVER
     * 4. mappings(action.getNode) getParent is ""METHOD_INVOCATION_RECEIVER""
     */
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
        if(action.getName().equals("update-node")){
            if(mappings.containsKey(action.getNode())){
                System.out.println(action);
                System.out.println("*****parent type****");
                System.out.println(action.getNode().getParent().getType().toString());
                System.out.println("******mappings parent parent*******");
                System.out.println(mappings.get(action.getNode()).getParent().getType());
                System.out.println(mappings.get(action.getNode()).getParent().getParent().toString());
                System.out.println("miaomiaomiaomiaomiaomiaomiaomiao");
                System.out.println(action.getNode().getParent().toTreeString());
                System.out.println(mappings.get(action.getNode()).getParent().getParent().toTreeString());
                mappings.get(action.getNode()).getParent().getParent().getChild(0);

            }
        }

        return action.getName().equals("update-node")
                && action.getNode().getType().toString().contains("SimpleName")
                && !action.getNode().getParent().getType().toString().equals("METHOD_INVOCATION_RECEIVER")
                && !action.getNode().getParent().getType().toString().equals("MethodInvocation")
                && !action.getNode().getParent().getType().toString().equals("ArrayAccess")
                && mappings.containsKey(action.getNode())
                && mappings.get(action.getNode()).getParent().getType().toString().equals("METHOD_INVOCATION_RECEIVER");
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
        return false;
    }

    @Override
    public SrcDstRange getSrcDstRange(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        return null;
    }

}
