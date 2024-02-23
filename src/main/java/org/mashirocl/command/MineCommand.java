package org.mashirocl.command;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.mashirocl.dao.MinedMicroChange;
import org.mashirocl.editscript.ActionRetriever;
import org.mashirocl.editscript.DiffEditScriptWithSource;
import org.mashirocl.editscript.EditScriptStorer;
import org.mashirocl.match.ActionLocator;
import org.mashirocl.match.ActionStatus;
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
        log.info("Edit Script obtained for {} commits", res.size());

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
//        int totalCodeChangeLines = 0;
//        int microChangeCoveredLines = 0;
        int [] totalADCodeChangeLines = new int [] {0,0};
        int [] microADChangeCoveredLines = new int [] {0,0};
        int [] textDiffLines = new int[] {0,0};

        for (String commitID : res.keySet()) {
//            if(
//                    !commitID.contains("260195e1011b507911c113ac3ce6348ddfe22340")&&
//                    !commitID.contains("f5a6f7492bc7df0edad219174c98adcfffd58092")&&
//            !commitID.contains("5f3a5140815195faed7296e4d8bc24b00943cbf8")&&
//            !commitID.contains("7fadb84cb7d87d409194ea389f68ca65fa633319")&&
//            !commitID.contains("616c17334c200831a752915ceb1ff3bc4633c9f3")&&
//            !commitID.contains("e4b5f990962a323075b80e099aa22b0694ebd73a")&&
//            !commitID.contains("3a01485a88cf57d1907385d6715e0fce8ed8a785")&&
//            !commitID.contains("4c9d7f7acf4ac1eaf6fdbfdf09a2e6da226e36b1")&&
//                            !commitID.contains("28338288ae249cc65da377500a2ac7cd83eede12")&&
//                            !commitID.contains("4a0541a2cb0bdeb985074700e26045528c32fc75")&&
//                            !commitID.contains("c83527d229a0441780e0dcb6c8230151bbd4133e")&&
//                            !commitID.contains("e531f9a846917c5d93b52d0aacd608070f68a324")&&
//                            !commitID.contains("d2830a3ea04c4d51bc1f89885008dd2cce47074b")&&
//                            !commitID.contains("187d81c61e56ad369dfb2ebaa4909159a4087df5")&&
//                            !commitID.contains("0e2f408c9cae1451bc5d4c7834769eed8de5daf9")&&
//                            !commitID.contains("6d8b71cc9688ba25292c4b31b7f1c1aa389106ba")&&
//                            !commitID.contains("07abc73a54c609c38c7339ceec0951f1c6666b6e")&&
//                            !commitID.contains("b75f836abba5529af3d2d60c9fb52766f8c5c901")
//                            !commitID.contains("2b6ce3832335bacf9f556e3ead8f9d090d382208")
//                            !commitID.contains("46003fcb58ea0d89062fcad3ac54e4820e0ff66c")
//                            !commitID.contains("7bcc5ef98b324a0ac0c267b2b7bc4d795ed13628")

//            )
//                continue;
//            System.out.println(commitID);
            count++;
            log.info("Mining {}/{} {}...",count,total_count,commitID);
            log.info("DiffEditScriptWithSource {}", res.get(commitID));
            for (DiffEditScriptWithSource diffEditScriptWithSource : res.get(commitID)) {
                // 1 EditScript is 1 file?->yes
                EditScript editScript = diffEditScriptWithSource.getEditScript();
                EditScriptStorer editScriptStorer = diffEditScriptWithSource.getEditScriptStorer();
                Map<Tree, Tree> mappings = EditScriptExtractor.mappingStoreToMap(editScriptStorer.getMappingStore());
                Map<Tree, List<Action>> nodeActions = ActionRetriever.retrieveMap(editScript);

                System.out.println(editScriptStorer.getChangedLines());

                //positions for all the change in a single file in a commit
//                List<Position> changePositions = new LinkedList<>();
//                SeperatedPosition changePositions = new SeperatedPosition(new LinkedList<>(), new LinkedList<>());
                RangeSet<Integer> treeActionAdditionRange = TreeRangeSet.create();
                RangeSet<Integer> treeActionDeletionRange = TreeRangeSet.create();
                RangeSet<Integer> treeActionMicroChangeAdditionRange = TreeRangeSet.create();
                RangeSet<Integer> treeActionMicroChangeDeletionRange = TreeRangeSet.create();

                //positions for micro-changes in a single file in a commit
                List<Position> microChangePositions = new LinkedList<>();

                log.info("# of actions {}", editScript.size());
                for (Action a : editScript) {

                    // action node is the descendant of ifstatement or itself is ifstatement
//                    if(!ActionStatus.isDescendantOfIfStatement(a)) continue;
//                    log.info("tree node for action is the descendant of if {}", a.getNode().toTreeString());

                    // action node is the child of ifstatement or itself is ifstatement
                    if(!ActionStatus.isChildOfIfStatement(a)) continue;
                    log.info("tree for action is the child of if {}", a.getNode().toTreeString());

                    numberTotalActionNumber+=1;

                    //mine micro-changes
                    List<MicroChange> microChanges = patternMatcherGumTree.match(a, mappings, nodeActions, editScriptStorer);


                    //action location
//                    changePositions.addAll(actionLocator.getLocation(a, editScriptStorer));
//                    SeperatedPosition tempPositions = actionLocator.getSeperatedLocations(a, mappings, editScriptStorer);
                    SrcDstRange treeActionRanges =  actionLocator.getRanges(a, mappings, editScriptStorer);
                    treeActionDeletionRange.addAll(treeActionRanges.getSrcRange());
                    treeActionAdditionRange.addAll(treeActionRanges.getDstRange());
//                    System.out.println("ranges:");
//                    System.out.println(treeActionRanges);
//                    System.out.printf("treeActionDeletionRange: %s\n", treeActionDeletionRange);
//                    System.out.printf("treeActionAdditionRange: %s\n", treeActionAdditionRange);


//                    changePositions.addPositions(tempPositions.getSrcPositions(), tempPositions.getDstPositions());



                    if(microChanges.isEmpty()) continue;
                    //micro-change covered locations (lines)
                    for(MicroChange microChange:microChanges){
                        treeActionMicroChangeDeletionRange.addAll(microChange.getSrcDstRange().getSrcRange());
                        treeActionMicroChangeAdditionRange.addAll(microChange.getSrcDstRange().getDstRange());
                    }
//                    microChangePositions.addAll(microChanges.stream().flatMap(p->p.getPositions().stream()).toList());
//
                    minedMicroChanges.addAll(microChanges.stream()
                                            .map(p -> new MinedMicroChange(
                                                    repositoryName,
                                                    commitID,
                                                    diffEditScriptWithSource.getDiffEntry().getOldPath(),
                                                    diffEditScriptWithSource.getDiffEntry().getNewPath(),
                                                    p.getType(),
                                                    p.getAction(),
                                                    p.getSrcDstRange().toString())).toList());
                    numberMicroChangeContainedAction+=1;

                }

                SrcDstRange textRange =  editScriptStorer.getChangedLines();
                int textRemoved = coveredLength(textRange.getSrcRange());
                int textAdded = coveredLength(textRange.getDstRange());

                log.info("textRange: {}", textRange);
                log.info("text removed: {}", textRemoved);
                log.info("text added: {}", textAdded);

                // Some purely addition/deletion are regarded as Modify-Change-Type by jgit
                if(textRemoved>0 && textAdded>0) {
                    textDiffLines[0] += textRemoved;
                    textDiffLines[1] += textAdded;
                }

                if(!treeActionDeletionRange.isEmpty()){
                    //intersection of text range and tree range
                    treeActionDeletionRange.removeAll(textRange.getSrcRange().complement());
                    log.info("treeActionDeletionRange: lines deleted {}", treeActionDeletionRange);
                    totalADCodeChangeLines[0] += coveredLength(treeActionDeletionRange);
                }
                if(!treeActionAdditionRange.isEmpty()){
                    //intersection of text range and tree range
                    treeActionAdditionRange.removeAll(textRange.getDstRange().complement());
                    log.info("treeActionAdditionRange: lines added {}", treeActionAdditionRange);
                    totalADCodeChangeLines[1] += coveredLength(treeActionAdditionRange);
                }


//                if(!changePositions.isEmpty()) {
////                    int codeChangeLines = actionLocator.coveredLines(changePositions);
//                    int [] codeChangeLines = actionLocator.coveredLines(changePositions);
//                    totalADCodeChangeLines[0] += codeChangeLines[0];
//                    totalADCodeChangeLines[1] += codeChangeLines[1];
//                    log.info("# lines code changed: {}", codeChangeLines);
//                    log.info("changed lines' positions: {}", changePositions);
//                }
//
                if(!treeActionMicroChangeDeletionRange.isEmpty()){
                    //intersection of text range and tree range
                    treeActionMicroChangeDeletionRange.removeAll(textRange.getSrcRange().complement());
                    log.info("treeActionMicroChangeDeletionRange: micro-change covered deleted lines {}", treeActionMicroChangeDeletionRange);
                    microADChangeCoveredLines[0] += coveredLength(treeActionMicroChangeDeletionRange);
                }

                if(!treeActionMicroChangeAdditionRange.isEmpty()){
                    //intersection of text range and tree range
                    treeActionMicroChangeAdditionRange.removeAll(textRange.getDstRange().complement());
                    log.info("treeActionMicroChangeAdditionRange: micro-change covered added lines {}", treeActionMicroChangeAdditionRange);
                    microADChangeCoveredLines[1] += coveredLength(treeActionMicroChangeAdditionRange);
                }

            }
        }


        log.info("total tree removed lines: {}", totalADCodeChangeLines[0]);
        log.info("total tree added lines: {}", totalADCodeChangeLines[1]);

        log.info("total text removed lines: {}", textDiffLines[0]);
        log.info("total text added lines: {}", textDiffLines[1]);

        log.info("removed lines covered by micro-change: {}", microADChangeCoveredLines[0]);
        log.info("added lines covered by micro-change: {}", microADChangeCoveredLines[1]);

        log.info("micro-change covered added lines/number of total lines of tree code added: {}/{}="+String.format("%.4f", (float)microADChangeCoveredLines[0]/totalADCodeChangeLines[0]), microADChangeCoveredLines[0], totalADCodeChangeLines[0]);
        log.info("micro-change covered deleted lines/number of total lines of tree code delted: {}/{}="+String.format("%.4f", (float)microADChangeCoveredLines[1]/totalADCodeChangeLines[1]), microADChangeCoveredLines[1], totalADCodeChangeLines[1]);


//        log.info("micro-change lines/number of total lines of code change: {}/{}="+String.format("%.4f", (float)microChangeCoveredLines/totalCodeChangeLines), microChangeCoveredLines, totalCodeChangeLines);

        log.info("Total number of actions: {}", numberTotalActionNumber);
        log.info("Micro-change contained actions: {}", numberMicroChangeContainedAction);
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

    public static int coveredLength(RangeSet<Integer> rangeSet){
        int coveredLength = 0;
        for (Range<Integer> range : rangeSet.asRanges()) {
            int length = 0;
            if (range.hasLowerBound() && range.hasUpperBound()) {
                // Calculate the basic length
                length = range.upperEndpoint() - range.lowerEndpoint() - 1; // Subtract 1 because we start assuming both ends are open

                // Adjust based on the inclusivity of the endpoints
                if (range.lowerBoundType() == BoundType.CLOSED) {
                    length += 1; // Include the lower endpoint if it's closed
                }
                if (range.upperBoundType() == BoundType.CLOSED) {
                    length += 1; // Include the upper endpoint if it's closed
                }
            }
            coveredLength += length;
        }
        return coveredLength;
    }

}
