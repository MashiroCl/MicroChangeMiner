package org.mashirocl.match;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import org.mashirocl.microchange.MicroChange;
import org.mashirocl.microchange.MicroChangePattern;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/10 10:03
 */
public class PatternMatcherGumTree implements PatternMatcher {

    private final List<MicroChangePattern> microChangePatternList = new LinkedList<>();

    public void addMicroChange(MicroChangePattern microChangePattern){
        microChangePatternList.add(microChangePattern);
    }

    public void addMicroChanges(Stream<MicroChangePattern> microChangeStream){
        ;
    }

//    @Override
//    public MicroChangePattern match(Action action) {
//        for(MicroChangePattern pattern: microChangePatternList){
//            if(pattern.matchConditionGumTree(action)){
//                System.out.println("Match found with pattern: "+pattern.getClass().getSimpleName());
//                System.out.println(action);
//            }
//        }
//
//        return null;
//    }

    @Override
    public List<MicroChange> match(Action action, Map<Tree, Tree> mappings) {
        List<MicroChange> microChanges = new LinkedList<>();
        for(MicroChangePattern pattern: microChangePatternList){
            if(pattern.matchConditionGumTree(action, mappings)){
                microChanges.add(MicroChange.of(pattern.getClass().getSimpleName(),
                        action.toString()));
                System.out.println("Match found with pattern: "+pattern.getClass().getSimpleName());
//                System.out.println(action);
            }
        }
        return microChanges;
    }

}
