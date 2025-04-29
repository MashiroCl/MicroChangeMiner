package org.mashirocl.command;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.tree.Tree;
import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.mashirocl.dao.*;
import org.mashirocl.editscript.*;
import org.mashirocl.location.RangeOperations;
import org.mashirocl.match.ActionLocator;
import org.mashirocl.match.ActionStatus;
import org.mashirocl.match.PatternMatcher;
import org.mashirocl.match.PatternMatcherGumTree;
import org.mashirocl.microchange.*;
import org.mashirocl.microchange.loop.*;
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
import java.util.stream.Collectors;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/17 16:08
 */

@Slf4j
@Command(name = "mine", description = "Match micro-patterns for a repository")
public class MineCommand implements Callable<Integer> {

    public static class Config {
        @CommandLine.Parameters(index = "0",
                description = "Mining micro-changes in this repository")
        String methodLevelGitPath;

        @CommandLine.Parameters(index = "1", description = "Output mining result in json file")
        String outputJsonPath;

        @Option(names = {"-c", "--csv"}, description = "Output mining result to csv")
        String outputCsvPath;

        @Option(names = {"--map"},
                description = "Convert the method-level commit hash to original hash")
        String commitMap;

        @Option(names = {"--refactoring"}, description = "the RM result path for the repository")
        String refactoringPath;

        @Option(names = {"--original"}, description = "the original file-level repository git path")
        String originalRepoGitPath;

        @Option(names = {"--notCoveredPath"},
                description = "the output path for conditional expressions that are not covered by micro-change & refactoring")
        String notCoveredPath;

        // @Option(names = {"--brief"}, description = "the brief output")
        // boolean brief;
    }

    @CommandLine.Mixin
    protected Config config = new Config();


    public static void loadMicroChanges(PatternMatcher patternMatcherGumTree) {
         patternMatcherGumTree.addMicroChange(new AddConjunctOrDisjunct());
         patternMatcherGumTree.addMicroChange(new WrapStatementInBlock());
         patternMatcherGumTree.addMicroChange(new AdjustConditionBoundary());
         patternMatcherGumTree.addMicroChange(new ConditionalToBooleanReturn());
         patternMatcherGumTree.addMicroChange(new ConditionalToSwitch());
         patternMatcherGumTree.addMicroChange(new ConditionalToExpression());
         patternMatcherGumTree.addMicroChange(new WrapStatementInConditional());
         patternMatcherGumTree.addMicroChange(new ExtendIfWithElse());
         patternMatcherGumTree.addMicroChange(new ExtendElseWithIf());
         patternMatcherGumTree.addMicroChange(new UnwrapStatementFromConditional());
         patternMatcherGumTree.addMicroChange(new AddConditionalStatement());
         patternMatcherGumTree.addMicroChange(new MoveInwardCondition());
         patternMatcherGumTree.addMicroChange(new MoveOutwardCondition());
         patternMatcherGumTree.addMicroChange(new RemoveConditionalStatement());
         patternMatcherGumTree.addMicroChange(new UnwrapStatementFromBlock());
         patternMatcherGumTree.addMicroChange(new RemoveElse());
         patternMatcherGumTree.addMicroChange(new SwapThenAndElse());
         patternMatcherGumTree.addMicroChange(new ReverseCondition());
         patternMatcherGumTree.addMicroChange(new RemoveConjunctOrDisjunct());
         patternMatcherGumTree.addMicroChange(new FlipLogicOperator());
         patternMatcherGumTree.addMicroChange(new ConvertForToWhile());
         patternMatcherGumTree.addMicroChange(new ConvertForEachToFor());
         patternMatcherGumTree.addMicroChange(new AddLoopInitializer());
         patternMatcherGumTree.addMicroChange(new ChangeLoopInitializerType());
         patternMatcherGumTree.addMicroChange(new RenameLoopInitializer());
         patternMatcherGumTree.addMicroChange(new ChangeLoopInitializationExpression());
//        patternMatcherGumTree.addMicroChange(new ChangeLoopCondition());
        log.info("{}", patternMatcherGumTree.listLoadedMicroChanges());

        // patternMatcherGumTree.addMicroChange(new UnifyCondition());

    }


    private RepositoryAccess initRepository() {
        log.info("start mining...");
        return new RepositoryAccess(Path.of(config.methodLevelGitPath));
    }

    private Map<String, List<DiffEditScriptWithSource>> extractEditScripts(RepositoryAccess ra,
            DiffFormatter diffFormatter){
        diffFormatter.setRepository(ra.getRepository());
         Map<String, List<DiffEditScriptWithSource>> editScripts =
         EditScriptExtractor.getEditScript(ra, diffFormatter);
//        Map<String, List<DiffEditScriptWithSource>> editScripts =
//                EditScriptExtractor.getEditScriptForSingleCommit(ra, diffFormatter,
//                        "2a0b607dca280521e9f6fd2ab2dfa104a4366e7b");
        log.info("Edit Script obtained for {} commits", editScripts.size());
        return editScripts;
    }

    private Map<String, Map<String, SrcDstRange>> loadTextualDiff(String methodLevelGitPath,
            DiffFormatter diffFormatter){
        Map<String, Map<String, SrcDstRange>> textualDiff = TextualDiff
                .getTextualDiff(new RepositoryAccess(Path.of(methodLevelGitPath)), diffFormatter);
        log.info("Textual Diff loaded");
        return textualDiff;
    }

    private CommitMapper createCommitMapper() {
        if (config.commitMap == null) {
            log.error("lack of commit map");
        }
        return new CommitMapper(config.commitMap);
    }


    private Map<String, List<Refactoring>> loadRefactorings(MethodLevelConvertor methodLevelConvertor) {
        log.info("Load refactorings");
        Map<String, List<Refactoring>> refMap =
                methodLevelConvertor.getMethodLevelRefactorings(config.refactoringPath,
                        config.commitMap, config.methodLevelGitPath, config.originalRepoGitPath);
        log.info("Refactorings loaded");
        int refThreshold = 3;
        // exclude refactorings that across more than 3 lines (Extract related refs are excluded)
        RefactoringLoader.excludeRefactoringsAccordingToLineRanges(refMap, refThreshold);
        log.info("Excluded refactorings across more than {}", refThreshold);

        return refMap;
    }

