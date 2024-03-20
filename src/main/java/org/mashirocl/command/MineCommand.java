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
import org.mashirocl.dao.*;
import org.mashirocl.editscript.*;
import org.mashirocl.match.ActionLocator;
import org.mashirocl.match.ActionStatus;
import org.mashirocl.match.PatternMatcher;
import org.mashirocl.match.PatternMatcherGumTree;
import org.mashirocl.microchange.*;
import org.mashirocl.refactoringminer.MethodLevelConvertor;
import org.mashirocl.refactoringminer.Refactoring;
import org.mashirocl.refactoringminer.RefactoringLoader;
import org.mashirocl.refactoringminer.SideLocation;
import org.mashirocl.util.CSVWriter;
import org.mashirocl.util.CommitMapper;
import org.mashirocl.util.LinkAttacher;
import org.mashirocl.util.RepositoryAccess;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
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
        String methodLevelGitPath;

        @CommandLine.Parameters(index = "1", description = "Output mining result in json file")
        String outputJsonPath;

        @Option(names = {"-c", "--csv"}, description = "Output mining result to csv")
        String outputCsvPath;

        @Option(names = {"--map"}, description = "Convert the method-level commit hash to original hash")
        String commitMap;

        @Option(names = {"--refactoring"}, description = "the RM result path for the repository")
        String refactoringPath;

        @Option(names = {"--original"}, description = "the original file-level repository git path")
        String originalRepoGitPath;

