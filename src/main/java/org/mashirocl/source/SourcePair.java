package org.mashirocl.source;

import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.Tree;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.revwalk.RevTree;
import org.mashirocl.util.RepositoryAccess;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/10 10:38
 * source files in the before and after snapshot of a commit
 */

@Slf4j
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SourcePair{
    private FileSource src;
    private FileSource dst;

    public static SourcePair of(final FileSource src, final FileSource dst){
        return new SourcePair(src, dst);
    }

    public MappingStore getMappingStore(Matcher matcher){
        try {
            return matcher.match(new JdtTreeGenerator().generateFrom().string(src.getSource()).getRoot(),
                    new JdtTreeGenerator().generateFrom().string(dst.getSource()).getRoot());
        }
        catch (IOException e){
            log.error("Error in get MappingStore for {} {}\n", src.getFilePath(), dst.getFilePath());
            return null;
        }
    }

    public static MappingStore getMappingStore(String src, String dst, Matcher matcher){
        try {
            return matcher.match(new JdtTreeGenerator().generateFrom().string(src).getRoot(),
                    new JdtTreeGenerator().generateFrom().string(dst).getRoot());
        }
        catch (IOException e){
            log.error("Error in get MappingStore for src: {}\n dst: {}\n", src, dst);
            return null;
        }
    }

    public Map<Tree, Tree> getMappings(Matcher matcher){
        Map<Tree,Tree> map = new HashMap<>();
        for (Mapping cur : getMappingStore(matcher)) {
            map.put(cur.first, cur.second);
        }
        return map;
    }

    public static Map<Tree, Tree> getMappings(String src, String dst, Matcher matcher){
        HashMap<Tree,Tree> map = new HashMap<>();
        for (Mapping cur : Objects.requireNonNull(getMappingStore(src, dst, matcher))) {
            map.put(cur.first, cur.second);
        }
        return map;
    }

    public static Map<Tree, Tree> getMappings(MappingStore mappingStore){
        HashMap<Tree,Tree> map = new HashMap<>();
        for (Mapping cur : mappingStore) {
            map.put(cur.first, cur.second);
        }
        return map;
    }

}
