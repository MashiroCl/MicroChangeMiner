package org.mashirocl;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import org.eclipse.jgit.diff.DiffFormatter;
import org.mashirocl.editscript.DiffEditScriptMapping;
import org.mashirocl.match.PatternMatcher;
import org.mashirocl.match.PatternMatcherGumTree;
import org.mashirocl.microchange.MicroChangePattern;
import org.mashirocl.microchange.ReverseThenElse;
import org.mashirocl.editscript.EditScriptExtractor;
import org.mashirocl.util.RepositoryAccess;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/08 9:40
 */
public class Main {
    public static void main(String[] args){
        final String testRepositoryPath = "/Users/leichen/data/OSS/my-refactoring-toy-example/.git";
//        final String TestRepositoryPresto = "/Users/leichen/data/OSS/presto/.git";
        final String mbassadorPath = "/Users/leichen/data/OSS/mbassador/.git";
        final String mbassadorMethodLevelPath = "/Users/leichen/data/parsable_method_level/mbassador/.git";

//        final RepositoryAccess ra = new RepositoryAccess(Path.of(mbassadorMethodLevelPath));
        final RepositoryAccess ra = new RepositoryAccess(Path.of(testRepositoryPath));

        final DiffFormatter diffFormatter = new DiffFormatter(System.out);
        diffFormatter.setRepository(ra.getRepository());

        Map<String, List<DiffEditScriptMapping>> res = EditScriptExtractor.getEditScript(ra, diffFormatter);

//        res.replaceAll(
//                (c, v) -> res.get(c)
//                        .stream()
//                        .filter(diffEditScript -> diffEditScript.getDiffEntry().getChangeType().equals(DiffEntry.ChangeType.MODIFY))
//                        .collect(Collectors.toList()));

        MicroChangePattern reverseConditional = new ReverseThenElse();
        PatternMatcher patternMatcherGumTree = new PatternMatcherGumTree();
        patternMatcherGumTree.addMicroChange(reverseConditional);

//        for(String commitID: res.keySet()){
//            System.out.printf("commitID %s\n", commitID);
//            for(DiffEditScriptMapping diffEditScript: res.get(commitID)){
//                EditScript editScript = diffEditScript.getDiffEditScript().getEditScript();
//                Map<Tree, Tree> mappings = diffEditScript.getMappings(EditScriptExtractor.defaultMatcher);
//                    for(Action a:editScript){
//                        patternMatcherGumTree.match(a, mappings);
//                    }
//
//            }
//        }

        for(String commitID: res.keySet()){
            System.out.printf("commitID %s\n", commitID);
//            if(!commitID.contains("b0c9ecf93170a73054eeda3c5623d2b6dffb1db8")) continue;
            for(DiffEditScriptMapping diffEditScript: res.get(commitID)){
                EditScript editScript = diffEditScript.getDiffEditScript().getEditScript();
                Map<Tree, Tree> mappings = EditScriptExtractor.mappingStoreToMap(diffEditScript.getEditScriptMapping().getMappingStore());
                for(Action a:editScript){
                    patternMatcherGumTree.match(a, mappings);
                }

            }
        }

    }

}