//        @Option(names = {"--brief"}, description = "the brief output")
//        boolean brief;
    }

    @CommandLine.Mixin
    protected Config config = new Config();


    public static void loadMicroChanges(PatternMatcher patternMatcherGumTree) {
        patternMatcherGumTree.addMicroChange(new ReverseThenElse());
        patternMatcherGumTree.addMicroChange(new ExtendIfWithElse());
        patternMatcherGumTree.addMicroChange(new ExtendElseWithIf());
//        patternMatcherGumTree.addMicroChange(new SemanticallySameConditionUpdate());
        patternMatcherGumTree.addMicroChange(new ConditionalToBooleanReturn());
        patternMatcherGumTree.addMicroChange(new ConditionalToSwitch());
        patternMatcherGumTree.addMicroChange(new ReverseConditional());
        patternMatcherGumTree.addMicroChange(new ConditionalToTernaryOperator());
        patternMatcherGumTree.addMicroChange(new RemoveElse());
        patternMatcherGumTree.addMicroChange(new UnifyCondition());
        patternMatcherGumTree.addMicroChange(new ExtractFromCondition());
        patternMatcherGumTree.addMicroChange(new ParallelCondition());
        patternMatcherGumTree.addMicroChange(new ChangeBoundaryCondition());
        patternMatcherGumTree.addMicroChange(new RemoveConditionBlock());
        patternMatcherGumTree.addMicroChange(new InsertConditionBlock());
        patternMatcherGumTree.addMicroChange(new IntoCondition());
        patternMatcherGumTree.addMicroChange(new AddAdditionalCondition());
        patternMatcherGumTree.addMicroChange(new AddCurlyBrace());
        patternMatcherGumTree.addMicroChange(new RemoveCurlyBrace());
    }

    @Override
    public Integer call() throws Exception {
        //TODO the ratio decreases! debug the refactoring, mbassador loses one covered line
        log.info("start mining...");
        final RepositoryAccess ra = new RepositoryAccess(Path.of(config.methodLevelGitPath));
        final String repositoryName = Path.of(config.methodLevelGitPath).getParent().getFileName().toString();

        final DiffFormatter diffFormatter = new DiffFormatter(System.out);
        diffFormatter.setRepository(ra.getRepository());

//        Map<String, List<DiffEditScriptWithSource>> res = EditScriptExtractor.getEditScript(ra, diffFormatter);
        Map<String, List<DiffEditScriptWithSource>> res = EditScriptExtractor.getEditScriptForSingleCommit(ra, diffFormatter, "f3298f9f205526837ec3ba1520fce38e8840fbbd");
        log.info("Edit Script obtained for {} commits", res.size());

        //TODO: fix the logic here, the commit map is necessary for refactoring
        if (config.commitMap == null) {
            log.error("lack of commit map");
        }
        CommitMapper commitMapper = new CommitMapper(config.commitMap);

        log.info("Load refactorings");
        MethodLevelConvertor methodLevelConvertor = new MethodLevelConvertor(commitMapper);
        Map<String, List<Refactoring>> refMap = methodLevelConvertor.getMethodLevelRefactorings(config.refactoringPath, config.commitMap, config.methodLevelGitPath, config.originalRepoGitPath);
        log.info("Refactorings loaded");
        int refThreshold = 3;
        // exclude refactorings that across more than 3 lines (Extract related refs are excluded)
        RefactoringLoader.excludeRefactoringsAccordingToLineRanges(refMap, refThreshold);
        log.info("Excluded refactorings across more than {}", refThreshold);


        PatternMatcher patternMatcherGumTree = new PatternMatcherGumTree();
        // load micro change types
        loadMicroChanges(patternMatcherGumTree);

        List<CommitDAO> commitDAOs = new LinkedList<>();
        List<NotCoveredDAO> notCovered = new LinkedList<>();
//        List<MinedMicroChange> minedMicroChanges = new LinkedList<>();
        ActionLocator actionLocator = new ActionLocator();

        int count = 0;
        int numberMicroChangeContainedConditionRelatedAction = 0, numberTotalConditionRelatedActionNumber = 0;
        int total_count = res.keySet().size();
        int[] totalADCodeChangeLines = new int[]{0, 0};
        int[] microADChangeCoveredLines = new int[]{0, 0};
        int[] textDiffLines = new int[]{0, 0};
//        int[] refCoveredLines = new int[]{0, 0};
        int [] refCoveredCondition = new int[]{0, 0};
        int [] mrADChangeCoveredLines = new int[]{0,0};

        for (String commitID : res.keySet()) {
            List<MicroChangeDAO> microChangeDAOs = new LinkedList<>();
            List<RefactoringDAO> refactoringDAOs = new LinkedList<>();

//            if(!commitID.contains("3626ca6ec151c679dd5140b8c82dc92d24321d87"))
//                continue;
            count++;
            log.info("Mining {}/{} {}...", count, total_count, commitID);

            List<Refactoring> refactoringList = refMap.getOrDefault(commitID, new LinkedList<>());

            for (DiffEditScriptWithSource diffEditScriptWithSource : res.get(commitID)) {
                log.info("DiffEditScriptWithSource {}", diffEditScriptWithSource);
                EditScript editScript = diffEditScriptWithSource.getEditScript();
                EditScriptStorer editScriptStorer = diffEditScriptWithSource.getEditScriptStorer();
                Map<Tree, Tree> mappings = EditScriptExtractor.mappingStoreToMap(editScriptStorer.getMappingStore());
                Map<Tree, List<Action>> nodeActions = ActionRetriever.retrieveMap(editScript);

                SrcDstRange srcDstLineRangeOfIf = new SrcDstRange();
                if (editScriptStorer instanceof EditScriptStorerIncludeIf) {
                    srcDstLineRangeOfIf = ((EditScriptStorerIncludeIf) editScriptStorer).getSrcDstLineRangeOfIf();
                }

                // not include If condition, exclude
                if (srcDstLineRangeOfIf.isEmpty()) {
//                    log.info("if condition not included, skipped");
                    continue;
                }

                SrcDstRange treeActionPerFile = new SrcDstRange();
                SrcDstRange treeActionMicroChangePerFile = new SrcDstRange();

                log.info("# of actions {}", editScript.size());
                for (Action a : editScript) {
                    //mine micro-changes
                    List<MicroChange> microChanges = patternMatcherGumTree.match(a, mappings, nodeActions, editScriptStorer);

                    //action location
                    SrcDstRange treeActionRanges = actionLocator.getLineRanges(a, mappings, editScriptStorer);

                    if (!ActionStatus.hasIntersection(treeActionRanges, srcDstLineRangeOfIf)) {
                        continue;
                    }
                    numberTotalConditionRelatedActionNumber += 1;

                    treeActionPerFile.getSrcRange().addAll(treeActionRanges.getSrcRange());
                    treeActionPerFile.getDstRange().addAll(treeActionRanges.getDstRange());

                    if (microChanges.isEmpty()) continue;

                    //micro-change covered range
                    for (MicroChange microChange : microChanges) {
                        treeActionMicroChangePerFile.getSrcRange().addAll(microChange.getSrcDstRange().getSrcRange());
                        treeActionMicroChangePerFile.getDstRange().addAll(microChange.getSrcDstRange().getDstRange());
                    }

//                    minedMicroChanges.addAll(microChanges.stream()
//                            .map(p -> new MinedMicroChange(
//                                    repositoryName,
//                                    commitID,
//                                    diffEditScriptWithSource.getDiffEntry().getOldPath(),
//                                    diffEditScriptWithSource.getDiffEntry().getNewPath(),
//                                    p.getType(),
//                                    p.getAction(),
//                                    p.getSrcDstRange().toString())).toList());
                    microChangeDAOs.addAll(
                            microChanges.stream().map(p->new MicroChangeDAO(new MicroChangeFileSpecified(p,diffEditScriptWithSource.getDiffEntry()))).toList());
                    numberMicroChangeContainedConditionRelatedAction += 1;

                }

                List<Refactoring> inConditionRefactoring = getInConditionRefactoring(diffEditScriptWithSource, refactoringList, srcDstLineRangeOfIf);
                // include the refactoring covered lines
                SrcDstRange inConditionRefRange = extractRefactoringRange(inConditionRefactoring);

                refactoringDAOs.addAll(inConditionRefactoring.stream().map(RefactoringDAO::new).toList());


                // (micro-change U ref) ∩ tree-diff ∩ textual-if-location
                SrcDstRange microChangeUnionRefRange = calculateMicroChangeUnionRefRange(inConditionRefRange,
                        treeActionMicroChangePerFile,
                        srcDstLineRangeOfIf, treeActionPerFile);
                mrADChangeCoveredLines[0]+=coveredLength(microChangeUnionRefRange.getSrcRange());
                mrADChangeCoveredLines[1]+=coveredLength(microChangeUnionRefRange.getDstRange());


                // action touched if range
                int textRemoved = coveredLength(srcDstLineRangeOfIf.getSrcRange());
                int textAdded = coveredLength(srcDstLineRangeOfIf.getDstRange());

//                log.info("textRange: {}", textRange);

                // Some purely addition/deletion are regarded as Modify-Change-Type by jgit
                if (textRemoved > 0 && textAdded > 0) {
                    textDiffLines[0] += textRemoved;
                    textDiffLines[1] += textAdded;
                }


                if(!treeActionPerFile.getSrcRange().isEmpty()){
                    //intersection of text range and tree range
                    treeActionPerFile.getSrcRange().removeAll(srcDstLineRangeOfIf.getSrcRange().complement());
                    log.info("text range src: {}", srcDstLineRangeOfIf.getSrcRange());
                    log.info("tree diff: lines deleted {}", treeActionPerFile.getSrcRange());
                    totalADCodeChangeLines[0] += coveredLength(treeActionPerFile.getSrcRange());
                }


                if (!treeActionPerFile.getDstRange().isEmpty()) {
                    //intersection of text range and tree range
                    treeActionPerFile.getDstRange().removeAll(srcDstLineRangeOfIf.getDstRange().complement());
                    log.info("text range dst: {}", srcDstLineRangeOfIf.getDstRange());
                    log.info("tree diff: lines added {}", treeActionPerFile.getDstRange());
                    totalADCodeChangeLines[1] += coveredLength(treeActionPerFile.getDstRange());
                }


//                if (refactoringList != null) {
//                    // ref covered intersect action covered (discuss all the code changes under the context of being covered GumTree action)
//                    inConditionRefRange.getSrcRange().removeAll(treeActionPerFile.getSrcRange().complement());
//                    inConditionRefRange.getDstRange().removeAll(treeActionPerFile.getDstRange().complement());
//                    refCoveredLines[0] += coveredLength(inConditionRefRange.getSrcRange());
//                    refCoveredLines[1] += coveredLength(inConditionRefRange.getDstRange());
//                }

                if(!treeActionMicroChangePerFile.getSrcRange().isEmpty()){
                    //intersection of text range and tree range
                    treeActionMicroChangePerFile.getSrcRange().removeAll(srcDstLineRangeOfIf.getSrcRange().complement());
                    // intersection of micro-change range with tree range: because the calculation of micro-change range is not using the action node, while the tree range is, sometimes
                    // the micro-change covers a larger range than the tree range.
                    treeActionMicroChangePerFile.getSrcRange().removeAll(treeActionPerFile.getSrcRange().complement());
                    log.info("micro-change covered deleted lines {}", treeActionMicroChangePerFile.getSrcRange());
                    microADChangeCoveredLines[0] += coveredLength(treeActionMicroChangePerFile.getSrcRange());
                }

                if(!treeActionMicroChangePerFile.getDstRange().isEmpty()){
                    //intersection of text range and tree range
                    treeActionMicroChangePerFile.getDstRange().removeAll(srcDstLineRangeOfIf.getDstRange().complement());
                    // intersection of micro-change range with tree range: because the calculation of micro-change range is not using the action node, while the tree range is, sometimes
                    // the micro-change covers a larger range than the tree range.
                    treeActionMicroChangePerFile.getDstRange().removeAll(treeActionPerFile.getDstRange().complement());
                    log.info("micro-change covered added lines {}", treeActionMicroChangePerFile.getDstRange());
                    microADChangeCoveredLines[1] += coveredLength(treeActionMicroChangePerFile.getDstRange());
                }

                URL link = new URL(LinkAttacher.searchLink(repositoryName));

                // store mined micro changes
                if(!microChangeDAOs.isEmpty() || !refactoringDAOs.isEmpty()) {
                    commitDAOs.add(new CommitDAO(repositoryName,
                            commitMapper.getMap().get(commitID),
                            LinkAttacher.attachLink(commitMapper.getMap().get(commitID), link.toString()),
                            microChangeDAOs, refactoringDAOs));
                }


                // Not covered
                RangeSet<Integer> srcNotCovered =  notCoveredRange(treeActionPerFile.getSrcRange(), treeActionMicroChangePerFile.getSrcRange());
                RangeSet<Integer> dstNotCovered =  notCoveredRange(treeActionPerFile.getDstRange(), treeActionMicroChangePerFile.getDstRange());
                if(!srcNotCovered.isEmpty() || !dstNotCovered.isEmpty()) {
                    RangeSet<Integer> originalLevelSrcNotCovered = methodLevelConvertor.covertMethodLevelRangeToFileLevel(
                            methodLevelConvertor.getParentCommit(new File(config.methodLevelGitPath), commitID),
                            Path.of(config.methodLevelGitPath).getParent().toString(),
                            diffEditScriptWithSource.getDiffEntry().getOldPath(), srcNotCovered);
                    RangeSet<Integer> originalLevelDstNotCovered = methodLevelConvertor.covertMethodLevelRangeToFileLevel(
                            commitID, Path.of(config.methodLevelGitPath).getParent().toString(),
                            diffEditScriptWithSource.getDiffEntry().getNewPath(), dstNotCovered);
                    // Store not covered
                    notCovered.add(new NotCoveredDAO(repositoryName,
                            commitID,
                            LinkAttacher.attachLink(commitMapper.getMap().get(commitID), link.toString()),
                            diffEditScriptWithSource.getDiffEntry().getOldPath(),
                            diffEditScriptWithSource.getDiffEntry().getNewPath(),
                            originalLevelSrcNotCovered,
                            originalLevelDstNotCovered));
                    logNotCovered(srcNotCovered, dstNotCovered, commitID, commitMapper, link, diffEditScriptWithSource, methodLevelConvertor, config.methodLevelGitPath);
                }
            }
        }


        CSVWriter.writeCommit2Json(commitDAOs, config.outputJsonPath);
        CSVWriter.writeCommit2CSV(config.outputJsonPath, config.outputCsvPath);

        CSVWriter.writeNotCoveredToJson(notCovered, "./notCovered.json");


        logTreeDALines(totalADCodeChangeLines);
        logTextDALines(textDiffLines);
        logMicroChangeCoveredDALines(microADChangeCoveredLines);
        logMicroChangeCoverageRatio(microADChangeCoveredLines, totalADCodeChangeLines);
        logMicroChangeWithRefCoveregeRatio(mrADChangeCoveredLines, totalADCodeChangeLines);
        logRefactoringCoverageRatio(refCoveredCondition);
        logActions(numberTotalConditionRelatedActionNumber, numberMicroChangeContainedConditionRelatedAction);

//        log.info("Converting method-level commit hash to original hash according to {}", config.commitMap);
//        minedMicroChanges.forEach(p -> p.setCommitID(commitMapper.getMap().get((p.getCommitID()))));

//        CSVWriter.writeMicroChange2Json(minedMicroChanges, config.outputPath);

//        if (config.csvPath != null) {
//            CSVWriter.writeMircoChangesToCsv(config.outputPath, config.csvPath, new URL(LinkAttacher.searchLink(repositoryName)));
//        }

//        writeCSV(minedMicroChangesFileSpecified);

        return 0;
    }

    public static int coveredLength(RangeSet<Integer> rangeSet) {
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

    public static SrcDstRange calculateMicroChangeUnionRefRange(SrcDstRange inConditionRefRange,
                                                                SrcDstRange treeActionMicroChange,
                                                                SrcDstRange srcDstLineRangeOfIf,
                                                                SrcDstRange treeActionPerFile){
        // (micro-change U ref) ∩ tree-diff ∩ textual-if-location
        SrcDstRange res = new SrcDstRange();
        res.setSrcRange(TreeRangeSet.create(inConditionRefRange.getSrcRange()));
        res.setDstRange(TreeRangeSet.create(inConditionRefRange.getDstRange()));

        res.getSrcRange().addAll(treeActionMicroChange.getSrcRange());
        res.getDstRange().addAll(treeActionMicroChange.getDstRange());

        res.getSrcRange().removeAll(srcDstLineRangeOfIf.getSrcRange().complement());
        res.getDstRange().removeAll(srcDstLineRangeOfIf.getDstRange().complement());

        res.getSrcRange().removeAll(treeActionPerFile.getSrcRange().complement());
        res.getDstRange().removeAll(treeActionPerFile.getDstRange().complement());

        return res;
    }




    public static SrcDstRange getInConditionRefactoringRange(DiffEditScriptWithSource diffEditScriptWithSource, List<Refactoring> refactoringList, SrcDstRange srcDstLineRangeOfIf) {
        SrcDstRange range = new SrcDstRange();
        for (Refactoring refactoring : refactoringList) {
            //left side
            for (SideLocation sideLocation : refactoring.getLeftSideLocations()) {
                // condition expression range intersects refactoring covered range
                if (sideLocation.getPath().toString().equals(diffEditScriptWithSource.getDiffEntry().getOldPath())) {
                    for(Range<Integer> ifRange: srcDstLineRangeOfIf.getSrcRange().asRanges()){
                        if(sideLocation.getRange().encloses(ifRange)){
                            range.getSrcRange().add(ifRange);
                        }
                    }
                }
            }

            //right side
            for (SideLocation sideLocation : refactoring.getRightSideLocations()) {
                if (sideLocation.getPath().toString().equals(diffEditScriptWithSource.getDiffEntry().getNewPath())) {
                    for(Range<Integer> ifRange: srcDstLineRangeOfIf.getDstRange().asRanges()){
                        if(sideLocation.getRange().encloses(ifRange)){
                            range.getDstRange().add(ifRange);
                        }
                    }
                }
            }

        }
        return range;
    }

    public static List<Refactoring> getInConditionRefactoring(DiffEditScriptWithSource diffEditScriptWithSource, List<Refactoring> refactoringList, SrcDstRange srcDstLineRangeOfIf){
        List<Refactoring> inConditionRefactoring = new LinkedList<>();
        for (Refactoring refactoring : refactoringList) {
            List<SideLocation> leftSideLocations = new LinkedList<>();
            List<SideLocation> rightSideLocations = new LinkedList<>();
            //left side
            for (SideLocation sideLocation : refactoring.getLeftSideLocations()) {
                // condition expression range intersects refactoring covered range
                if (sideLocation.getPath().toString().equals(diffEditScriptWithSource.getDiffEntry().getOldPath())) {
                    for(Range<Integer> ifRange: srcDstLineRangeOfIf.getSrcRange().asRanges()){
                        if(sideLocation.getRange().encloses(ifRange)){
                            leftSideLocations.add(new SideLocation(sideLocation.getPath(),ifRange));
                        }
                    }
                }
            }

            //right side
            for (SideLocation sideLocation : refactoring.getRightSideLocations()) {
                if (sideLocation.getPath().toString().equals(diffEditScriptWithSource.getDiffEntry().getNewPath())) {
                    for(Range<Integer> ifRange: srcDstLineRangeOfIf.getDstRange().asRanges()){
                        if(sideLocation.getRange().encloses(ifRange)){
                            rightSideLocations.add(new SideLocation(sideLocation.getPath(),ifRange));
                        }
                    }
                }
            }

            if(!leftSideLocations.isEmpty() || !rightSideLocations.isEmpty()){
                inConditionRefactoring.add(new Refactoring(refactoring.getType(),leftSideLocations, rightSideLocations));
            }
        }
        return inConditionRefactoring;
    }


    public static SrcDstRange extractRefactoringRange(List<Refactoring> refactoringList){
        SrcDstRange res = new SrcDstRange();
        for(Refactoring ref: refactoringList){
            for (SideLocation leftSideLocation : ref.getLeftSideLocations()) {
                res.getSrcRange().add(leftSideLocation.getRange());
            }
            for (SideLocation rightSideLocation : ref.getRightSideLocations()) {
                res.getDstRange().add(rightSideLocation.getRange());
            }
        }
        return res;
    }

    public static RangeSet<Integer> notCoveredRange(RangeSet<Integer> biggerRange, RangeSet<Integer> smallerRange){
        RangeSet<Integer> notCovered = TreeRangeSet.create();
        for (Range allRange : biggerRange.asRanges()) {
            boolean flag = false;
            for (Range microRange : smallerRange.asRanges()) {
                if (microRange.encloses(allRange)) {
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                notCovered.add(allRange);
            }
        }
        return notCovered;
    }


    public static void logNotCovered(RangeSet<Integer> srcNotCovered,
                                     RangeSet<Integer> dstNotCovered,
                                     String commitID,
                                     CommitMapper commitMapper,
                                     URL link,
                                     DiffEditScriptWithSource diffEditScriptWithSource,
                                     MethodLevelConvertor methodLevelConvertor,
                                     String methodLevelGitPath){
            if(!srcNotCovered.isEmpty()){
                log.info("src not fully covered!: {}, {}, {}, {}",
                        commitID,
                        LinkAttacher.attachLink(commitMapper.getMap().get(commitID),
                                link.toString()),
                        diffEditScriptWithSource.getDiffEntry(),
                        methodLevelConvertor.covertMethodLevelRangeToFileLevel(
                                methodLevelConvertor.getParentCommit(new File(methodLevelGitPath), commitID),
                                Path.of(methodLevelGitPath).getParent().toString(),
                                diffEditScriptWithSource.getDiffEntry().getOldPath(), srcNotCovered));
            }
            if(!dstNotCovered.isEmpty()){
                log.info("dst not fully covered!: {}, {}, {}, {}",
                        commitID,
                        LinkAttacher.attachLink(commitMapper.getMap().get(commitID),
                                link.toString()),
                        diffEditScriptWithSource.getDiffEntry(),
                        methodLevelConvertor.covertMethodLevelRangeToFileLevel(commitID, Path.of(methodLevelGitPath).getParent().toString(), diffEditScriptWithSource.getDiffEntry().getNewPath(), dstNotCovered));
            }
    }


    public static void logTreeDALines(int [] totalADCodeChangeLines){
        log.info("total tree removed lines: {}", totalADCodeChangeLines[0]);
        log.info("total tree added lines: {}", totalADCodeChangeLines[1]);
    }
    public static void logTextDALines(int [] textDiffLines){
        log.info("total text removed lines: {}", textDiffLines[0]);
        log.info("total text added lines: {}", textDiffLines[1]);
    }
    public static void logMicroChangeCoveredDALines(int [] microADChangeCoveredLines){
        log.info("removed lines covered by micro-change: {}", microADChangeCoveredLines[0]);
        log.info("added lines covered by micro-change: {}", microADChangeCoveredLines[1]);
    }

    public static void logMicroChangeWithRefCoveregeRatio(int [] mrADChangeCoveredLines, int [] totalADCodeChangeLines){
        log.info("(removed lines covered by micro-change U ref)/number of total lines of tree code deleted: {}/{}=" + String.format("%.4f", (float) mrADChangeCoveredLines[0] / totalADCodeChangeLines[0]), mrADChangeCoveredLines[0], totalADCodeChangeLines[0]);
        log.info("(added lines covered by micro-change U ref)/number of total lines of tree code added: {}/{}=" + String.format("%.4f", (float) mrADChangeCoveredLines[1] / totalADCodeChangeLines[1]), mrADChangeCoveredLines[1], totalADCodeChangeLines[1]);

    }
    public static void logMicroChangeCoverageRatio(int [] microADChangeCoveredLines, int [] totalADCodeChangeLines){
        log.info("micro-change covered deleted lines/number of total lines of tree code deleted: {}/{}=" + String.format("%.4f", (float) microADChangeCoveredLines[0] / totalADCodeChangeLines[0]), microADChangeCoveredLines[0], totalADCodeChangeLines[0]);
        log.info("micro-change covered added lines/number of total lines of tree code added: {}/{}=" + String.format("%.4f", (float) microADChangeCoveredLines[1] / totalADCodeChangeLines[1]), microADChangeCoveredLines[1], totalADCodeChangeLines[1]);
    }
//    public static void logRefactoringCoverageRatio(int [] refCoveredLines, int [] refCoveredMicroChange,int [] totalADCodeChangeLines,int []  microADChangeCoveredLines){
//        log.info("refactoring covered deleted lines/number of total lines of tree code deleted: {}/{}=" + String.format("%.4f", (float) refCoveredLines[0] / totalADCodeChangeLines[0]), refCoveredLines[0], totalADCodeChangeLines[0]);
//        log.info("refactoring covered added lines/number of total lines of tree code added: {}/{}=" + String.format("%.4f", (float) refCoveredLines[1] / totalADCodeChangeLines[1]), refCoveredLines[1], totalADCodeChangeLines[1]);
//        log.info("refactoring covered micro-change deleted lines/micro-change covered deleted lines: {}/{}=" + String.format("%.4f", (float) refCoveredMicroChange[0] / microADChangeCoveredLines[0]), refCoveredMicroChange[0], microADChangeCoveredLines[0]);
//        log.info("refactoring covered micro-change added lines/micro-change covered added lines: {}/{}=" + String.format("%.4f", (float) refCoveredMicroChange[1] / microADChangeCoveredLines[1]), refCoveredMicroChange[1], microADChangeCoveredLines[1]);
//    }

    public static void logRefactoringCoverageRatio(int [] refCoveredCondition){
        log.info("refactoring covered deleted conditional lines :{}", refCoveredCondition[0]);
        log.info("refactoring covered added conditional lines :{}", refCoveredCondition[1]);
    }

    public static void logActions(int numberTotalActionNumber, int numberMicroChangeContainedAction){
        log.info("Total number of actions: {}", numberTotalActionNumber);
        log.info("Micro-change contained actions: {}", numberMicroChangeContainedAction);
        log.info("micro-change contained action/number of total actions: {}/{}=" + String.format("%.4f", (float) numberMicroChangeContainedAction / numberTotalActionNumber), numberMicroChangeContainedAction, numberTotalActionNumber);
    }

}