    private void writeResults(List<CommitDAO> commitDAOs, List<NotCoveredDAO> notCovered){
        CSVWriter.writeCommit2Json(commitDAOs, config.outputJsonPath);
        CSVWriter.writeCommit2CSV(config.outputJsonPath, config.outputCsvPath);

        if (config.notCoveredPath == null) {
            CSVWriter.writeNotCoveredToJson(notCovered, "./notCovered.json");
        } else {
            CSVWriter.writeNotCoveredToJson(notCovered, config.notCoveredPath);
        }
    }

    private void logSummaryStats(ProcessingStats stats) {
        logStructureChangeLength(stats.structureExpressionChangeLines);
        logStructureExpressionChangeContainedCommit(stats.structureExpressionChangeContainedCommit);
        logNumberOfFilesBeingProcessed(stats.numberOfFilesProcessed);
        logTreeDALines(stats.totalADCodeChangeLines);
        logMicroChangeCoveredDALines(stats.microADChangeCoveredLines);
        logMicroChangeCoverageRatio(stats.microADChangeCoveredLines, stats.totalADCodeChangeLines);
        logMicroChangeWithRefCoveregeRatio(stats.mrADChangeCoveredLines, stats.totalADCodeChangeLines);
        logActions(stats.numberTotalConditionRelatedActionNumber,
                stats.numberMicroChangeContainedStructureRelatedAction);
    }


    private Map<String, RenameRefactoring> extractRenameRefactorings(
            List<Refactoring> refactoringList) {
        Map<String, RenameRefactoring> renamingMap = new HashMap<>();
        for (Refactoring ref : refactoringList) {
            if (ref instanceof RenameRefactoring) {
                renamingMap.put(((RenameRefactoring) ref).getRename(), (RenameRefactoring) ref);
            }
        }
        return renamingMap;
    }


    private ProcessingResults processCommits(
            Map<String, List<DiffEditScriptWithSource>> commitEditscriptMap,
            Map<String, Map<String, SrcDstRange>> textualDiff,
            Map<String, List<Refactoring>> refMap, String repositoryName,
            PatternMatcher patternMatcherGumTree) throws Exception {

        ProcessingStats stats = new ProcessingStats();
        List<CommitDAO> commitDAOs = new LinkedList<>();
        List<NotCoveredDAO> notCovered = new LinkedList<>();
        ActionLocator actionLocator = new ActionLocator();

        int count = 0;
        int total_count = commitEditscriptMap.keySet().size();
        log.info("Number of commits to be processed: {}", commitEditscriptMap.size());

        for (String commitID : commitEditscriptMap.keySet()) {
            count++;
            log.info("Mining {}/{} {}...", count, total_count, commitID);

            processCommit(commitID, commitEditscriptMap.get(commitID), textualDiff, refMap,
                    repositoryName, commitDAOs, notCovered, stats, actionLocator, patternMatcherGumTree);
        }

        return new ProcessingResults(commitDAOs, notCovered, stats);
    }

    private void processCommit(String commitID, List<DiffEditScriptWithSource> diffScripts,
            Map<String, Map<String, SrcDstRange>> textualDiff,
            Map<String, List<Refactoring>> refMap, String repositoryName,
            List<CommitDAO> commitDAOs, List<NotCoveredDAO> notCovered, ProcessingStats stats,
            ActionLocator actionLocator, PatternMatcher patternMatcherGumTree) throws Exception {

        List<MicroChangeDAO> microChangeDAOs = new LinkedList<>();
        List<RefactoringDAO> refactoringDAOs = new LinkedList<>();

        CommitMapper commitMapper = createCommitMapper();

        List<Refactoring> refactoringList = refMap.getOrDefault(commitID, new LinkedList<>());
        Map<String, RenameRefactoring> renamingMap = extractRenameRefactorings(refactoringList);
        log.info("(added)renamingMap {}", renamingMap);

        for (DiffEditScriptWithSource diffScript : diffScripts) {
            stats.numberOfFilesProcessed++;
            processSingleFileDiff(diffScript, commitID, textualDiff, renamingMap, refactoringList,
                    repositoryName, microChangeDAOs, refactoringDAOs, notCovered, stats,
                    actionLocator, patternMatcherGumTree);


            // Store mined micro changes if any were found
            if (!microChangeDAOs.isEmpty() || !refactoringDAOs.isEmpty()) {
                URL link = new URL(LinkAttacher.searchLink(repositoryName));
                commitDAOs.add(new CommitDAO(repositoryName, commitMapper.getMap().get(commitID),
                        LinkAttacher.attachLink(commitMapper.getMap().get(commitID), link.toString()),
                        microChangeDAOs, refactoringDAOs));
            }
        }

    }

    /**
     * Process a single file diff from a commit
     */
    private void processSingleFileDiff(
            DiffEditScriptWithSource diffScript,
            String commitID,
            Map<String, Map<String, SrcDstRange>> textualDiff,
            Map<String, RenameRefactoring> renamingMap,
            List<Refactoring> refactoringList,
            String repositoryName,
            List<MicroChangeDAO> microChangeDAOs,
            List<RefactoringDAO> refactoringDAOs,
            List<NotCoveredDAO> notCovered,
            ProcessingStats stats,
            ActionLocator actionLocator,
            PatternMatcher patternMatcherGumTree) throws Exception {

        log.info("DiffEditScriptWithSource {}", diffScript);

        EditScript editScript = diffScript.getEditScript();
        EditScriptStorer editScriptStorer = diffScript.getEditScriptStorer();
        Map<Tree, Tree> mappings = EditScriptExtractor.mappingStoreToMap(editScriptStorer.getMappingStore());
        Map<Tree, List<Action>> nodeActions = ActionRetriever.retrieveMap(editScript);

        // Extract conditional expression ranges
//        SrcDstRange srcDstLineRangeOfIf = extractStructureRanges(ControlStructureType.IF, editScriptStorer, stats);

        Map<ControlStructureType, SrcDstRange> structureRanges = new HashMap<>();
        for (ControlStructureType type : ControlStructureType.values()) {
            structureRanges.put(type, extractStructureRanges(type, editScriptStorer, stats));
        }


        // Intersect structure expression ranges with textual diff if available
        if (textualDiff.containsKey(commitID) &&
                textualDiff.get(commitID).containsKey(diffScript.getDiffEntry().getOldPath())) {
            structureRanges.keySet().forEach(type -> {
                structureRanges.compute(type, (k, range) -> ActionStatus.getIntersection(range, textualDiff.get(commitID).get(diffScript.getDiffEntry().getOldPath())));});

        for(ControlStructureType type : structureRanges.keySet()) {
            log.info("Structure Ranges {}: {}", type, structureRanges.get(type));

        }
//            srcDstLineRangeOfIf = ActionStatus.getIntersection(
//                    srcDstLineRangeOfIf,
//                    textualDiff.get(commitID).get(diffScript.getDiffEntry().getOldPath()));
        }

        // not include If condition, exclude
        // if (srcDstLineRangeOfIf.isEmpty()) {
        // log.info("if condition not included, skipped");
        // continue;
        // }

        // Process actions and collect ranges
        ChangeProcessingResult changeResult = processDiffActions(
                editScript, mappings, nodeActions, editScriptStorer,
                structureRanges, actionLocator, patternMatcherGumTree, stats, renamingMap, diffScript);

        // Process microchanges and refactorings
        processChangesAndRefactorings(
                diffScript, commitID, changeResult.microChanges, refactoringList,
                changeResult.treeActionRange, structureRanges, repositoryName,
                microChangeDAOs, refactoringDAOs, notCovered, stats);
    }

