package org.mashirocl.refactoringminer;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.diff.DiffEntry;

import java.util.LinkedList;
import java.util.List;

/**
 * @author mashirocl@gmail.com
 * @since 2024/03/06 15:49
 */

@Getter
@Setter
@ToString
@Slf4j
@AllArgsConstructor
public class Refactoring {
    private String type;
    private List<SideLocation> leftSideLocations = new LinkedList<>();
    private List<SideLocation> rightSideLocations = new LinkedList<>();

    public Refactoring(JsonNode refactoringNode){
        type = refactoringNode.get("type").toString();
        for(JsonNode leftSideLocation: refactoringNode.get("leftSideLocations")){
            leftSideLocations.add(new SideLocation(leftSideLocation));
        }
        for(JsonNode rightSideLocation: refactoringNode.get("rightSideLocations")){
            rightSideLocations.add(new SideLocation(rightSideLocation));
        }
    }
}

