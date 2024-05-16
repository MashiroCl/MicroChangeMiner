package org.mashirocl.dao;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.mashirocl.microchange.MicroChangeFileSpecified;
import org.mashirocl.refactoringminer.SideLocation;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * @author mashirocl@gmail.com
 * @since 2024/03/18 15:26
 */

@Getter
@Setter
@NoArgsConstructor
@ToString
public class MicroChangeDAO {
    @JsonProperty("type")
    private String type;
    // micro change types now will have only one set of left/right side location, set it as a list for the future
    @JsonProperty("leftSideLocations")
    private List<SideLocationDAO> leftSideLocations;
    @JsonProperty("rightSideLocations")
    private List<SideLocationDAO> rightSideLocations;
    @JsonProperty("action")
    private String action;

    public MicroChangeDAO(MicroChangeFileSpecified microChange){
        type = microChange.getType();
        action = microChange.getAction();
        leftSideLocations = new LinkedList<>();
        rightSideLocations = new LinkedList<>();
        microChange.getLeftSideLocations().forEach(p->leftSideLocations.add(new SideLocationDAO(p)));
        microChange.getRightSideLocations().forEach(p->rightSideLocations.add(new SideLocationDAO(p)));
    }

    public MicroChangeDAO(String type, String action, List<SideLocation> leftSideLocations, List<SideLocation> rightSideLocations){
        this.type = type;
        this.action = action;
        this.leftSideLocations = new LinkedList<>();
        this.rightSideLocations = new LinkedList<>();
        leftSideLocations.forEach(p->this.leftSideLocations.add(new SideLocationDAO(p)));
        rightSideLocations.forEach(p->this.rightSideLocations.add(new SideLocationDAO(p)));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MicroChangeDAO microChangeDAO = (MicroChangeDAO) o;
        return Objects.equals(toString(), microChangeDAO.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(toString());
    }
}