    /**
     * Process microchanges and refactorings,
     * 1. convert to file level,
     * 2. intersect the micro-change ranges with tree-diff and structure expression ranges,
     * 3. intersect the refactoring ranges with tree-diff and structure expression ranges,
     * 4. union the micro-change range and refactoring ranges
     * 5. store micro-changeDAO and refactoringDAOs
     */
    private void processChangesAndRefactorings(
            DiffEditScriptWithSource diffScript,
            String commitID,
            List<MicroChange> microChangesPerFile,
            List<Refactoring> refactoringList,
            SrcDstRange treeActionRange,
            Map<ControlStructureType, SrcDstRange> structureRanges,
//            SrcDstRange conditionalExprRange,
            String repositoryName,
            List<MicroChangeDAO> microChangeDAOs,
            List<RefactoringDAO> refactoringDAOs,
            List<NotCoveredDAO> notCovered,
            ProcessingStats stats) throws Exception {

        CommitMapper commitMapper = createCommitMapper();
        MethodLevelConvertor methodLevelConvertor = new MethodLevelConvertor(commitMapper);

        // Convert microchanges to file-specified format
        List<MicroChangeFileSpecified> microChangeFileSpecifiedList = convertToFileSpecified(
                microChangesPerFile, diffScript.getDiffEntry());


        // Intersection with tree-diff and conditional expression ranges
//        List<MicroChangeFileSpecified> inConditionMicroChange = microChangeIntersectWithRange(
//                microChangeFileSpecifiedList, treeActionRange);
//        List<MicroChangeFileSpecified> treeDiffInConditionMicroChange = microChangeIntersectWithRange(
//                inConditionMicroChange, conditionalExprRange);

        // Intersection of micro-chagnes with tree-diff and with structure expression range
        Map<ControlStructureType, List<MicroChangeFileSpecified>> structureChangeMicroChange = new HashMap<>();
        for (ControlStructureType type : structureRanges.keySet()) {
            structureChangeMicroChange.put(type, microChangeIntersectWithRange(
                    microChangeFileSpecifiedList, treeActionRange));
        }
        Map<ControlStructureType, List<MicroChangeFileSpecified>> treeDiffStructureChangeMicroChange = new HashMap<>();
        for (ControlStructureType type : structureRanges.keySet()) {
            treeDiffStructureChangeMicroChange.put(type, microChangeIntersectWithRange(
                    structureChangeMicroChange.get(type), structureRanges.get(type)));
        }

        // Intersection of refactorings with tree-diff and conditional expression ranges
//        List<Refactoring> inConditionRefactoring = refactoringIntersectWithRange(
//                diffScript, refactoringList, treeActionRange);
//        List<Refactoring> treeDiffInConditionRefactoring = refactoringIntersectWithRange(
//                diffScript, inConditionRefactoring, conditionalExprRange);


        // Intersection of refactorings with tree-diff and structure expression ranges
        Map<ControlStructureType, List<Refactoring>> structureChangeRefactoring = new HashMap<>();
        for (ControlStructureType type : structureRanges.keySet()) {
            structureChangeRefactoring.put(type, refactoringIntersectWithRange(
                    diffScript, refactoringList, treeActionRange));
        }
        Map<ControlStructureType, List<Refactoring>> treeDiffStructureChangeRefactoring = new HashMap<>();
        for(ControlStructureType type : structureRanges.keySet()) {
            treeDiffStructureChangeRefactoring.put(type, refactoringIntersectWithRange(
                    diffScript, structureChangeRefactoring.get(type), structureRanges.get(type)));
        }

        // Calculate combined range of microchanges and refactorings
//        SrcDstRange microChangeUnionRefRange = calculateMicroChangeUnionRefRange(
//                structureChangeMicroChange, treeDiffStructureChangeRefactoring, diffScript);

        // Calculate combined range of microchanges and refactorings
        log.info("(added) treeDiffStructureChangeRefactoring {}", treeDiffStructureChangeRefactoring.get(ControlStructureType.FOR));
        log.info("(added) treeDiffStructureChangeMicroChange {}", treeDiffStructureChangeMicroChange.get(ControlStructureType.FOR));
        Map<ControlStructureType, SrcDstRange> microChangeUnionRefRange = new HashMap<>();
        for (ControlStructureType type : structureRanges.keySet()) {
            microChangeUnionRefRange.put(type, calculateMicroChangeUnionRefRange(treeDiffStructureChangeMicroChange.get(type),
                    treeDiffStructureChangeRefactoring.get(type), diffScript));
        }

        log.info("(added) structureChangeUnionRefRange {}", microChangeUnionRefRange);

        // Update statistics
        updateStatistics(treeActionRange, structureRanges, treeDiffStructureChangeMicroChange,
                microChangeUnionRefRange, stats);

        // Process uncovered ranges
        processUncoveredRanges(commitID, diffScript, treeActionRange, structureRanges,
                microChangeUnionRefRange, repositoryName, notCovered, methodLevelConvertor);

        // Convert and store microchanges and refactorings
        storeChangesAndRefactorings(commitID, treeDiffStructureChangeMicroChange, treeDiffStructureChangeRefactoring,
                microChangeDAOs, refactoringDAOs, methodLevelConvertor);
    }

