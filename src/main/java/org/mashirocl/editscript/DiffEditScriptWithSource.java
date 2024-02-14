package org.mashirocl.editscript;

import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.Tree;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.eclipse.jgit.revwalk.RevTree;
import org.mashirocl.source.FileSource;
import org.mashirocl.source.SourcePair;
import org.mashirocl.util.RepositoryAccess;

import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/17 10:50
 */

@AllArgsConstructor
@Getter
public class DiffEditScriptMapping{
    private final DiffEditScript diffEditScript;
    private EditScriptMapping editScriptMapping;

    public static DiffEditScriptMapping of(final DiffEditScript diffEditScript, final EditScriptMapping editScriptMapping){
        return new DiffEditScriptMapping(diffEditScript, editScriptMapping);
    }



}
