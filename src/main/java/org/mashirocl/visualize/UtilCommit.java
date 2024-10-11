package org.mashirocl.visualize;

import lombok.Getter;
import lombok.Setter;
import org.mashirocl.dao.MicroChangeDAO;
import org.mashirocl.dao.RefactoringDAO;

import java.util.List;

/**
 * @author mashirocl@gmail.com
 * @since 2024/06/08 6:56
 */

@Getter
@Setter
public class UtilCommit extends Commit{
    private ChangeRanges addition;
    private ChangeRanges removal;
    private ChangeRanges modification;

    public UtilCommit(String repository, String sha1, String url, List<MicroChangeDAO> microChanges, List<RefactoringDAO> refactorings){
        super(repository, sha1, url, microChanges, refactorings);
        addition = new ChangeRanges();
        removal = new ChangeRanges();
        modification = new ChangeRanges();
    }

}
