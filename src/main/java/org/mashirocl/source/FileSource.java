package org.mashirocl.source;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/10 11:03
 * souce code of a file at a snapshot
 */

@Slf4j
@AllArgsConstructor
@Getter
@Setter
public class FileSource {
    private final String filePath;
    private final RevTree tree;
    private final Repository repo;

    public String getSource(){
        TreeWalk treeWalk = new TreeWalk(repo);
        String source = "";
        try {
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            treeWalk.setFilter(PathFilter.create(filePath));
            if(treeWalk.next()) {
                source = new String(repo.open(treeWalk.getObjectId(0))
                        .getBytes(), StandardCharsets.UTF_8);
            }
        } catch (final IOException e){
            log.error(e.getMessage(), e);
        }
        treeWalk.close();
        return source;
    }

    public List<String> getSourceInLines(){
        return Arrays.asList(getSource().split("\\R"));

    }

    public static FileSource of(final String filePath, final RevTree tree, final Repository repo){
        return new FileSource(filePath,tree,repo);
    }

    public static boolean isExtension(final Path path, final String extension){
        final String fileName =  path.getFileName().toString();
        int dotIndex = path.getFileName().toString().lastIndexOf('.');
        if(dotIndex>0 && dotIndex<fileName.length()-1){
            return fileName.substring(dotIndex).equals(extension);
        }
        return false;
    }

}
