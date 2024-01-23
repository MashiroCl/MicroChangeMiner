package org.mashirocl.util;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.Tree;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.junit.jupiter.api.Test;
import org.mashirocl.match.PatternMatcherGumTree;
import org.mashirocl.match.PatternMatcher;
import org.mashirocl.microchange.MicroChangePattern;
import org.mashirocl.microchange.ReverseThenElse;
import org.mashirocl.source.SourcePair;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/08 15:34
 */
class RepositoryAccessTest {

    private final String TestRepositoryPath = "/Users/leichen/data/OSS/my-refactoring-toy-example/.git";
    private final RepositoryAccess ra = new RepositoryAccess(Path.of(TestRepositoryPath));

    private final String endCommitID = "2bab7ef422f5a211224dc303d800068abe93e448";
    private final String startCommitID = "cb98b3ede192736b48b18c4bf3b824758b6c4f7a";

    private final DiffFormatter diffFormatter = new DiffFormatter(System.out);

    @Test
    void walk() throws IOException {
        diffFormatter.setRepository(ra.getRepository());
        Iterable<RevCommit> walk = ra.walk(startCommitID, endCommitID);
        for(RevCommit commit:walk){
           RevTree fromTree = commit.getTree();
           RevTree toTree = commit.getParent(0).getTree();
           List<DiffEntry> diffEntryList = diffFormatter.scan(toTree,fromTree);
           String filePath = diffEntryList.get(0).getOldPath();


           for(DiffEntry diffEntry:diffEntryList){
               System.out.println(diffEntry.toString());
               System.out.println(diffEntry.getChangeType());
               System.out.println(diffEntry.getOldPath());
               System.out.println(diffEntry.getNewPath());
               System.out.println(getSource(filePath, fromTree, ra.getRepository()));
           }
        }

    }

    void printAction(Action action){
        System.out.printf("action\n %s\n", action);
        System.out.printf("node\n %s\n",action.getNode().toString());
        System.out.printf("parent\n %s\n", action.getNode().getParent().toString());
        System.out.printf("child\n %s\n", action.getNode().getChild(0));
    }

    @Test
    void computingEditScript() throws IOException{
//        Run.initClients();
        String srcFile = "/Users/leichen/data/OSS/my-refactoring-toy-example/app/src/main/java/code/1493a015ea8e50d654539f67eca6bbd027d74a26.txt";
        String dstFile = "/Users/leichen/data/OSS/my-refactoring-toy-example/app/src/main/java/code/9f802465c0eed0ca92c3c3e048f79c2ebc6cba08.txt";
//        srcFile = "/Users/leichen/data/OSS/mbassador/017f52a3c9593e6294db4059eb643a8e07de9440.java";
//        dstFile = "/Users/leichen/data/OSS/mbassador/17b1fee1b6b44612588e711aa0279c511c9d8d3a.java";
//        Tree src = TreeGenerators.getInstance().getTree(srcFile).getRoot();
//        Tree dst = TreeGenerators.getInstance().getTree(dstFile).getRoot();
        Tree src = new JdtTreeGenerator().generateFrom().string(new String(Files.readAllBytes(Path.of(srcFile)))).getRoot();
        Tree dst = new JdtTreeGenerator().generateFrom().string(new String(Files.readAllBytes(Path.of(dstFile)))).getRoot();
        Matcher defaultMatcher = Matchers.getInstance().getMatcher();
        MappingStore mappings = defaultMatcher.match(src, dst);

        EditScriptGenerator editScriptGenerator = new SimplifiedChawatheScriptGenerator();
        EditScript actions = editScriptGenerator.computeActions(mappings);

//        for(int i=0;i<actions.asList().size();i++){
//            System.out.printf("Index %d\n",i);
//            System.out.println(actions.asList().get(i));
//        }


//        System.out.println("---mbassador------c31467da32150ea54b6eafa90f1973ef6ad0345c----------");
//        printAction(actions.asList().get(313));
//        for (Iterator<Mapping> it = mappings.iterator(); it.hasNext(); ) {
//            Mapping cur = it.next();
//            if(cur.first.equals(actions.asList().get(313).getNode())){
//                System.out.println(cur.first.getParent());
//                System.out.println(cur.second.getParent().toTreeString());
//                System.out.println("node found");
//            }
//        }
//        System.out.println("-------------------------------------------------------------------------------");
//
        System.out.println("--------------------------------Second edit------------------------------------");
        printAction(actions.asList().get(0));
        HashMap<Tree,Tree> map = new HashMap<>();
        for (Mapping cur : mappings) {
            map.put(cur.first, cur.second);
        }

        Tree node = actions.asList().get(0).getNode();
        if(map.containsKey(node)){
            System.out.println("node found");
            System.out.println("first");
            System.out.println(node.getParent());
            System.out.println("second");
            System.out.println(map.get(node).getParent());
        }
        System.out.println("-------------------------------------------------------------------------------");

//        System.out.println("--------------------------------Third edit------------------------------------");
//        printAction(actions.asList().get(2));
//        System.out.println("-------------------------------------------------------------------------------");
    }

