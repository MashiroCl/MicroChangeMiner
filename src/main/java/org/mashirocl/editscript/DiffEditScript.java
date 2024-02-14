package org.mashirocl.editscript;

import com.github.gumtreediff.actions.EditScript;
import lombok.*;
import org.eclipse.jgit.diff.DiffEntry;

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
