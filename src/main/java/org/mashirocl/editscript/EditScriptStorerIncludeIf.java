package org.mashirocl.editscript;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.matchers.MappingStore;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.mashirocl.microchange.SrcDstRange;
import org.mashirocl.source.SourcePair;

/**
 * @author mashirocl@gmail.com
 * @since 2024/02/25 13:38
 */

@Getter
@Setter
public class EditScriptStorerIncludeIf extends EditScriptStorer{
    SrcDstRange srcDstLineRangeOfIf;
    public EditScriptStorerIncludeIf(EditScript editScript, MappingStore mappingStore, CompilationUnit srcCompilationUnit, CompilationUnit dstCompilationUnit, SrcDstRange changedLines) {
        super(editScript, mappingStore, srcCompilationUnit, dstCompilationUnit, changedLines);
    }

    public EditScriptStorerIncludeIf(EditScript editScript, MappingStore mappingStore, CompilationUnit srcCompilationUnit, CompilationUnit dstCompilationUnit) {
        super(editScript, mappingStore, srcCompilationUnit, dstCompilationUnit);
    }

    public EditScriptStorerIncludeIf(EditScript editScript, MappingStore mappingStore, SourcePair sourcePair) {
        super(editScript, mappingStore,sourcePair);
    }

}
