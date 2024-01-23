package org.mashirocl.dao;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.mashirocl.microchange.MicroChange;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/22 15:05
 */

@Getter
@Setter
@ToString
@NoArgsConstructor(force = true)
public class MinedMicroChange {
    @JsonProperty("repository")
    private final String repository;
    @JsonProperty("commitID")
    private String commitID;
    @JsonProperty("oldPath")
    private final String oldPath;
    @JsonProperty("newPath")
    private final String newPath;
    @JsonProperty("microChange")
    private final MicroChange microChange;

    public MinedMicroChange(String repository, String commitID, String oldPath, String newPath, MicroChange microChange) {
        this.repository = repository;
        this.commitID = simplifyCommitID(commitID);
        this.oldPath = oldPath;
        this.newPath = newPath;
        this.microChange = microChange;
    }

    private String simplifyCommitID(String commitID){
        assert commitID != null;
        return commitID.split(" ")[1];
    }
}
