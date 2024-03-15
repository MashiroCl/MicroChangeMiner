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
    @JsonProperty("type")
    private final String type;
    @JsonProperty("action")
    private final String action;
    @JsonProperty("position")
    private final String position;

    public MinedMicroChange(String repository, String commitID, String oldPath, String newPath, String type, String action, String position) {
        this.repository = repository;
        this.commitID = commitID;
        this.oldPath = oldPath;
        this.newPath = newPath;
        this.type = type;
        this.action = action;
        this.position =position;
    }

    private String simplifyCommitID(String commitID){
        assert commitID != null;
        return commitID.split(" ")[1];
    }
}
