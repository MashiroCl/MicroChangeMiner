package org.mashirocl.editscript;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.matchers.MappingStore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/17 14:45
 */


@Deprecated
@AllArgsConstructor
@Getter
public class EditScriptMapping {
    private final EditScript editScript;
    private final MappingStore mappingStore;

    public static EditScriptMapping of(final EditScript editScript, final MappingStore mappingStore){
        return new EditScriptMapping(editScript, mappingStore);
    }
}
