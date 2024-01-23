package org.mashirocl.editscript;

import com.github.gumtreediff.actions.EditScript;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.jgit.diff.DiffEntry;
import org.mashirocl.source.FileSource;
import org.mashirocl.source.SourcePair;

import java.util.HashMap;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/10 15:33
 */
@AllArgsConstructor
@Getter
@Setter
@ToString
public class DiffEditScript {
    private final DiffEntry diffEntry;
    private final EditScript editScript;
    public static DiffEditScript of (final DiffEntry diffEntry, final EditScript editScript){
        return new DiffEditScript(diffEntry, editScript);
    }
}
