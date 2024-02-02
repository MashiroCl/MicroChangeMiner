package org.mashirocl.command;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.diff.DiffFormatter;
import org.mashirocl.dao.MinedMicroChange;
import org.mashirocl.editscript.DiffEditScriptMapping;
import org.mashirocl.match.PatternMatcher;
import org.mashirocl.match.PatternMatcherGumTree;
import org.mashirocl.microchange.*;
import org.mashirocl.editscript.EditScriptExtractor;
import org.mashirocl.util.CommitMapper;
import org.mashirocl.util.LinkAttacher;
import org.mashirocl.util.MicroChangeWriter;
import org.mashirocl.util.RepositoryAccess;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.net.URL;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/17 16:08
 */

@Slf4j
@Command(name = "mine", description = "Match micro-patterns for a repository")
public class MineCommand implements Callable<Integer> {

    public static class Config {
        @CommandLine.Parameters(index = "0", description = "Mining micro-changes in this repository")
        String repositoryPath;

        @CommandLine.Parameters(index = "1", description = "Output mining result in json file")
        String outputPath;

        @Option(names = {"-c", "--csv"}, description = "Output mining result to csv")
        String csvPath;

        @Option(names = {"--map"}, description = "Convert the method-level commit hash to original hash")
        String commitMap;
    }

    @CommandLine.Mixin
    protected Config config = new Config();

    @Override
    public Integer call() throws Exception {
        log.info("start mining...");
        final RepositoryAccess ra = new RepositoryAccess(Path.of(config.repositoryPath));
        final String repositoryName = Path.of(config.repositoryPath).getParent().getFileName().toString();

        final DiffFormatter diffFormatter = new DiffFormatter(System.out);
        diffFormatter.setRepository(ra.getRepository());

        Map<String, List<DiffEditScriptMapping>> res = EditScriptExtractor.getEditScript(ra, diffFormatter);

        PatternMatcher patternMatcherGumTree = new PatternMatcherGumTree();
        patternMatcherGumTree.addMicroChange(new ReverseThenElse());
        patternMatcherGumTree.addMicroChange(new ExtendIfWithElse());
        patternMatcherGumTree.addMicroChange(new ExtendElseWithIf());
        patternMatcherGumTree.addMicroChange(new SemanticallySameConditionUpdate());
        patternMatcherGumTree.addMicroChange(new ConditionalToBooleanReturn());
        patternMatcherGumTree.addMicroChange(new ConditionalToSwitch());
        patternMatcherGumTree.addMicroChange(new ReverseConditional());
        patternMatcherGumTree.addMicroChange(new ConditionalToTernaryOperator());
        patternMatcherGumTree.addMicroChange(new RemoveElse());
        patternMatcherGumTree.addMicroChange(new UnifyCondition());
        patternMatcherGumTree.addMicroChange(new DeleteConditionals());
        patternMatcherGumTree.addMicroChange(new ParallelCondition());
        patternMatcherGumTree.addMicroChange(new ChangeBoundaryCondition());

        List<MinedMicroChange> minedMicroChanges = new LinkedList<>();

        int count = 0;
        int total_count = res.keySet().size();
        for (String commitID : res.keySet()) {
//            if(!commitID.contains("b0c9ecf93170a73054eeda3c5623d2b6dffb1db8")) continue;
//            System.out.println(commitID);
            count++;
            log.info("Mining {}/{} {}...",count,total_count,commitID);
            for (DiffEditScriptMapping diffEditScript : res.get(commitID)) {
                EditScript editScript = diffEditScript.getDiffEditScript().getEditScript();
                Map<Tree, Tree> mappings = EditScriptExtractor.mappingStoreToMap(diffEditScript.getEditScriptMapping().getMappingStore());
                for (Action a : editScript) {
                    List<MicroChange> microChanges = patternMatcherGumTree.match(a, mappings);
                    if(microChanges.isEmpty()) continue;
                    minedMicroChanges.addAll(microChanges.stream()
                                            .map(p -> new MinedMicroChange(
                                                    repositoryName,
                                                    commitID,
                                                    diffEditScript.getDiffEditScript().getDiffEntry().getOldPath(),
                                                    diffEditScript.getDiffEditScript().getDiffEntry().getNewPath(),
                                                    p)).toList());
                }
            }
        }

        if(config.commitMap!=null){
            log.info("Converting method-level commit hash to original hash according to {}", config.commitMap);
            CommitMapper commitMapper = new CommitMapper(config.commitMap);
            minedMicroChanges.forEach(p->p.setCommitID(commitMapper.getMap().get((p.getCommitID()))));
        }

        MicroChangeWriter.writeJson(minedMicroChanges, config.outputPath);

        if(config.csvPath!=null){
            MicroChangeWriter.writeCsv(config.outputPath, config.csvPath, new URL(LinkAttacher.searchLink(repositoryName)));
        }
//        minedMicroChanges.forEach(System.out::println);
        return 0;
    }
}
