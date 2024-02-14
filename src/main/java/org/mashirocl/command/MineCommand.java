package org.mashirocl.command;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.diff.DiffFormatter;
import org.mashirocl.dao.MinedMicroChange;
import org.mashirocl.editscript.ActionRetriever;
import org.mashirocl.editscript.DiffEditScriptWithSource;
import org.mashirocl.editscript.EditScriptStorer;
import org.mashirocl.match.ActionLocator;
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
import java.util.stream.Collectors;

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

        Map<String, List<DiffEditScriptWithSource>> res = EditScriptExtractor.getEditScript(ra, diffFormatter);

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

//        TODO: Not finished
//        patternMatcherGumTree.addMicroChange(new ReplaceVariableWithExpression());

        List<MinedMicroChange> minedMicroChanges = new LinkedList<>();
        ActionLocator actionLocator = new ActionLocator();

        int count = 0;
        int numberMicroChangeContainedAction=0, numberTotalActionNumber = 0;
        int total_count = res.keySet().size();
        int totalCodeChangeLines = 0;
        int microChangeCoveredLines = 0;

        for (String commitID : res.keySet()) {
//            if(!commitID.contains("187d81c61e56ad369dfb2ebaa4909159a4087df5"))
//                continue;
            System.out.println(commitID);
            count++;
            log.info("Mining {}/{} {}...",count,total_count,commitID);
            for (DiffEditScriptWithSource diffEditScriptWithSource : res.get(commitID)) {
                EditScript editScript = diffEditScriptWithSource.getEditScript();
                EditScriptStorer editScriptStorer = diffEditScriptWithSource.getEditScriptStorer();
                Map<Tree, Tree> mappings = EditScriptExtractor.mappingStoreToMap(editScriptStorer.getMappingStore());
                Map<Tree, List<Action>> nodeActions = ActionRetriever.retrieveMap(editScript);

                //positions for all the change in a single file in a commit
                List<Position> changePositions = new LinkedList<>();
                //positions for micro-changes in a single file in a commit
                List<Position> microChangePositions = new LinkedList<>();

                for (Action a : editScript) {
//                    System.out.println("action number "+numberTotalActionNumber);
//                    System.out.println(a);
//                    System.out.println("parent");
//                    System.out.println(a.getNode().getParent());
//                    System.out.println("parent parent");
//                    System.out.println(a.getNode().getParent().getParent());
//                    System.out.println("parent action");
//                    for(Action aa:nodeActions.get(a.getNode().getParent())){
//                        System.out.println(aa);
//                    }
//                    System.out.println("parent parent action");
//                    for(Action aa:nodeActions.get(a.getNode().getParent().getParent().getChild(2))){
//                        System.out.println(aa);
//                    }
//                    break;
                    numberTotalActionNumber+=1;
                    //mine micro-changes
                    List<MicroChange> microChanges = patternMatcherGumTree.match(a, mappings, nodeActions, editScriptStorer);
                    //action location
                    changePositions.addAll(actionLocator.getLocation(a, editScriptStorer));
                    if(microChanges.isEmpty()) continue;
                    //micro-change locations
                    microChangePositions.addAll(microChanges.stream().flatMap(p->p.getPositions().stream()).toList());

                    minedMicroChanges.addAll(microChanges.stream()
                                            .map(p -> new MinedMicroChange(
                                                    repositoryName,
                                                    commitID,
                                                    diffEditScriptWithSource.getDiffEntry().getOldPath(),
                                                    diffEditScriptWithSource.getDiffEntry().getNewPath(),
                                                    p)).toList());
                    numberMicroChangeContainedAction+=1;

                }

//                List<Position> totalChangeScopes = actionLocator.scopeCalculate(changePositions);
//                List<Position> microChangeScopes = actionLocator.scopeCalculate(microChangePositions);
                if(!changePositions.isEmpty()) {
                    int codeChangeLines = actionLocator.coveredLines(changePositions);
                    totalCodeChangeLines += codeChangeLines;
                    log.info("# lines code changed: {}", codeChangeLines);
                }
                if(!microChangePositions.isEmpty()){
                    int actionChangeLines = actionLocator.coveredLines(microChangePositions);
                    microChangeCoveredLines+=actionChangeLines;
                    log.info("micro change covered lines: {}", actionChangeLines);
                }
            }
        }


        log.info("micro-change lines/number of total lines of code change: {}/{}="+String.format("%.4f", (float)microChangeCoveredLines/totalCodeChangeLines), microChangeCoveredLines, totalCodeChangeLines);
        log.info("micro-change contained action/number of total actions: {}/{}="+String.format("%.4f", (float)numberMicroChangeContainedAction/numberTotalActionNumber), numberMicroChangeContainedAction, numberTotalActionNumber);

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
