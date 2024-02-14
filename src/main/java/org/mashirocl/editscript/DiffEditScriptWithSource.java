package org.mashirocl.editscript;

import com.github.gumtreediff.actions.EditScript;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.eclipse.jgit.diff.DiffEntry;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/17 10:50
 */

@Getter
public class DiffEditScriptWithSource extends DiffEditScript{
    private final EditScriptStorer editScriptStorer;
    public DiffEditScriptWithSource(final DiffEntry diffEntry, final EditScript editScript, final EditScriptStorer editScriptStorer){
        super(diffEntry, editScript);
        this.editScriptStorer = editScriptStorer;
    }

    public DiffEditScriptWithSource(final DiffEditScript diffEditScript, final EditScriptStorer editScriptStorer){
        super(diffEditScript.getDiffEntry(), diffEditScript.getEditScript());
        this.editScriptStorer = editScriptStorer;
    }

    public static DiffEditScriptWithSource of(final DiffEditScript diffEditScript, final EditScriptStorer editScriptStorer){
        return new DiffEditScriptWithSource(diffEditScript, editScriptStorer);
    }



}
