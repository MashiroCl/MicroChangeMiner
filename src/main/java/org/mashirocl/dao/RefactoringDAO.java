package org.mashirocl.dao;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.jgit.diff.DiffEntry;
import org.mashirocl.refactoringminer.Refactoring;
import org.mashirocl.refactoringminer.SideLocation;

import java.util.List;

/**
 * @author mashirocl@gmail.com
 * @since 2024/03/18 15:26
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefactoringDAO {
    @JsonProperty("type")
    private String type;
    @JsonProperty("leftSideLocations")
    private List<SideLocation> leftSideLocations;
    @JsonProperty("rightSideLocations")
    private List<SideLocation> rightSideLocations;

    public RefactoringDAO(Refactoring refactoring){
        type = refactoring.getType();
        leftSideLocations = refactoring.getLeftSideLocations();
        rightSideLocations = refactoring.getRightSideLocations();
    }
}
