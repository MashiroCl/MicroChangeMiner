package org.mashirocl.match;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import lombok.extern.slf4j.Slf4j;
import org.mashirocl.editscript.EditScriptStorer;
import org.mashirocl.microchange.MicroChange;
import org.mashirocl.microchange.MicroChangePattern;
import org.mashirocl.microchange.SrcDstRange;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/10 10:03
 */
@Slf4j
public class PatternMatcherGumTree implements PatternMatcher {

    private final List<MicroChangePattern> microChangePatternList = new LinkedList<>();

    public void addMicroChange(MicroChangePattern microChangePattern){
        microChangePatternList.add(microChangePattern);
    }

    public void addMicroChanges(Stream<MicroChangePattern> microChangeStream){
        ;
    }

    @Override
    public List<MicroChange> match(Action action, Map<Tree, Tree> mappings) {
        List<MicroChange> microChanges = new LinkedList<>();
        for(MicroChangePattern pattern: microChangePatternList){
            if(pattern.matchConditionGumTree(action, mappings)){
                microChanges.add(MicroChange.of(pattern.getClass().getSimpleName(),
                        action.toString()));
                log.info("Match found with pattern: {}",pattern.getClass().getSimpleName());
//                System.out.println(action);
            }
        }
        return microChanges;
    }


    public List<MicroChange> match(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        List<MicroChange> microChanges = new LinkedList<>();
        for(MicroChangePattern pattern: microChangePatternList){
            if(pattern.matchConditionGumTree(action, mappings, nodeActions)){
                SrcDstRange location = pattern.getSrcDstRange(action, mappings, nodeActions, editScriptStorer);
                microChanges.add(
                        MicroChange.of(
                                pattern.getClass().getSimpleName(),
                                action.toString(),
//                                pattern.getPosition(action, mappings, nodeActions, editScriptStorer)
                               location
                        ));
                log.info("Match found with pattern: {}, locations: {}",pattern.getClass().getSimpleName(), location);
            }
        }
        return microChanges;
    }




//    public void loadAllMicroChanges(){
//        microChangePatternList.add();
//    }

}
