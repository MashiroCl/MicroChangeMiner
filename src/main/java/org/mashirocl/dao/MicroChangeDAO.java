package org.mashirocl.dao;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.mashirocl.microchange.MicroChangeFileSpecified;
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
public class MicroChangeDAO {
    @JsonProperty("type")
    private String type;
    // micro change types now will have only one set of left/right side location, set it as a list for the future
    @JsonProperty("leftSideLocations")
    private List<SideLocationDAO> leftSideLocations;
    @JsonProperty("rightSideLocations")
    private List<SideLocationDAO> rightSideLocations;

    public MicroChangeDAO(MicroChangeFileSpecified microChange){
        type = microChange.getType();
        leftSideLocations = new LinkedList<>();
        rightSideLocations = new LinkedList<>();
        microChange.getLeftSideLocations().forEach(p->leftSideLocations.add(new SideLocationDAO(p)));
        microChange.getRightSideLocations().forEach(p->rightSideLocations.add(new SideLocationDAO(p)));
    }

    public MicroChangeDAO(String type, List<SideLocation> leftSideLocations, List<SideLocation> rightSideLocations){
        this.type = type;
        this.leftSideLocations = new LinkedList<>();
        this.rightSideLocations = new LinkedList<>();
        leftSideLocations.forEach(p->this.leftSideLocations.add(new SideLocationDAO(p)));
        rightSideLocations.forEach(p->this.rightSideLocations.add(new SideLocationDAO(p)));
    }
}
