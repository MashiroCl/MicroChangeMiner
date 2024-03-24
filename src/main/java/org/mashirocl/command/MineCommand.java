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
import org.mashirocl.refactoringminer.*;
import org.mashirocl.textualdiff.TextualDiff;
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
        patternMatcherGumTree.addMicroChange(new AddAdditionalCondition());
        patternMatcherGumTree.addMicroChange(new AddCurlyBrace());
        patternMatcherGumTree.addMicroChange(new ChangeBoundaryCondition());
        patternMatcherGumTree.addMicroChange(new ConditionalToBooleanReturn());
        patternMatcherGumTree.addMicroChange(new ConditionalToSwitch());
        patternMatcherGumTree.addMicroChange(new ConditionalToTernaryOperator());
        patternMatcherGumTree.addMicroChange(new EncapsulateInCondition());
        patternMatcherGumTree.addMicroChange(new ExtendIfWithElse());
        patternMatcherGumTree.addMicroChange(new ExtendElseWithIf());
        patternMatcherGumTree.addMicroChange(new ExtractFromCondition());
        patternMatcherGumTree.addMicroChange(new InsertConditionBlock());
        patternMatcherGumTree.addMicroChange(new LiftCondition());
        patternMatcherGumTree.addMicroChange(new LowerCondition());
        patternMatcherGumTree.addMicroChange(new RemoveConditionBlock());
        patternMatcherGumTree.addMicroChange(new RemoveCurlyBrace());
        patternMatcherGumTree.addMicroChange(new RemoveElse());
        patternMatcherGumTree.addMicroChange(new ReverseThenElse());
        patternMatcherGumTree.addMicroChange(new ReverseConditional());
        patternMatcherGumTree.addMicroChange(new SimplifyConditional());
