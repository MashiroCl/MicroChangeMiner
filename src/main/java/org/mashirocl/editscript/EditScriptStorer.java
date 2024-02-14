package org.mashirocl.editscript;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.mashirocl.source.SourcePair;

/**
 * @author mashirocl@gmail.com
 * @since 2024/02/11 17:03
 */
@AllArgsConstructor
@Getter
public class EditScriptStorer{
    private final EditScript editScript;
    private final MappingStore mappingStore;
    private final CompilationUnit srcCompilationUnit;
    private final CompilationUnit dstCompilationUnit;

    public static EditScriptStorer of(final EditScript editScript,
                                      final MappingStore mappingStore,
                                      final CompilationUnit srcCompilationUnit,
                                      final CompilationUnit dstCompilationUnit){
        return new EditScriptStorer(editScript, mappingStore, srcCompilationUnit, dstCompilationUnit);
    }

    public static EditScriptStorer of(final EditScript editScript,
                                      final MappingStore mappingStore,
                                      final SourcePair sourcePair){
        return new EditScriptStorer(editScript, mappingStore, sourcePair.getSrcCompilationUnit(), sourcePair.getDstCompilationUnit());
    }

}
