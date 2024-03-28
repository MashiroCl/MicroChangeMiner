package org.mashirocl.dao;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.jgit.diff.DiffEntry;
import org.mashirocl.refactoringminer.Refactoring;
import org.mashirocl.refactoringminer.SideLocation;

import java.util.LinkedList;
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
    private List<SideLocationDAO> leftSideLocations;
    @JsonProperty("rightSideLocations")
    private List<SideLocationDAO> rightSideLocations;
    @JsonProperty("description")
    private String description;

    public RefactoringDAO(Refactoring refactoring){
        type = refactoring.getType();
        description = refactoring.getDescription();
        leftSideLocations = new LinkedList<>();
        rightSideLocations = new LinkedList<>();
        refactoring.getLeftSideLocations().forEach(p->leftSideLocations.add(new SideLocationDAO(p)));
        refactoring.getRightSideLocations().forEach(p->rightSideLocations.add(new SideLocationDAO(p)));
    }
}