//        patternMatcherGumTree.addMicroChange(new UnifyCondition());

    }

    @Override
    public Integer call() throws Exception {
        log.info("start mining...");
        final RepositoryAccess ra = new RepositoryAccess(Path.of(config.methodLevelGitPath));
        final String repositoryName = Path.of(config.methodLevelGitPath).getParent().getFileName().toString();

        final DiffFormatter diffFormatter = new DiffFormatter(System.out);
        diffFormatter.setRepository(ra.getRepository());

        Map<String, List<DiffEditScriptWithSource>> res = EditScriptExtractor.getEditScript(ra, diffFormatter);
//        Map<String, List<DiffEditScriptWithSource>> res = EditScriptExtractor.getEditScriptForSingleCommit(ra, diffFormatter, "8586dd92d305f490ccb452bf516639f23cf4fedf");
        log.info("Edit Script obtained for {} commits", res.size());
        Map<String, Map<String, SrcDstRange>> textualDiff = TextualDiff.getTextualDiff(new RepositoryAccess(Path.of(config.methodLevelGitPath)), diffFormatter);
        log.info("Textual Diff loaded");

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
        ActionLocator actionLocator = new ActionLocator();

        int count = 0;
        int numberMicroChangeContainedConditionRelatedAction = 0, numberTotalConditionRelatedActionNumber = 0;
        int total_count = res.keySet().size();
        int[] totalADCodeChangeLines = new int[]{0, 0};
        int[] microADChangeCoveredLines = new int[]{0, 0};
        int[] textDiffLines = new int[]{0, 0};
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
            // load renaming refactoring
            Map<String, RenameRefactoring> renamingMap = new HashMap<>();
            for(Refactoring refactoring:refactoringList){
                if(refactoring instanceof RenameRefactoring){
                    String renameSignature = ((RenameRefactoring) refactoring).getRename();
                    renamingMap.put(renameSignature, (RenameRefactoring) refactoring);
                    }
            }

//            log.info("refactoringList {}", refactoringList);
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

                // intersect with textual diff
                if(textualDiff.containsKey(commitID)
                        && textualDiff.get(commitID).containsKey(diffEditScriptWithSource.getDiffEntry().getOldPath())){
                    srcDstLineRangeOfIf = ActionStatus.getIntersection(
                            srcDstLineRangeOfIf,
                            textualDiff.get(commitID).get(diffEditScriptWithSource.getDiffEntry().getOldPath()));
                }

                // not include If condition, exclude
                if (srcDstLineRangeOfIf.isEmpty()) {
//                    log.info("if condition not included, skipped");
                    continue;
                }

                SrcDstRange treeActionPerFile = new SrcDstRange();
                SrcDstRange treeActionMicroChangePerFile = new SrcDstRange();
                List<MicroChange> microChangesPerFile = new LinkedList<>();

                log.info("# of actions {}", editScript.size());
                for (Action a : editScript) {
                    //mine micro-changes
                    List<MicroChange> microChanges = patternMatcherGumTree.match(a, mappings, nodeActions, editScriptStorer);

                    //action location
                    SrcDstRange treeActionRanges = actionLocator.getLineRanges(a, mappings, editScriptStorer);

                    if (!microChanges.isEmpty()) {
                        microChangesPerFile.addAll(microChanges);
                        log.info("microChangesPerFile {}", microChangesPerFile);
                        numberMicroChangeContainedConditionRelatedAction += 1;
                    }

                    if (ActionStatus.hasIntersection(treeActionRanges, srcDstLineRangeOfIf)) {
                        numberTotalConditionRelatedActionNumber += 1;
                        treeActionPerFile.getSrcRange().addAll(treeActionRanges.getSrcRange());
                        treeActionPerFile.getDstRange().addAll(treeActionRanges.getDstRange());
                    }

                    // collect rename contained actions range
                    RenameRefactoring renameRefactoring = ActionStatus.getRenamingRefactoring(a, renamingMap);
                    if(renameRefactoring!=null){
                        renameRefactoring.attachLineRange(diffEditScriptWithSource, treeActionRanges);
                    }

                }


                // micro-change ∩ tree-diff ∩ textual-if-location
                List<MicroChangeFileSpecified> microChangeFileSpecifiedListPerFile = new LinkedList<>();
                microChangesPerFile.stream().forEach(p->microChangeFileSpecifiedListPerFile.add(new MicroChangeFileSpecified(p,diffEditScriptWithSource.getDiffEntry())));
                List<MicroChangeFileSpecified> inConditionMicroChange = microChangeIntersectWithRange(microChangeFileSpecifiedListPerFile, treeActionPerFile);
                List<MicroChangeFileSpecified> treeDiffInConditionMicroChange = microChangeIntersectWithRange(inConditionMicroChange, srcDstLineRangeOfIf);


                // refactoring ∩ tree-diff ∩ textual-if-location
                List<Refactoring> inConditionRefactoring = refactoringIntersectWithRange(diffEditScriptWithSource, refactoringList, treeActionPerFile);
                List<Refactoring> treeDiffInConditonRefactoring = refactoringIntersectWithRange(diffEditScriptWithSource, inConditionRefactoring, srcDstLineRangeOfIf);

                // (micro-change U ref) ∩ tree-diff ∩ textual-if-location
                SrcDstRange microChangeUnionRefRange = calculateMicroChangeUnionRefRange(treeDiffInConditionMicroChange, treeDiffInConditonRefactoring, diffEditScriptWithSource);

                mrADChangeCoveredLines[0]+=coveredLength(microChangeUnionRefRange.getSrcRange());
                mrADChangeCoveredLines[1]+=coveredLength(microChangeUnionRefRange.getDstRange());

                // Not covered
                // TODO debug here
                URL link = new URL(LinkAttacher.searchLink(repositoryName));
                SrcDstRange shouldCoveredRange = ActionStatus.getIntersection(treeActionPerFile, srcDstLineRangeOfIf);
                RangeSet<Integer> srcNotCovered = notCoveredRange(shouldCoveredRange.getSrcRange(), microChangeUnionRefRange.getSrcRange());
                RangeSet<Integer> dstNotCovered =  notCoveredRange(shouldCoveredRange.getDstRange(), microChangeUnionRefRange.getDstRange());
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


                // convert the method-level micro-changes in the same file to file level & store
                for(MicroChangeFileSpecified microChange:treeDiffInConditionMicroChange){
                    methodLevelConvertor.covertMethodLevelMicroChangeToFileLevel(commitID,config.methodLevelGitPath,microChange);
                    microChangeDAOs.add(new MicroChangeDAO(microChange));
                }


                // convert the method-level refactoring in the same file to file level & store
                for(Refactoring r:treeDiffInConditonRefactoring){
                    methodLevelConvertor.covertMethodLevelRefactoringToFileLevel(commitID, config.methodLevelGitPath, r);
                    refactoringDAOs.add(new RefactoringDAO(r));
                }

                // action touched if range
                int textRemoved = coveredLength(srcDstLineRangeOfIf.getSrcRange());
                int textAdded = coveredLength(srcDstLineRangeOfIf.getDstRange());


                // Some purely addition/deletion are regarded as Modify-Change-Type by jgit
                if (textRemoved > 0 && textAdded > 0) {
                    textDiffLines[0] += textRemoved;
                    textDiffLines[1] += textAdded;
                }


                if(!treeActionPerFile.getSrcRange().isEmpty()){
                    //intersection of text range and tree range
                    treeActionPerFile.getSrcRange().removeAll(srcDstLineRangeOfIf.getSrcRange().complement());
                    log.info("text if range src: {}", srcDstLineRangeOfIf.getSrcRange());
                    log.info("tree diff: lines deleted {}", treeActionPerFile.getSrcRange());
                    totalADCodeChangeLines[0] += coveredLength(treeActionPerFile.getSrcRange());
                }


                if (!treeActionPerFile.getDstRange().isEmpty()) {
                    //intersection of text range and tree range
                    treeActionPerFile.getDstRange().removeAll(srcDstLineRangeOfIf.getDstRange().complement());
                    log.info("text if range dst: {}", srcDstLineRangeOfIf.getDstRange());
                    log.info("tree diff: lines added {}", treeActionPerFile.getDstRange());
                    totalADCodeChangeLines[1] += coveredLength(treeActionPerFile.getDstRange());
                }


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

//                if(!microChangeUnionRefRange.getSrcRange().isEmpty()){
//                    log.info("micro-change U refactoring covered deleted lines {}", microChangeUnionRefRange.getSrcRange());
//                }
//                if(!microChangeUnionRefRange.getDstRange().isEmpty()){
//                    log.info("micro-change U refactoring covered added lines {}", microChangeUnionRefRange.getDstRange());
//                }


                // store mined micro changes
                if(!microChangeDAOs.isEmpty() || !refactoringDAOs.isEmpty()) {
                    commitDAOs.add(new CommitDAO(repositoryName,
                            commitMapper.getMap().get(commitID),
                            LinkAttacher.attachLink(commitMapper.getMap().get(commitID), link.toString()),
                            microChangeDAOs,
                            refactoringDAOs));
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

    public static SrcDstRange getRefactoringRangeInFile(List<Refactoring> refactorings, String oldPath, String newPath){
        SrcDstRange refactoringRange = new SrcDstRange();
        for(Refactoring ref: refactorings){
            // refactoring left side
            for(SideLocation sideLocation: ref.getLeftSideLocations()){
                if(!sideLocation.getPath().toString().equals(oldPath)){
                    continue;
                }
                refactoringRange.getSrcRange().add(sideLocation.getRange());
            }
            // refactoring right side
            for(SideLocation sideLocation: ref.getRightSideLocations()){
                if(!sideLocation.getPath().toString().equals(newPath)){
                    continue;
                }
                refactoringRange.getDstRange().add(sideLocation.getRange());
            }
        }
        //TODO remove
        log.info("get refactoring Range: {}", refactoringRange);
        return  refactoringRange;
    }

    public static SrcDstRange getMicroChangeRangeInFile(List<MicroChangeFileSpecified> microChanges, String oldPath, String newPath){
        SrcDstRange microChangeRange = new SrcDstRange();

        log.info("oldPath: {}", oldPath);
        log.info("newPath: {}", newPath);
        log.info("microChange: {}", microChanges);
        for(MicroChangeFileSpecified microChange: microChanges){
            // refactoring left side
            for(SideLocation sideLocation: microChange.getLeftSideLocations()){
                log.info("sideLocations: {}", sideLocation);
                if(!sideLocation.getPath().toString().equals(oldPath)){
                    continue;
                }
                microChangeRange.getSrcRange().add(sideLocation.getRange());
            }
            // refactoring right side
            for(SideLocation sideLocation: microChange.getRightSideLocations()){
                if(!sideLocation.getPath().toString().equals(newPath)){
                    continue;
                }
                microChangeRange.getDstRange().add(sideLocation.getRange());
            }
        }
        log.info("get microchange Range: {}", microChangeRange);
        return  microChangeRange;
    }

    public static SrcDstRange calculateMicroChangeUnionRefRange(List<MicroChangeFileSpecified> microChanges,
                                                                List<Refactoring> refactorings,
                                                                DiffEditScriptWithSource diffEditScriptWithSource){
        // micro-change U ref
        SrcDstRange res = new SrcDstRange();
        // micro change range
        SrcDstRange microChangeRange = getMicroChangeRangeInFile(
                microChanges,
                diffEditScriptWithSource.getDiffEntry().getOldPath(),
                diffEditScriptWithSource.getDiffEntry().getNewPath()
        );


        SrcDstRange refactoringRange = getRefactoringRangeInFile(
                refactorings,
                diffEditScriptWithSource.getDiffEntry().getOldPath(),
                diffEditScriptWithSource.getDiffEntry().getNewPath()
        );



        // union
        res.setSrcRange(TreeRangeSet.create(refactoringRange.getSrcRange()));
        res.setDstRange(TreeRangeSet.create(refactoringRange.getDstRange()));
        res.getSrcRange().addAll(microChangeRange.getSrcRange());
        res.getDstRange().addAll(microChangeRange.getDstRange());

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

    public static List<Refactoring> refactoringIntersectWithRange(DiffEditScriptWithSource diffEditScriptWithSource, List<Refactoring> refactoringList, SrcDstRange range) {
        List<Refactoring> res = new LinkedList<>();
        // left side
        for (Refactoring refactoring : refactoringList) {
            List<SideLocation> leftSideLocations = new LinkedList<>();
            //left side
            for (SideLocation sideLocation : refactoring.getLeftSideLocations()) {
                if (sideLocation.getPath().toString().equals(diffEditScriptWithSource.getDiffEntry().getOldPath())) {
                    for (Range<Integer> r : range.getSrcRange().asRanges()) {
                        if(r.isConnected(sideLocation.getRange())){
                            leftSideLocations.add(new SideLocation(sideLocation.getPath(), r.intersection(sideLocation.getRange())));
                        }
                    }
                }
            }

            // right side
            List<SideLocation> rightSideLocations = new LinkedList<>();
            for (SideLocation sideLocation : refactoring.getRightSideLocations()) {
                if (sideLocation.getPath().toString().equals(diffEditScriptWithSource.getDiffEntry().getNewPath())) {
                    for (Range<Integer> r : range.getDstRange().asRanges()) {
                        if(r.isConnected(sideLocation.getRange())){
                            rightSideLocations.add(new SideLocation(sideLocation.getPath(), r.intersection(sideLocation.getRange())));
                        }
                    }
                }
            }
            if(!leftSideLocations.isEmpty() || !rightSideLocations.isEmpty()){
                res.add(new Refactoring(refactoring.getType(),leftSideLocations, rightSideLocations));
            }
        }
        return res;
    }

    public static List<MicroChangeFileSpecified> microChangeIntersectWithRange(List<MicroChangeFileSpecified> microChangeList, SrcDstRange range){
        List<MicroChangeFileSpecified> res = new LinkedList<>();
        for(MicroChangeFileSpecified microChange: microChangeList){
            List<SideLocation> leftSideLocations = new LinkedList<>();
            //left side
            for (SideLocation sideLocation : microChange.getLeftSideLocations()) {
                for (Range<Integer> r : range.getSrcRange().asRanges()) {
                    if(r.isConnected(sideLocation.getRange())){
                        leftSideLocations.add(new SideLocation(sideLocation.getPath(), r.intersection(sideLocation.getRange())));
                    }
                }
            }

            // right side
            List<SideLocation> rightSideLocations = new LinkedList<>();
            for (SideLocation sideLocation : microChange.getRightSideLocations()) {
                    for (Range<Integer> r : range.getDstRange().asRanges()) {
                        if(r.isConnected(sideLocation.getRange())){
                            rightSideLocations.add(new SideLocation(sideLocation.getPath(), r.intersection(sideLocation.getRange())));
                        }
                    }
            }
            if(!leftSideLocations.isEmpty() || !rightSideLocations.isEmpty()){
                res.add(new MicroChangeFileSpecified(microChange.getType(), microChange.getAction(), leftSideLocations, rightSideLocations));
            }
        }
        return res;
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