package org.mashirocl.visualize;

import lombok.*;
import org.mashirocl.dao.MicroChangeDAO;
import org.mashirocl.dao.RefactoringDAO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/04/22 14:39
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Commit {
    private String repository;
    private String sha1;
    private String url;
    private Map<String, String> preChangeSourceCode;
    private Map<String, String> postChangeSourceCode;
    private ChangeRanges fullChangeRanges; // change ranges for file-level textual diff
    private ChangeRanges changeRanges; // change ranges for method-level textual diff intersect tree diff
    private List<MicroChangeDAO> microChanges;
    private List<RefactoringDAO> refactorings;

    public Commit(String repository, String sha1, String url, List<MicroChangeDAO> microChanges, List<RefactoringDAO> refactorings){
        this.repository = repository;
        this.sha1 = sha1;
        this.url = url;
        this.preChangeSourceCode = new HashMap<>();
        this.postChangeSourceCode = new HashMap<>();
        this.fullChangeRanges = new ChangeRanges();
        this.changeRanges = new ChangeRanges();
        this.microChanges = microChanges;
        this.refactorings = refactorings;
    }


    public Commit(String repository, String sha1, String url,Map<String, String> preChangeSourceCode, Map<String, String> postChangeSourceCode, List<MicroChangeDAO> microChanges, List<RefactoringDAO> refactorings){
        this.repository = repository;
        this.sha1 = sha1;
        this.url = url;
        this.preChangeSourceCode = preChangeSourceCode;
        this.postChangeSourceCode = postChangeSourceCode;
        this.changeRanges = new ChangeRanges();
        this.microChanges = microChanges;
        this.refactorings = refactorings;
    }
}
