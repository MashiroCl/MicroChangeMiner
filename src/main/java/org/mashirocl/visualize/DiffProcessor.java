package org.mashirocl.visualize;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.Tree;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.mashirocl.editscript.EditScriptExtractor;
import org.mashirocl.editscript.EditScriptStorer;
import org.mashirocl.match.ActionLocator;
import org.mashirocl.microchange.SrcDstRange;
import org.mashirocl.refactoringminer.MethodLevelConvertor;
import org.mashirocl.refactoringminer.SideLocation;
import org.mashirocl.source.FileSource;
import org.mashirocl.source.SourcePair;
import org.mashirocl.textualdiff.Chunk;
import org.mashirocl.textualdiff.Differencer;
import org.mashirocl.textualdiff.JGitDifferencer;
import org.mashirocl.util.CommitMapper;
import org.mashirocl.util.RepositoryAccess;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author mashirocl@gmail.com
 * @since 2024/04/24 13:57
 */

@Slf4j
@AllArgsConstructor
@Getter
@Setter
public class DiffProcessor {
    private final DiffFormatter diffFormatter;
    public final EditScriptGenerator editScriptGenerator;
    public final ActionLocator actionLocator;

    public DiffRange getTreeDiff(final RepositoryAccess repositoryAccess, final MethodLevelConvertor methodLevelConvertor, final String methodLevelGitPath) {
        log.info("Obtaining tree diff...");
        DiffRange treeDiffs = new DiffRange();
        Iterable<RevCommit> commits = getCommits(repositoryAccess);

        for (RevCommit commit : commits) {
            List<DiffEntry> diffEntries = filterDiffEntries(commit);
            if (diffEntries.isEmpty()) continue;

            List<SideLocation> leftSideLocations = new LinkedList<>();
            List<SideLocation> rightSideLocations = new LinkedList<>();
            processDiffs(repositoryAccess, diffEntries, commit, leftSideLocations, rightSideLocations, "TreeDiff");

            if (!leftSideLocations.isEmpty() && !rightSideLocations.isEmpty()) {
                methodLevelConvertor.convertMethodLevelActionToFileLevel(commit.getId().name(),methodLevelGitPath ,leftSideLocations, rightSideLocations);
                treeDiffs.getDiff().put(commit.getId().name(), new PairSideLocation(leftSideLocations, rightSideLocations));
            }
        }

//        resetRepositoryAccess(methodLevelGitPath);
        return treeDiffs;
    }

    /**
     * get the textual diff in method level repository and convert the result to file level
     * @param repositoryAccess
     * @param methodLevelConvertor
     * @param methodLevelGitPath
     * @return
     */
    public DiffRange getTextualDiff(final RepositoryAccess repositoryAccess, final MethodLevelConvertor methodLevelConvertor, final String methodLevelGitPath) {
        log.info("Obtaining textual diff...");
        DiffRange textualDiffs = new DiffRange();
        Iterable<RevCommit> commits = getCommits(repositoryAccess);

        for (RevCommit commit : commits) {
            log.info(commit.getId().name());
            List<DiffEntry> diffEntries = filterDiffEntries(commit);
            if (diffEntries.isEmpty()) continue;

            List<SideLocation> leftSideLocations = new LinkedList<>();
            List<SideLocation> rightSideLocations = new LinkedList<>();
            processDiffs(repositoryAccess, diffEntries, commit, leftSideLocations, rightSideLocations, "TextualDiff");

            if (!leftSideLocations.isEmpty() && !rightSideLocations.isEmpty()) {
                methodLevelConvertor.convertMethodLevelActionToFileLevel(commit.getId().name(),methodLevelGitPath ,leftSideLocations, rightSideLocations);
                textualDiffs.getDiff().put(commit.getId().name(), new PairSideLocation(leftSideLocations, rightSideLocations));
            }
        }

        log.info("finished");
//        resetRepositoryAccess(methodLevelGitPath);
        return textualDiffs;
    }


    public DiffRange getTextualDiff(final RepositoryAccess repositoryAccess) {
        log.info("Obtaining file-level textual diff...");
        DiffRange textualDiffs = new DiffRange();
        Iterable<RevCommit> commits = getCommits(repositoryAccess);

        for (RevCommit commit : commits) {
            log.info(commit.getId().name());
            List<DiffEntry> diffEntries = filterDiffEntries(commit);
            if (diffEntries.isEmpty()) continue;

            List<SideLocation> leftSideLocations = new LinkedList<>();
            List<SideLocation> rightSideLocations = new LinkedList<>();
            processDiffs(repositoryAccess, diffEntries, commit, leftSideLocations, rightSideLocations, "TextualDiff");

            if (!leftSideLocations.isEmpty() && !rightSideLocations.isEmpty()) {
                textualDiffs.getDiff().put(commit.getId().name(), new PairSideLocation(leftSideLocations, rightSideLocations));
            }
        }

        log.info("finished");
        return textualDiffs;
    }

