package org.mashirocl.visualize;

import org.mashirocl.refactoringminer.SideLocation;

import java.util.List;

/**
 * @author mashirocl@gmail.com
 * @since 2024/04/24 15:06
 */
public class PairSideLocation {
    public List<SideLocation> leftSideLocations;
    public List<SideLocation> rightSideLocations;

    public PairSideLocation(List<SideLocation> leftSideLocations, List<SideLocation> rightSideLocations) {
        this.leftSideLocations = leftSideLocations;
        this.rightSideLocations = rightSideLocations;
    }

    @Override
    public String toString() {
        return "leftSideLocations: " + leftSideLocations.toString() + "\n" + "rightSideLocations: " + rightSideLocations;
    }
}