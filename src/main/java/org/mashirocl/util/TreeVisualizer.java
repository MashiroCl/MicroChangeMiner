package org.mashirocl.util;

import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.tree.Tree;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.mashirocl.editscript.DiffEditScriptMapping;
import org.mashirocl.source.FileSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/20 14:37
 */
public class TreeVisualizer {

    final static DiffFormatter diffFormatter;

    static {
        diffFormatter = new DiffFormatter(System.out);
    }


    public static Tree getTree(String sourceCode) throws IOException {
        return new JdtTreeGenerator().generateFrom().string(sourceCode).getRoot();
    }

    public static Tree [] visualizeTree(RepositoryAccess ra, String targetCommitID){
        diffFormatter.setRepository(ra.getRepository());
        Iterable<RevCommit> walk = ra.walk(null, "HEAD");
        try{
            for(RevCommit commit:walk){
                if(commit.getId().toString().contains(targetCommitID)){
                    if (commit.getParents().length == 0) continue;
                    RevTree newTree = commit.getTree();
                    RevTree oldTree = commit.getParent(0).getTree();
                    List<DiffEntry> diffEntryList = diffFormatter.scan(newTree, oldTree);
                    for (DiffEntry diffEntry : diffEntryList) {
                        // exclude non-source code file (on both file-level/method-level)
                        String oldPath = diffEntry.getOldPath();
                        String newPath = diffEntry.getNewPath();
                        if(!oldPath.contains(".java") &&
                                !newPath.contains(".java") &&
                                !oldPath.contains(".mjava") &&
                                !newPath.contains(".mjava"))
                            break;
                        Tree beforeChangeTree = getTree(FileSource.of(oldPath, oldTree, ra.getRepository()).getSource());
                        Tree afterChangeTree = getTree(FileSource.of(newPath, newTree, ra.getRepository()).getSource());
                        return new Tree[]{beforeChangeTree, afterChangeTree};
                    }
                }

                }
            } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public static void main(String [] args){
        final String TestRepositoryPath = "/Users/leichen/data/parsable_method_level/my-refactoring-toy-example/.git";
        final RepositoryAccess ra = new RepositoryAccess(Path.of(TestRepositoryPath));
        Tree [] trees = visualizeTree(ra, "d882eae913e9b7ee6b3634b61ac6108af06ad7e7");
        if(trees.length==0){
            System.out.println("commit not exists");
            return;
        }
        System.out.println("---------------------------------before Tree---------------------------------");
        System.out.println(trees[0].toTreeString());
        System.out.println("---------------------------------after Tree---------------------------------");
        System.out.println(trees[1].toTreeString());
    }

}