    /**
     * Convert microchanges to file-specified format
     */
    private List<MicroChangeFileSpecified> convertToFileSpecified(
            List<MicroChange> microChanges, DiffEntry diffEntry) {

        return microChanges.stream()
                .map(m -> new MicroChangeFileSpecified(m, diffEntry))
                .collect(Collectors.toList());
    }

    /**
     * Update statistics based on processed changes
     */
    private void updateStatistics(
            SrcDstRange treeActionRange,
            Map<ControlStructureType, SrcDstRange> structureRanges,
//            SrcDstRange conditionalExprRange,
            Map<ControlStructureType, List<MicroChangeFileSpecified>> structureChangeMicroChange,
//            List<MicroChangeFileSpecified> treeDiffInConditionMicroChange,
            Map<ControlStructureType, SrcDstRange> structureChangeUnionRefRange,
//            SrcDstRange microChangeUnionRefRange,
            ProcessingStats stats) {

        // Update microchange coverage statistics
        RangeSet<Integer> structureChangeUnionRefRangeSrc = TreeRangeSet.create();
        RangeSet<Integer> structureChangeUnionRefRangeDst = TreeRangeSet.create();
        structureChangeMicroChange.keySet().forEach(type -> structureChangeUnionRefRangeSrc.addAll(structureChangeUnionRefRange.get(type).getSrcRange()));
        structureChangeMicroChange.keySet().forEach(type -> structureChangeUnionRefRangeDst.addAll(structureChangeUnionRefRange.get(type).getDstRange()));

        log.info("(added) structureChangeUnionRefRangeSrc {}", structureChangeUnionRefRangeSrc);
        log.info("(added) structureChangeUnionRefRangeSrc length {}", coveredLength(structureChangeUnionRefRangeSrc));
        stats.mrADChangeCoveredLines[0] += coveredLength(structureChangeUnionRefRangeSrc);
        stats.mrADChangeCoveredLines[1] += coveredLength(structureChangeUnionRefRangeDst);

//        stats.mrADChangeCoveredLines[0] += coveredLength(microChangeUnionRefRange.getSrcRange());
//        stats.mrADChangeCoveredLines[1] += coveredLength(microChangeUnionRefRange.getDstRange());

        boolean structureChangeContainFlag = false;

        //TODO: need to union first? any duplpicate counted lines?
        // Update total AD code change statistics
        for(ControlStructureType type : structureRanges.keySet()) {
            RangeSet<Integer> temp = RangeOperations.deepCopyRangeSet(treeActionRange.getSrcRange());
            RangeSet<Integer> structureExprRange = structureRanges.get(type).getSrcRange();
            temp.removeAll(structureExprRange.complement());
            structureChangeContainFlag = structureChangeContainFlag || !temp.isEmpty();
            log.info("{} expression range src: {}", type, structureExprRange);
            log.info("{} expression lines deleted {}", type, temp);
            stats.totalADCodeChangeLines[0] += coveredLength(temp);
            stats.structureExpressionChangeLines.get(type)[0]+=coveredLength(temp);
        }

//        if (!treeActionRange.getSrcRange().isEmpty()) {
//            RangeSet<Integer> temp = RangeOperations.deepCopyRangeSet(treeActionRange.getSrcRange());
//            temp.removeAll(conditionalExprRange.getSrcRange().complement());
//            log.info("conditional expression range src: {}", conditionalExprRange.getSrcRange());
//            log.info("conditional expression lines deleted {}", treeActionRange);
//            stats.totalADCodeChangeLines[0] += coveredLength(treeActionRange.getSrcRange());
//        }

//        if (!treeActionRange.getDstRange().isEmpty()) {
//            treeActionRange.getDstRange().removeAll(conditionalExprRange.getDstRange().complement());
//            log.info("conditional expression range dst: {}", conditionalExprRange.getDstRange());
//            log.info("conditional expression lines added {}", treeActionRange.getDstRange());
//            stats.totalADCodeChangeLines[1] += coveredLength(treeActionRange.getDstRange());
//        }

        for(ControlStructureType type : structureRanges.keySet()) {
            RangeSet<Integer> temp = RangeOperations.deepCopyRangeSet(treeActionRange.getDstRange());
            RangeSet<Integer> structureExprRange = structureRanges.get(type).getDstRange();
            temp.removeAll(structureExprRange.complement());
            structureChangeContainFlag = structureChangeContainFlag || !temp.isEmpty();
            log.info("{} expression range dst: {}", type, structureExprRange);
            log.info("{} expression lines added {}", type, temp);
            stats.totalADCodeChangeLines[1] += coveredLength(temp);
            stats.structureExpressionChangeLines.get(type)[1]+=coveredLength(temp);
        }

        // structureRange intersection is not empty
        if (structureChangeContainFlag) {
            stats.structureExpressionChangeContainedCommit += 1;
        }

        for(ControlStructureType type : structureChangeMicroChange.keySet()) {
            if (!structureChangeMicroChange.get(type).isEmpty()) {
                RangeSet<Integer> srcRangeSet = TreeRangeSet.create();
                for (MicroChangeFileSpecified m : structureChangeMicroChange.get(type)) {
                    m.getLeftSideLocations().forEach(p -> srcRangeSet.add(p.getRange()));
                }
                stats.microADChangeCoveredLines[0] += coveredLength(srcRangeSet);

                RangeSet<Integer> dstRangeSet = TreeRangeSet.create();
                for (MicroChangeFileSpecified m : structureChangeMicroChange.get(type)) {
                    m.getRightSideLocations().forEach(p -> dstRangeSet.add(p.getRange()));
                }
                stats.microADChangeCoveredLines[1] += coveredLength(dstRangeSet);
            }
        }

        // Update microchange coverage statistics
//        if (!treeDiffInConditionMicroChange.isEmpty()) {
//            RangeSet<Integer> srcRangeSet = TreeRangeSet.create();
//            for (MicroChangeFileSpecified m : treeDiffInConditionMicroChange) {
//                m.getLeftSideLocations().forEach(p -> srcRangeSet.add(p.getRange()));
//            }
//            stats.microADChangeCoveredLines[0] += coveredLength(srcRangeSet);
//
//            RangeSet<Integer> dstRangeSet = TreeRangeSet.create();
//            for (MicroChangeFileSpecified m : treeDiffInConditionMicroChange) {
//                m.getRightSideLocations().forEach(p -> dstRangeSet.add(p.getRange()));
//            }
//            stats.microADChangeCoveredLines[1] += coveredLength(dstRangeSet);
//        }
    }

