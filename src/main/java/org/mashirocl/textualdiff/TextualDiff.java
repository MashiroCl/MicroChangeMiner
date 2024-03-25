package org.mashirocl.textualdiff;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.mashirocl.microchange.SrcDstRange;
import org.mashirocl.source.FileSource;
import org.mashirocl.util.RepositoryAccess;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * @author mashirocl@gmail.com
 * @since 2024/03/23 17:11
 */
@Slf4j
public class TextualDiff {

    public static Differencer<String> myers = new JGitDifferencer.Myers<>();

    public static void main(String[] args) {
        final RepositoryAccess ra = new RepositoryAccess(Path.of("/Users/leichen/data/parsable_method_level/my-refactoring-toy-example/.git"));
        final DiffFormatter diffFormatter = new DiffFormatter(System.out);
        diffFormatter.setRepository(ra.getRepository());
        System.out.println(getTextualDiff(ra, diffFormatter));
    }

    public static Map<String,  Map<String, SrcDstRange>> getTextualDiff(RepositoryAccess ra, DiffFormatter diffFormatter) {
        // commit: file: SrcDstRange
        log.info("Computing textual diff...");
        Map<String,  Map<String, SrcDstRange>> res = new HashMap<>();
        Iterable<RevCommit> walk = ra.walk(null, "HEAD");
        try {
            for (RevCommit commit : walk) {
                if (commit.getParents().length == 0) {
                    continue;
                }
                RevTree newTree = commit.getTree();
                RevTree oldTree = commit.getParent(0).getTree();
                List<DiffEntry> diffEntryList = diffFormatter.scan(oldTree, newTree);
                // skip the commits which are purely addition or deletion
                if (diffEntryList.stream().allMatch(p -> p.getChangeType() == DiffEntry.ChangeType.ADD)
                        || diffEntryList.stream().allMatch(p -> p.getChangeType() == DiffEntry.ChangeType.DELETE)) {
                    continue;
                }
                res.put(commit.getName(), new HashMap<>());
                for (DiffEntry diffEntry : diffEntryList) {
                    // exclude non-source code file (on both file-level/method-level)
                    String oldPath = diffEntry.getOldPath();
                    String newPath = diffEntry.getNewPath();
                    if (!oldPath.contains(".java") &&
                            !newPath.contains(".java") &&
                            !oldPath.contains(".mjava") &&
                            !newPath.contains(".mjava")) {
                        continue;
                    }

                    // skip the file which has only addition/ deletion
                    if (diffEntry.getChangeType() == DiffEntry.ChangeType.ADD
                            || diffEntry.getChangeType() == DiffEntry.ChangeType.DELETE)
                        continue;

                    FileSource oldFile = FileSource.of(oldPath, oldTree, ra.getRepository());
                    FileSource newFile = FileSource.of(newPath, newTree, ra.getRepository());
                    List<Chunk> chunks = myers.computeDiff(oldFile.getSourceInLines(), newFile.getSourceInLines());
                    SrcDstRange srcDstRange = new SrcDstRange();
                    for (Chunk chunk : chunks) {
                        srcDstRange.getSrcRange().add(chunk.convertToSrcDstRange().get(0));
                        srcDstRange.getDstRange().add(chunk.convertToSrcDstRange().get(1));
                    }
                    res.get(commit.getName()).put(diffEntry.getOldPath(),srcDstRange);
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return res;
    }
}
