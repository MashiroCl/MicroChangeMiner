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
@AllArgsConstructor
public class MicroChangeDAO {
    @JsonProperty("type")
    private String type;
    // micro change types now will have only one set of left/right side location, set it as a list for the future
    @JsonProperty("leftSideLocations")
    private List<SideLocation> leftSideLocations;
    @JsonProperty("rightSideLocations")
    private List<SideLocation> rightSideLocations;

    public MicroChangeDAO(MicroChangeFileSpecified microChange){
        type = microChange.getType();
        leftSideLocations = microChange.getLeftSideLocations();
        rightSideLocations = microChange.getRightSideLocations();
    }
}