    /**
     * Process uncovered ranges and store them
     */
    private void processUncoveredRanges(
            String commitID,
            DiffEditScriptWithSource diffScript,
            SrcDstRange treeActionRange,
            Map<ControlStructureType, SrcDstRange> structureRanges,
//            SrcDstRange conditionalExprRange,
//            SrcDstRange microChangeUnionRefRange,
            Map<ControlStructureType, SrcDstRange> structureChangeUnionRefRange ,
            String repositoryName,
            List<NotCoveredDAO> notCovered,
            MethodLevelConvertor methodLevelConvertor) throws Exception {

        URL link = new URL(LinkAttacher.searchLink(repositoryName));
        CommitMapper commitMapper = createCommitMapper();

        // Calculate the range that should be covered (intersection of tree action and structure expression)
        SrcDstRange shouldCoveredRange = ActionStatus.getIntersection(treeActionRange, structureRanges.values().stream().toList());

        // Calculate not covered ranges
//        RangeSet<Integer> srcNotCovered = notCoveredRange(shouldCoveredRange.getSrcRange(), microChangeUnionRefRange.getSrcRange());
//        RangeSet<Integer> dstNotCovered = notCoveredRange(shouldCoveredRange.getDstRange(), microChangeUnionRefRange.getDstRange());

        RangeSet<Integer> srcNotCovered = notCoveredRange(shouldCoveredRange.getSrcRange(), ActionStatus.getUnion(structureChangeUnionRefRange.values().stream().toList()).getSrcRange());
        RangeSet<Integer> dstNotCovered = notCoveredRange(shouldCoveredRange.getDstRange(), ActionStatus.getUnion(structureChangeUnionRefRange.values().stream().toList()).getDstRange());

        if (!srcNotCovered.isEmpty() || !dstNotCovered.isEmpty()) {
            // Convert method-level ranges to file-level
            RangeSet<Integer> originalLevelSrcNotCovered = methodLevelConvertor.covertMethodLevelRangeToFileLevel(
                    methodLevelConvertor.getParentCommit(new File(config.methodLevelGitPath), commitID),
                    Path.of(config.methodLevelGitPath).getParent().toString(),
                    diffScript.getDiffEntry().getOldPath(), srcNotCovered);

            RangeSet<Integer> originalLevelDstNotCovered = methodLevelConvertor.covertMethodLevelRangeToFileLevel(
                    commitID, Path.of(config.methodLevelGitPath).getParent().toString(),
                    diffScript.getDiffEntry().getNewPath(), dstNotCovered);

            // Store not covered ranges
            notCovered.add(new NotCoveredDAO(repositoryName,
                    commitID,
                    LinkAttacher.attachLink(commitMapper.getMap().get(commitID), link.toString()),
                    diffScript.getDiffEntry().getOldPath(),
                    diffScript.getDiffEntry().getNewPath(),
                    originalLevelSrcNotCovered,
                    originalLevelDstNotCovered));

            logNotCovered(srcNotCovered, dstNotCovered, commitID, commitMapper, link,
                    diffScript, methodLevelConvertor, config.methodLevelGitPath);
        }
    }

    /**
     * Store microchanges and refactorings as DAOs
     */
    private void storeChangesAndRefactorings(
            String commitID,
            Map<ControlStructureType, List<MicroChangeFileSpecified>> treeDiffStructureChangeMicroChange,
//            List<MicroChangeFileSpecified> treeDiffInConditionMicroChange,
            Map<ControlStructureType, List<Refactoring>> treeDiffStructureChangeRefactoring,
//            List<Refactoring> treeDiffInConditionRefactoring,
            List<MicroChangeDAO> microChangeDAOs,
            List<RefactoringDAO> refactoringDAOs,
            MethodLevelConvertor methodLevelConvertor) {

        // Convert and store microchanges
        for (MicroChangeFileSpecified microChange : treeDiffStructureChangeMicroChange.values().stream().flatMap(List::stream).toList()) {
            methodLevelConvertor.covertMethodLevelMicroChangeToFileLevel(commitID, config.methodLevelGitPath, microChange);
            microChangeDAOs.add(new MicroChangeDAO(microChange));
        }

        // Convert and store refactorings
        for (Refactoring refactoring : treeDiffStructureChangeRefactoring.values().stream().flatMap(List::stream).toList()) {
            methodLevelConvertor.covertMethodLevelRefactoringToFileLevel(commitID, config.methodLevelGitPath, refactoring);
            refactoringDAOs.add(new RefactoringDAO(refactoring));
        }
    }

    /**
     * return line ranges for structure expressions such as IF, FOR, WHILE in the whole file etc.
     * @param editScriptStorer
     * @param stats
     * @return
     */
    private SrcDstRange extractStructureRanges(ControlStructureType type, EditScriptStorer editScriptStorer, ProcessingStats stats){
        SrcDstRange structureRanges = editScriptStorer.getStructureRange(type);
        stats.updateNumberOfStructureExpressions(type, structureRanges);
        return structureRanges;
    }


    private SrcDstRange extractLoopExpresionRanges(EditScriptStorer editScriptStorer, ProcessingStats stats){
        SrcDstRange srcDstRangeOfLoop = new SrcDstRange();
        return null;
    }


