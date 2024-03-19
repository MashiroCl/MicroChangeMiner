package org.mashirocl.dao;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * @author mashirocl@gmail.com
 * @since 2024/03/18 15:24
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CommitDAO {
    @JsonProperty("repository")
    private String repository;
    @JsonProperty("sha1")
    private String sha1;
    @JsonProperty("url")
    private String url;

    @JsonProperty("microChanges")
    private List<MicroChangeDAO> microChanges;
    @JsonProperty("refactorings")
    private List<RefactoringDAO> refactorings;

}
