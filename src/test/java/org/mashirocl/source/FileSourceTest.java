package org.mashirocl.source;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.junit.jupiter.api.Test;
import org.mashirocl.editscript.EditScriptExtractor;
import org.mashirocl.util.RepositoryAccess;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/10 13:11
 */
class FileSourceTest {
    private final String TestRepositoryPath = "/Users/leichen/data/OSS/my-refactoring-toy-example/.git";
    private final RepositoryAccess ra = new RepositoryAccess(Path.of(TestRepositoryPath));

    private final String endCommitID = "HEAD";
    private final String startCommitID = "cb98b3ede192736b48b18c4bf3b824758b6c4f7a";

    private final DiffFormatter diffFormatter = new DiffFormatter(System.out);

    @Test
    void getSource() throws IOException {
        diffFormatter.setRepository(ra.getRepository());
        Iterable<RevCommit> walk = ra.walk(startCommitID, endCommitID);
        for (RevCommit commit : walk) {
            RevTree newTree = commit.getTree();
            RevTree oldTree = commit.getParent(0).getTree();
            List<DiffEntry> diffEntryList = diffFormatter.scan(oldTree, newTree);
            System.out.printf("commit id is %s", commit.getId());
            for (DiffEntry diffEntry : diffEntryList) {
                String oldPath = diffEntry.getOldPath();
                String newPath = diffEntry.getNewPath();
                FileSource oldFile = FileSource.of(oldPath, oldTree, ra.getRepository());
                FileSource newFile = FileSource.of(newPath, newTree, ra.getRepository());
                System.out.println("old File as follows:");
                System.out.println(oldFile.getSource());
                System.out.println("new File as follows");
                System.out.println(newFile.getSource());
                System.out.println("edit script as follows");
                var v = EditScriptExtractor.getEditScriptMapping(oldFile.getSource(), newFile.getSource()).getEditScript();
                System.out.println(v);
            }

        }
    }
}