    /**
     * Process actions from the diff and collect microchanges and affected ranges
     */
    private ChangeProcessingResult processDiffActions(EditScript editScript,Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions,
                                                 EditScriptStorer editScriptStorer, Map<ControlStructureType, SrcDstRange> structureRanges,ActionLocator actionLocator,
                                                      PatternMatcher patternMatcherGumTree, ProcessingStats stats, Map<String, RenameRefactoring> renamingMap, DiffEditScriptWithSource diffScript){
        List<MicroChange> microChangesPerFile = new LinkedList<>();
        SrcDstRange treeActionPerFile = new SrcDstRange(); // record action ranges that intersect with structure changes
        Map<ControlStructureType, SrcDstRange> structureActionRanges = new HashMap<>();
        log.info("# of actions {}", editScript.size());
        for (Action a : editScript) {
            // action location
            SrcDstRange treeActionRanges =
                    actionLocator.getLineRanges(a, mappings, editScriptStorer);

            // if it is an renaming action, skip the micro-change detection
            RenameRefactoring renameRefactoring = collectRenameContainedActionsRange(a, renamingMap, treeActionRanges, diffScript);
            if(renameRefactoring!=null) continue;

            // mine micro-changes
            List<MicroChange> microChanges =
                    patternMatcherGumTree.match(a, mappings, nodeActions, editScriptStorer);


            if (!microChanges.isEmpty()) {
                microChangesPerFile.addAll(microChanges);
                stats.numberMicroChangeContainedStructureRelatedAction += 1;
            }


            // if tree action overlapped with IF-Statement, record it in treeActionPerFile
//            if (ActionStatus.hasIntersection(treeActionRanges, srcDstLineRangeOfIf)) {
//                stats.numberTotalConditionRelatedActionNumber += 1;
//                treeActionPerFile.getSrcRange().addAll(treeActionRanges.getSrcRange());
//                treeActionPerFile.getDstRange().addAll(treeActionRanges.getDstRange());
//            }

            // if tree action overlapped with structure expressions, record it in treeActionPerFile
            for (ControlStructureType type : structureRanges.keySet()) {
                SrcDstRange structureActionRange = structureRanges.get(type);
                if (structureActionRange!=null && ActionStatus.hasIntersection(treeActionRanges, structureActionRange)) {
                    stats.updateStructureActionNumber(type, 1);
                    treeActionPerFile.getSrcRange().addAll(treeActionRanges.getSrcRange());
                    treeActionPerFile.getDstRange().addAll(treeActionRanges.getDstRange());
                }
            }

        }
        log.info("(added) treeActionPerFile {}", treeActionPerFile);
        return new ChangeProcessingResult(microChangesPerFile, treeActionPerFile);

    }

    /**
     * Result class for diff action processing
     */
    private static class ChangeProcessingResult {
        final List<MicroChange> microChanges;
        final SrcDstRange treeActionRange;

        public ChangeProcessingResult(List<MicroChange> microChanges, SrcDstRange treeActionRange) {
            this.microChanges = microChanges;
            this.treeActionRange = treeActionRange;
        }
    }

    /**
     * Collect rename contained actions range
     */
    private RenameRefactoring collectRenameContainedActionsRange(
            Action action,
            Map<String, RenameRefactoring> renamingMap,
            SrcDstRange actionRanges,
            DiffEditScriptWithSource diffScript) {

        RenameRefactoring renameRefactoring = ActionStatus.getRenamingRefactoring(action, renamingMap);
        if (renameRefactoring != null) {
            renameRefactoring.attachLineRange(diffScript, actionRanges);
        }
       return renameRefactoring;
    }


    @Override
    public Integer call() throws Exception {
        final RepositoryAccess ra = initRepository();
        final String repositoryName =
                Path.of(config.methodLevelGitPath).getParent().getFileName().toString();
        final DiffFormatter diffFormatter = new DiffFormatter(System.out);
        diffFormatter.setRepository(ra.getRepository());

        Map<String, List<DiffEditScriptWithSource>> commitEditscriptMap =
                extractEditScripts(ra, diffFormatter);
        Map<String, Map<String, SrcDstRange>> textualDiff =
                loadTextualDiff(config.methodLevelGitPath, diffFormatter);

        CommitMapper commitMapper = createCommitMapper();
        MethodLevelConvertor methodLevelConvertor = new MethodLevelConvertor(commitMapper);
        Map<String, List<Refactoring>> refMap =
                loadRefactorings(methodLevelConvertor);

        PatternMatcher patternMatcherGumTree = new PatternMatcherGumTree();
        // load micro change types
        loadMicroChanges(patternMatcherGumTree);

        log.info("Number of commits to be processed: {}", commitEditscriptMap.size());

        ProcessingResults results = processCommits(commitEditscriptMap, textualDiff, refMap, repositoryName, patternMatcherGumTree);

        // Write results
        writeResults(results.commitDAOs, results.notCovered);
        logSummaryStats(results.stats);


        // logTextDALines(textDiffLines);

        // log.info("Converting method-level commit hash to original hash according to {}",
        // config.commitMap);
        // minedMicroChanges.forEach(p ->
        // p.setCommitID(commitMapper.getMap().get((p.getCommitID()))));

        // CSVWriter.writeMicroChange2Json(minedMicroChanges, config.outputPath);

        // if (config.csvPath != null) {
        // CSVWriter.writeMircoChangesToCsv(config.outputPath, config.csvPath, new
        // URL(LinkAttacher.searchLink(repositoryName)));
        // }

        // writeCSV(minedMicroChangesFileSpecified);

        return 0;
    }

    public static int coveredLength(RangeSet<Integer> rangeSet) {
        int coveredLength = 0;
        for (Range<Integer> range : rangeSet.asRanges()) {
            coveredLength += MineCommand.coveredLength(range);
        }
        return coveredLength;
    }

    public static int coveredLength(Range<Integer> range) {
        int length = 0;
        if (range.hasLowerBound() && range.hasUpperBound()) {
            // Calculate the basic length
            length = range.upperEndpoint() - range.lowerEndpoint() - 1; // Subtract 1 because we
                                                                        // start assuming both ends
                                                                        // are open

            // Adjust based on the inclusivity of the endpoints
            if (range.lowerBoundType() == BoundType.CLOSED) {
                length += 1; // Include the lower endpoint if it's closed
            }
            if (range.upperBoundType() == BoundType.CLOSED) {
                length += 1; // Include the upper endpoint if it's closed
            }
        }
        return length;
    }

    public static SrcDstRange getRefactoringRangeInFile(List<Refactoring> refactorings,
            String oldPath, String newPath) {
        SrcDstRange refactoringRange = new SrcDstRange();
        for (Refactoring ref : refactorings) {
            // refactoring left side
            for (SideLocation sideLocation : ref.getLeftSideLocations()) {
                if (!sideLocation.getPath().toString().equals(oldPath)) {
                    continue;
                }
                refactoringRange.getSrcRange().add(sideLocation.getRange());
            }
            // refactoring right side
            for (SideLocation sideLocation : ref.getRightSideLocations()) {
                if (!sideLocation.getPath().toString().equals(newPath)) {
                    continue;
                }
                refactoringRange.getDstRange().add(sideLocation.getRange());
            }
        }
        return refactoringRange;
    }