    @Test
    void testEditScriptOnTwoMethodLevelFiles() throws IOException{
        String srcFile = "/Users/leichen/project/semantic_lifter/SemanticLifter/mbassdor_fp_method_level_test/fp_3/before.java";
        String dstFile = "/Users/leichen/project/semantic_lifter/SemanticLifter/mbassdor_fp_method_level_test/fp_3/after.java";
//        Tree src = TreeGenerators.getInstance().getTree(srcFile).getRoot();
//        Tree dst = TreeGenerators.getInstance().getTree(dstFile).getRoot();
        Tree src = new JdtTreeGenerator().generateFrom().string(new String(Files.readAllBytes(Path.of(srcFile)))).getRoot();
        Tree dst = new JdtTreeGenerator().generateFrom().string(new String(Files.readAllBytes(Path.of(dstFile)))).getRoot();
        Matcher defaultMatcher = Matchers.getInstance().getMatcher();
        MappingStore mappings = defaultMatcher.match(src, dst);
        EditScriptGenerator editScriptGenerator = new SimplifiedChawatheScriptGenerator();
        EditScript actions = editScriptGenerator.computeActions(mappings);

        MicroChangePattern reverseConditional = new ReverseThenElse();
        PatternMatcher patternMatcher = new PatternMatcherGumTree();
        patternMatcher.addMicroChange(reverseConditional);

        for(Action a:actions){
            patternMatcher.match(a, SourcePair.getMappings(mappings));
        }
    }

    @Test
    void testInverseIfConditionEditListContainsTwoMove() throws IOException{
        String dstFile = "/Users/leichen/data/OSS/my-refactoring-toy-example/app/src/main/java/animal/52e398e_Condition.java";
        String srcFile = "/Users/leichen/data/OSS/my-refactoring-toy-example/app/src/main/java/animal/6b1b567_Condition.java";
        Tree src = new JdtTreeGenerator().generateFrom().string(new String(Files.readAllBytes(Path.of(srcFile)))).getRoot();
        Tree dst = new JdtTreeGenerator().generateFrom().string(new String(Files.readAllBytes(Path.of(dstFile)))).getRoot();
        Matcher defaultMatcher = Matchers.getInstance().getMatcher();
        MappingStore mappings = defaultMatcher.match(src, dst);
        for(Tree node: src.breadthFirst()){
            if(Objects.equals(node.getType().toString(), "IfStatement")){
                System.out.println(node.getChildren().size());
                System.out.println(node.getChild(0));
                System.out.println(node.getChild(1));
                System.out.println(node.getChild(2));
                break;
            }
        }

        EditScriptGenerator editScriptGenerator = new SimplifiedChawatheScriptGenerator();
        EditScript actions = editScriptGenerator.computeActions(mappings);

//        int count = 1;
//        for(Action action: actions.asList()){
//            System.out.printf("--------------------------------Edit %d ------------------------------------", count);
//            System.out.printf("action\n %s\n", action);
//            System.out.printf("node\n %s\n",action.getNode().toString());
//            System.out.printf("parent\n %s\n", action.getNode().getParent());
//            System.out.printf("child\n %s\n", action.getNode().getChildren().size());
//            count++;
//        }

//        //move Then block
//        System.out.println("--------------------------------First edit------------------------------------");
//        System.out.printf("action\n %s\n", actions.asList().get(0));
//        System.out.printf("node\n %s\n",actions.asList().get(0).getNode().toString());
//        System.out.printf("parent\n %s\n", actions.asList().get(0).getNode().getParent());
//        System.out.printf("child\n %s\n", actions.asList().get(0).getNode().getChild(0));
//        System.out.println("-------------------------------------------------------------------------------");
    }

    private String getSource(String path, RevTree tree, Repository repo) {
        TreeWalk treeWalk = new TreeWalk(repo);
        String source = "";
        try {
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            treeWalk.setFilter(PathFilter.create(path));
            if(treeWalk.next()) {
                source = new String(repo.open(treeWalk.getObjectId(0))
                        .getBytes(), StandardCharsets.UTF_8);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        treeWalk.close();
        return source;
    }
}