    public Map<String, Map<String, Map<String,String>>> getSource(final String fileLevelGit) {
        log.info("Obtaining source code...");
        RepositoryAccess repositoryAccess = new RepositoryAccess(Path.of(fileLevelGit));

        Iterable<RevCommit> commits = getCommits(repositoryAccess);
        // commit, pre/post,file, sourceCode
        Map<String, Map<String, Map<String,String>>> sourceCode = new HashMap<>();

        for (RevCommit commit : commits) {
            log.info(commit.getId().name());
            List<DiffEntry> diffEntries = filterDiffEntries(commit);
            if (diffEntries.isEmpty()) continue;
            for(DiffEntry diffEntry:diffEntries){
                String oldPath = diffEntry.getOldPath();
                String newPath = diffEntry.getNewPath();
                RevTree newTree = commit.getTree();
                RevTree oldTree = commit.getParent(0).getTree();

                FileSource oldFile = FileSource.of(oldPath, oldTree, repositoryAccess.getRepository());
                FileSource newFile = FileSource.of(newPath, newTree, repositoryAccess.getRepository());

                if(!sourceCode.containsKey(commit.getId().name())){
                    sourceCode.put(commit.getId().name(), new HashMap<>());
                }
                if(sourceCode.get(commit.getId().name()).get("preChange")==null){
                    sourceCode.get(commit.getId().name()).put("preChange", new HashMap<>());
                }
                sourceCode.get(commit.getId().name()).get("preChange").put(oldPath, oldFile.getSource());
                if(sourceCode.get(commit.getId().name()).get("postChange")==null){
                    sourceCode.get(commit.getId().name()).put("postChange", new HashMap<>());
                }
                sourceCode.get(commit.getId().name()).get("postChange").put(newPath, newFile.getSource());
            }

        }

        log.info("finished");
//        resetRepositoryAccess();
        return sourceCode;
    }

    private List<DiffEntry> filterDiffEntries(RevCommit commit){
        if (commit.getParents().length == 0) {
            return Collections.emptyList();
        }
        List<DiffEntry> diffEntryList = new LinkedList<>();
        try{
            RevTree newTree = commit.getTree();
            RevTree oldTree = commit.getParent(0).getTree();
            diffEntryList = diffFormatter.scan(oldTree, newTree);
        } catch (IOException e) {
        log.error(e.getMessage(),e);
        }
        return diffEntryList.stream()
                .filter(de -> !(de.getChangeType() == DiffEntry.ChangeType.ADD || de.getChangeType() == DiffEntry.ChangeType.DELETE))
                .filter(de -> (de.getOldPath().endsWith(".java") || de.getNewPath().endsWith(".java") || de.getOldPath().endsWith(".mjava")||de.getNewPath().endsWith(".mjava") ))
                .collect(Collectors.toList());
    }

    private void processDiffs(RepositoryAccess repositoryAccess, List<DiffEntry> diffEntries, RevCommit commit, List<SideLocation> leftSideLocations, List<SideLocation> rightSideLocations, String type) {
        if(type.equals("TreeDiff")){
            for (DiffEntry diffEntry : diffEntries) {
                processTreeDiffEntry(repositoryAccess, diffEntry, commit, leftSideLocations, rightSideLocations);
            }
        } else if (type.equals("TextualDiff")) {
            for (DiffEntry diffEntry : diffEntries) {
                processTextualDiffEntry(repositoryAccess, diffEntry, commit, leftSideLocations, rightSideLocations);
            }
        }

    }