    public static SrcDstRange getMicroChangeRangeInFile(List<MicroChangeFileSpecified> microChanges,
            String oldPath, String newPath) {
        SrcDstRange microChangeRange = new SrcDstRange();

        for (MicroChangeFileSpecified microChange : microChanges) {
            // refactoring left side
            for (SideLocation sideLocation : microChange.getLeftSideLocations()) {
                if (!sideLocation.getPath().toString().equals(oldPath)) {
                    continue;
                }
                microChangeRange.getSrcRange().add(sideLocation.getRange());
            }
            // refactoring right side
            for (SideLocation sideLocation : microChange.getRightSideLocations()) {
                if (!sideLocation.getPath().toString().equals(newPath)) {
                    continue;
                }
                microChangeRange.getDstRange().add(sideLocation.getRange());
            }
        }
        return microChangeRange;
    }

    public static SrcDstRange calculateMicroChangeUnionRefRange(
            List<MicroChangeFileSpecified> microChanges, List<Refactoring> refactorings,
            DiffEditScriptWithSource diffEditScriptWithSource) {
        // micro-change U ref
        SrcDstRange res = new SrcDstRange();
        // micro change range
        SrcDstRange microChangeRange = getMicroChangeRangeInFile(microChanges,
                diffEditScriptWithSource.getDiffEntry().getOldPath(),
                diffEditScriptWithSource.getDiffEntry().getNewPath());


        SrcDstRange refactoringRange = getRefactoringRangeInFile(refactorings,
                diffEditScriptWithSource.getDiffEntry().getOldPath(),
                diffEditScriptWithSource.getDiffEntry().getNewPath());



        // union
        res.setSrcRange(TreeRangeSet.create(refactoringRange.getSrcRange()));
        res.setDstRange(TreeRangeSet.create(refactoringRange.getDstRange()));
        res.getSrcRange().addAll(microChangeRange.getSrcRange());
        res.getDstRange().addAll(microChangeRange.getDstRange());

        return res;
    }



    public static SrcDstRange getInConditionRefactoringRange(
            DiffEditScriptWithSource diffEditScriptWithSource, List<Refactoring> refactoringList,
            SrcDstRange srcDstLineRangeOfIf) {
        SrcDstRange range = new SrcDstRange();
        for (Refactoring refactoring : refactoringList) {
            // left side
            for (SideLocation sideLocation : refactoring.getLeftSideLocations()) {
                // condition expression range intersects refactoring covered range
                if (sideLocation.getPath().toString()
                        .equals(diffEditScriptWithSource.getDiffEntry().getOldPath())) {
                    for (Range<Integer> ifRange : srcDstLineRangeOfIf.getSrcRange().asRanges()) {
                        if (sideLocation.getRange().encloses(ifRange)) {
                            range.getSrcRange().add(ifRange);
                        }
                    }
                }
            }

            // right side
            for (SideLocation sideLocation : refactoring.getRightSideLocations()) {
                if (sideLocation.getPath().toString()
                        .equals(diffEditScriptWithSource.getDiffEntry().getNewPath())) {
                    for (Range<Integer> ifRange : srcDstLineRangeOfIf.getDstRange().asRanges()) {
                        if (sideLocation.getRange().encloses(ifRange)) {
                            range.getDstRange().add(ifRange);
                        }
                    }
                }
            }

        }
        return range;
    }

    public static List<Refactoring> refactoringIntersectWithRange(
            DiffEditScriptWithSource diffEditScriptWithSource, List<Refactoring> refactoringList,
            SrcDstRange range) {
        List<Refactoring> res = new LinkedList<>();
        // left side
        for (Refactoring refactoring : refactoringList) {
            List<SideLocation> leftSideLocations = new LinkedList<>();
            // left side
            for (SideLocation sideLocation : refactoring.getLeftSideLocations()) {
                if (sideLocation.getPath().toString()
                        .equals(diffEditScriptWithSource.getDiffEntry().getOldPath())) {
                    for (Range<Integer> r : range.getSrcRange().asRanges()) {
                        if (r.isConnected(sideLocation.getRange())) {
                            leftSideLocations.add(new SideLocation(sideLocation.getPath(),
                                    r.intersection(sideLocation.getRange())));
                        }
                    }
                }
            }

            // right side
            List<SideLocation> rightSideLocations = new LinkedList<>();
            for (SideLocation sideLocation : refactoring.getRightSideLocations()) {
                if (sideLocation.getPath().toString()
                        .equals(diffEditScriptWithSource.getDiffEntry().getNewPath())) {
                    for (Range<Integer> r : range.getDstRange().asRanges()) {
                        if (r.isConnected(sideLocation.getRange())) {
                            rightSideLocations.add(new SideLocation(sideLocation.getPath(),
                                    r.intersection(sideLocation.getRange())));
                        }
                    }
                }
            }
            if (!leftSideLocations.isEmpty() || !rightSideLocations.isEmpty()) {
                res.add(new Refactoring(refactoring.getType(), refactoring.getDescription(),
                        leftSideLocations, rightSideLocations));
            }
        }
        return res;
    }

    public static List<MicroChangeFileSpecified> microChangeIntersectWithRange(
            List<MicroChangeFileSpecified> microChangeList, SrcDstRange range) {
        List<MicroChangeFileSpecified> res = new LinkedList<>();
        for (MicroChangeFileSpecified microChange : microChangeList) {
            List<SideLocation> leftSideLocations = new LinkedList<>();
            // left side
            for (SideLocation sideLocation : microChange.getLeftSideLocations()) {
                for (Range<Integer> r : range.getSrcRange().asRanges()) {
                    if (r.isConnected(sideLocation.getRange())) {
                        leftSideLocations.add(new SideLocation(sideLocation.getPath(),
                                r.intersection(sideLocation.getRange())));
                    }
                }
            }

            // right side
            List<SideLocation> rightSideLocations = new LinkedList<>();
            for (SideLocation sideLocation : microChange.getRightSideLocations()) {
                for (Range<Integer> r : range.getDstRange().asRanges()) {
                    if (r.isConnected(sideLocation.getRange())) {
                        rightSideLocations.add(new SideLocation(sideLocation.getPath(),
                                r.intersection(sideLocation.getRange())));
                    }
                }
            }
            if (!leftSideLocations.isEmpty() || !rightSideLocations.isEmpty()) {
                res.add(new MicroChangeFileSpecified(microChange.getType(), microChange.getAction(),
                        leftSideLocations, rightSideLocations));
            }
        }
        return res;
    }


