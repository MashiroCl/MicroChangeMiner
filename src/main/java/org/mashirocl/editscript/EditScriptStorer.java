package org.mashirocl.editscript;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.matchers.MappingStore;
import com.google.common.collect.Range;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.mashirocl.microchange.SrcDstRange;
import org.mashirocl.source.SourcePair;

import java.io.IOException;

/**
 * @author mashirocl@gmail.com
 * @since 2024/02/11 17:03
 */
@AllArgsConstructor
@Getter
@Slf4j
public class EditScriptStorer{
    private final EditScript editScript;
    private final MappingStore mappingStore;
    private final CompilationUnit srcCompilationUnit;
    private final CompilationUnit dstCompilationUnit;
    private SrcDstRange changedLines;

    public EditScriptStorer(final EditScript editScript,
                            final MappingStore mappingStore,
                            final CompilationUnit srcCompilationUnit,
                            final CompilationUnit dstCompilationUnit){
        this.editScript = editScript;
        this.mappingStore = mappingStore;
        this.srcCompilationUnit = srcCompilationUnit;
        this.dstCompilationUnit = dstCompilationUnit;
        this.changedLines = new SrcDstRange();
    }

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

    public void addChangedLineRanges(DiffFormatter diffFormatter, DiffEntry diffEntry){
        try {
            for(Edit edit: diffFormatter.toFileHeader(diffEntry).toEditList()){
                // edit.getBegin() is 0 based https://archive.eclipse.org/jgit/docs/jgit-2.0.0.201206130900-r/apidocs/org/eclipse/jgit/diff/Edit.html
                changedLines.getSrcRange().add(Range.closedOpen(edit.getBeginA()+1, edit.getEndA()+1));
                changedLines.getDstRange().add(Range.closedOpen(edit.getBeginB()+1, edit.getEndB()+1));
//                log.info("edit {}, add changed lines {}",edit, changedLines);
            }
        }
        catch (IOException e){
            log.error(e.getMessage(), e);
        }


    }
}