    private void processTreeDiffEntry(RepositoryAccess repositoryAccess, DiffEntry diffEntry, RevCommit commit, List<SideLocation> leftSideLocations, List<SideLocation> rightSideLocations) {
        SrcDstRange treeDiffOnFile = new SrcDstRange();
        String oldPath = diffEntry.getOldPath();
        String newPath = diffEntry.getNewPath();
        RevTree newTree = commit.getTree();
        RevTree oldTree = commit.getParent(0).getTree();
        SourcePair sourcePair = SourcePair.of(FileSource.of(oldPath, oldTree, repositoryAccess.getRepository()),
                FileSource.of(newPath, newTree, repositoryAccess.getRepository()));
        Matcher defaultMatcher =  Matchers.getInstance().getMatcher();
        MappingStore mapping = sourcePair.getMappingStore(defaultMatcher);

        EditScript editScript = editScriptGenerator.computeActions(mapping);
        EditScriptStorer editScriptStorer = new EditScriptStorer(editScript, mapping, sourcePair);
        Map<Tree, Tree> mappings = EditScriptExtractor.mappingStoreToMap(editScriptStorer.getMappingStore());

        for(Action a: editScript){
            SrcDstRange treeActionRanges = actionLocator.getLineRanges(a, mappings, editScriptStorer);
            treeDiffOnFile.getSrcRange().addAll(treeActionRanges.getSrcRange());
            treeDiffOnFile.getDstRange().addAll(treeActionRanges.getDstRange());
        }
        treeDiffOnFile.getSrcRange().asRanges().forEach(p->leftSideLocations.add(new SideLocation(oldPath, p)));
        treeDiffOnFile.getDstRange().asRanges().forEach(p->rightSideLocations.add(new SideLocation(newPath, p)));
    }


    private void processTextualDiffEntry(RepositoryAccess repositoryAccess, DiffEntry diffEntry, RevCommit commit, List<SideLocation> leftSideLocations, List<SideLocation> rightSideLocations) {
        String oldPath = diffEntry.getOldPath();
        String newPath = diffEntry.getNewPath();
        RevTree newTree = commit.getTree();
        RevTree oldTree = commit.getParent(0).getTree();

        FileSource oldFile = FileSource.of(oldPath, oldTree, repositoryAccess.getRepository());
        FileSource newFile = FileSource.of(newPath, newTree, repositoryAccess.getRepository());


        Differencer<String> myers = new JGitDifferencer.Myers<>();
        List<Chunk> chunks = myers.computeDiff(oldFile.getSourceInLines(), newFile.getSourceInLines());
        SrcDstRange srcDstRange = new SrcDstRange();
        for (Chunk chunk : chunks) {
            srcDstRange.getSrcRange().add(chunk.convertToSrcDstRange().get(0));
            srcDstRange.getDstRange().add(chunk.convertToSrcDstRange().get(1));
        }

        srcDstRange.getSrcRange().asRanges().forEach(p->leftSideLocations.add(new SideLocation(oldPath, p)));
        srcDstRange.getDstRange().asRanges().forEach(p->rightSideLocations.add(new SideLocation(newPath, p)));
    }
    private Iterable<RevCommit> getCommits(RepositoryAccess repositoryAccess) {
        return repositoryAccess.walk(null, "HEAD");
    }

//    public void resetRepositoryAccess(final String methodLevelGitPath){
//        this.setRepositoryAccess(new RepositoryAccess(Path.of(methodLevelGitPath)));
//    }


    public static void main(String [] args){
        String methodLevelGitPath = "/Users/leichen/data/parsable_method_level/mbassador/.git";
        RepositoryAccess ra = new RepositoryAccess(Path.of(methodLevelGitPath));

        final DiffFormatter diffFormatter = new DiffFormatter(System.out);
        diffFormatter.setRepository(ra.getRepository());

        EditScriptGenerator editScriptGenerator = new SimplifiedChawatheScriptGenerator();

        ActionLocator actionLocator = new ActionLocator();

        CommitMapper commitMapper = new CommitMapper("/Users/leichen/project/semantic_lifter/SemanticLifter/mined/2024.4.10_lut/commitMap/mbassador.json");
        MethodLevelConvertor methodLevelConvertor = new MethodLevelConvertor(commitMapper);

        DiffProcessor diffProcessor = new DiffProcessor(diffFormatter, editScriptGenerator, actionLocator);
        RepositoryAccess methodLevelRepositoryAccess = new RepositoryAccess(Path.of(methodLevelGitPath));
//        DiffRange treeDiff =  diffProcessor.getTreeDiff();
        // refresh the ra
        methodLevelRepositoryAccess = new RepositoryAccess(Path.of(methodLevelGitPath));
        DiffRange textualDiff =  diffProcessor.getTextualDiff(methodLevelRepositoryAccess, methodLevelConvertor, methodLevelGitPath);
//        DiffRange intersected =  DiffRange.intersection(treeDiff, textualDiff);
//        Map<String, CommitChange> intersected_range =  intersected.extractRange();


//        try (BufferedWriter writer = new BufferedWriter(new FileWriter("./intersected_range"))) {
//            writer.write(intersected_range.toString());
//        } catch (IOException e) {
//            System.err.println("An error occurred while writing to the file: " + e.getMessage());
//        }

    }
}