    public static SrcDstRange extractRefactoringRange(List<Refactoring> refactoringList) {
        SrcDstRange res = new SrcDstRange();
        for (Refactoring ref : refactoringList) {
            for (SideLocation leftSideLocation : ref.getLeftSideLocations()) {
                res.getSrcRange().add(leftSideLocation.getRange());
            }
            for (SideLocation rightSideLocation : ref.getRightSideLocations()) {
                res.getDstRange().add(rightSideLocation.getRange());
            }
        }
        return res;
    }

    public static RangeSet<Integer> notCoveredRange(RangeSet<Integer> biggerRange,
            RangeSet<Integer> smallerRange) {
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
            RangeSet<Integer> dstNotCovered, String commitID, CommitMapper commitMapper, URL link,
            DiffEditScriptWithSource diffEditScriptWithSource,
            MethodLevelConvertor methodLevelConvertor, String methodLevelGitPath) {
        if (!srcNotCovered.isEmpty()) {
            log.info("src not fully covered!: {}, {}, {}, {}", commitID,
                    LinkAttacher.attachLink(commitMapper.getMap().get(commitID), link.toString()),
                    diffEditScriptWithSource.getDiffEntry(),
                    methodLevelConvertor.covertMethodLevelRangeToFileLevel(
                            methodLevelConvertor.getParentCommit(new File(methodLevelGitPath),
                                    commitID),
                            Path.of(methodLevelGitPath).getParent().toString(),
                            diffEditScriptWithSource.getDiffEntry().getOldPath(), srcNotCovered));
        }
        if (!dstNotCovered.isEmpty()) {
            log.info("dst not fully covered!: {}, {}, {}, {}", commitID,
                    LinkAttacher.attachLink(commitMapper.getMap().get(commitID), link.toString()),
                    diffEditScriptWithSource.getDiffEntry(),
                    methodLevelConvertor.covertMethodLevelRangeToFileLevel(commitID,
                            Path.of(methodLevelGitPath).getParent().toString(),
                            diffEditScriptWithSource.getDiffEntry().getNewPath(), dstNotCovered));
        }
    }


    public static void  logStructureChangeLength(Map<ControlStructureType, int[]> structureChangeLength){
        for (ControlStructureType type : structureChangeLength.keySet()) {
            log.info("Structure change src length of {} is {}", type, structureChangeLength.get(type)[0]);
            log.info("Structure change dst length of {} is {}", type, structureChangeLength.get(type)[1]);
        }
    }

    public static void logTreeDALines(int[] totalADCodeChangeLines) {
        log.info("total tree removed lines: {}", totalADCodeChangeLines[0]);
        log.info("total tree added lines: {}", totalADCodeChangeLines[1]);
    }

    public static void logTextDALines(int[] textDiffLines) {
        log.info("total text removed lines: {}", textDiffLines[0]);
        log.info("total text added lines: {}", textDiffLines[1]);
    }

    public static void logMicroChangeCoveredDALines(int[] microADChangeCoveredLines) {
        log.info("removed lines covered by micro-change: {}", microADChangeCoveredLines[0]);
        log.info("added lines covered by micro-change: {}", microADChangeCoveredLines[1]);
    }

    public static void logMicroChangeWithRefCoveregeRatio(int[] mrADChangeCoveredLines,
            int[] totalADCodeChangeLines) {
        log.info(
                "(removed lines covered by micro-change U ref)/number of total lines of tree code deleted: {}/{}="
                        + String.format("%.4f",
                                (float) mrADChangeCoveredLines[0] / totalADCodeChangeLines[0]),
                mrADChangeCoveredLines[0], totalADCodeChangeLines[0]);
        log.info(
                "(added lines covered by micro-change U ref)/number of total lines of tree code added: {}/{}="
                        + String.format("%.4f",
                                (float) mrADChangeCoveredLines[1] / totalADCodeChangeLines[1]),
                mrADChangeCoveredLines[1], totalADCodeChangeLines[1]);

    }

    public static void logMicroChangeCoverageRatio(int[] microADChangeCoveredLines,
            int[] totalADCodeChangeLines) {
        log.info(
                "micro-change covered deleted lines/number of total lines of tree code deleted: {}/{}="
                        + String.format("%.4f",
                                (float) microADChangeCoveredLines[0] / totalADCodeChangeLines[0]),
                microADChangeCoveredLines[0], totalADCodeChangeLines[0]);
        log.info(
                "micro-change covered added lines/number of total lines of tree code added: {}/{}="
                        + String.format("%.4f",
                                (float) microADChangeCoveredLines[1] / totalADCodeChangeLines[1]),
                microADChangeCoveredLines[1], totalADCodeChangeLines[1]);
    }

    public static void logRefactoringCoverageRatio(int[] refCoveredCondition) {
        log.info("refactoring covered deleted conditional lines :{}", refCoveredCondition[0]);
        log.info("refactoring covered added conditional lines :{}", refCoveredCondition[1]);
    }

    public static void logActions(int numberTotalActionNumber,
            int numberMicroChangeContainedAction) {
        log.info("Total number of actions: {}", numberTotalActionNumber);
        log.info("Micro-change contained actions: {}", numberMicroChangeContainedAction);
        log.info(
                "micro-change contained action/number of total actions: {}/{}=" + String.format(
                        "%.4f", (float) numberMicroChangeContainedAction / numberTotalActionNumber),
                numberMicroChangeContainedAction, numberTotalActionNumber);
    }

    public static void logStructureExpressionChangeContainedCommit(
            int numberOfConditionalExpression) {
        log.info("Number of structure-expression-change contained commit: {}",
                numberOfConditionalExpression);
    }

    public static void logNumberOfFilesBeingProcessed(int numberOfFilesBeingProcessed) {
        log.info("number of files being processed: {}", numberOfFilesBeingProcessed);
    }

}
