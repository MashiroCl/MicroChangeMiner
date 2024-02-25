package org.mashirocl.editscript;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.Tree;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.mashirocl.source.FileSource;
import org.mashirocl.source.SourcePair;
import org.mashirocl.util.RepositoryAccess;

import java.io.IOException;
import java.util.*;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/10 13:24
 */
@Slf4j
public class EditScriptExtractor {

    public static final Matcher defaultMatcher;
    public static final EditScriptGenerator editScriptGenerator;

    static {
        defaultMatcher = Matchers.getInstance().getMatcher();
        editScriptGenerator = new SimplifiedChawatheScriptGenerator();
    }

    public static EditScriptMapping getEditScriptMapping(SourcePair sourcePair){
            MappingStore mappings = sourcePair.getMappingStore(defaultMatcher);
            return EditScriptMapping.of(editScriptGenerator.computeActions(mappings), mappings);
    }

    /**
     * extract edit scripts contained in the commits (startCommitID, endCommitID] in the repository
     * @param ra
     * @param diffFormatter
     * @param startCommitID
     * @param endCommitID
     * @return {commitID: [DiffEditScript]}
     */
    public static Map<String, List<DiffEditScriptWithSource>> getEditScript(RepositoryAccess ra, DiffFormatter diffFormatter, String startCommitID, String endCommitID){
        log.info("Computing edit script...");
        Map<String, List<DiffEditScriptWithSource>> res = new HashMap<>();
        Iterable<RevCommit> walk = ra.walk(startCommitID, endCommitID);
        int count = 0;
        int processed_method_count = 0;
        try {
            for (RevCommit commit : walk) {
                count++;
                log.info("Getting edit script for {}, #{}", commit.getId().name(), count);
                if (commit.getParents().length == 0) {
                    log.info("parent number is 0, skipped");
                    continue;
                }
                RevTree newTree = commit.getTree();
                RevTree oldTree = commit.getParent(0).getTree();
                List<DiffEntry> diffEntryList = diffFormatter.scan(oldTree, newTree);
                // skip the commits which are purely addition or deletion
                if(diffEntryList.stream().allMatch(p->p.getChangeType()==DiffEntry.ChangeType.ADD)
                        || diffEntryList.stream().allMatch(p->p.getChangeType()==DiffEntry.ChangeType.DELETE))
                {
                    log.info("Purely addition or deletion, skipped");
                    continue;
                }
                List<DiffEditScriptWithSource> diffEditScripts = new LinkedList<>();

                for (DiffEntry diffEntry : diffEntryList) {

                    // exclude non-source code file (on both file-level/method-level)
                    String oldPath = diffEntry.getOldPath();
                    String newPath = diffEntry.getNewPath();
                    if(!oldPath.contains(".java") &&
                            !newPath.contains(".java") &&
                            !oldPath.contains(".mjava") &&
                            !newPath.contains(".mjava")){
                        log.info("Change not to source code file, oldpath: {}, newpath: {}", oldPath, newPath);
                        continue;
                    }

//                    skip the file which has only addition/ deletion
                    if(diffEntry.getChangeType()==DiffEntry.ChangeType.ADD
                            ||diffEntry.getChangeType()==DiffEntry.ChangeType.DELETE)
                        continue;

                    SourcePair sourcePair = SourcePair.of(FileSource.of(oldPath, oldTree, ra.getRepository()),
                            FileSource.of(newPath, newTree, ra.getRepository()));

//                    diffEditScriptList.add(DiffEditScript.of(diffEntry, getEditScript(sourcePair)));
//                    EditScriptMapping editScriptMapping = getEditScriptMapping(sourcePair);

                    MappingStore mapping = sourcePair.getMappingStore(defaultMatcher);

                    EditScript editScript = editScriptGenerator.computeActions(mapping);
                    EditScriptStorerIncludeIf editScriptStorerIncludeIf = new EditScriptStorerIncludeIf(editScript, mapping, sourcePair);
                    editScriptStorerIncludeIf.setSrcDstLineRangeOfIf(sourcePair.locateIfLineRange());
//                    EditScriptStorer editScriptStorer = EditScriptStorer.of(editScript, mapping, sourcePair);
                    editScriptStorerIncludeIf.addChangedLineRanges(diffFormatter, diffEntry);

                    diffEditScripts.add(DiffEditScriptWithSource.of(DiffEditScript.of(diffEntry, editScriptStorerIncludeIf.getEditScript()), editScriptStorerIncludeIf));
                    processed_method_count++;
                }

                res.put(commit.getId().toString(), diffEditScripts);
            }
            log.info("Edit script computed, {} methods processed", processed_method_count);
            return res;
        }
        catch (IOException e){
            log.error(e.getMessage(), e);
        }
        return res;
    }

    public static Map<String, List<DiffEditScriptWithSource>> getEditScript(RepositoryAccess ra, DiffFormatter diffFormatter){
        return getEditScript(ra, diffFormatter, null, "HEAD");
    }

    public static Map<Tree, Tree> mappingStoreToMap(MappingStore mappings){
        HashMap<Tree,Tree> map = new HashMap<>();
        for (Mapping cur : mappings) {
            map.put(cur.first, cur.second);
            map.put(cur.second, cur.first);
        }
        return map;
    }

}
