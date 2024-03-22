package org.mashirocl.microchange;

import com.google.common.collect.Range;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.jgit.diff.DiffEntry;
import org.mashirocl.refactoringminer.SideLocation;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * @author mashirocl@gmail.com
 * @since 2024/03/19 14:08
 */


@Getter
@Setter
@NoArgsConstructor
public class MicroChangeFileSpecified extends MicroChange{
    private List<SideLocation> leftSideLocations;
    private List<SideLocation> rightSideLocations;

    public MicroChangeFileSpecified(MicroChange microChange, List<SideLocation> leftSideLocations, List<SideLocation> rightSideLocations){
        super(microChange.getType(), microChange.getAction());
        this.leftSideLocations = leftSideLocations;
        this.rightSideLocations = rightSideLocations;
    }

    public MicroChangeFileSpecified(String type, String action, List<SideLocation> leftSideLocations, List<SideLocation> rightSideLocations){
        super(type, action);
        this.leftSideLocations = leftSideLocations;
        this.rightSideLocations = rightSideLocations;
    }

    // micro-change in single file
    public MicroChangeFileSpecified(MicroChange microChange, DiffEntry diffEntry){
        super(microChange.getType(), microChange.getAction());
        this.leftSideLocations = new LinkedList<>();
        this.rightSideLocations = new LinkedList<>();
        if(!microChange.getSrcDstRange().getSrcRange().isEmpty()){
            this.leftSideLocations.add(new SideLocation(diffEntry.getOldPath(), microChange.getSrcDstRange().getSrcRange().asRanges().iterator().next()));
        }
        if(!microChange.getSrcDstRange().getDstRange().isEmpty()){
            this.rightSideLocations.add(new SideLocation(diffEntry.getNewPath(), microChange.getSrcDstRange().getDstRange().asRanges().iterator().next()));
        }
    }

    public String toString(){
        return "type: "+ getType() + "\naction: "+getAction() + "\nleftSideLocations: "+getLeftSideLocations()+"\nrightSideLocations: "+getRightSideLocations();
    }

}
