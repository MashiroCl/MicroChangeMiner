package org.mashirocl.visualize;

import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.mashirocl.refactoringminer.SideLocation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/04/24 15:14
 */
@Getter
@Setter
@Slf4j
public class DiffRange {
    private Map<String, PairSideLocation> diff;

    public DiffRange(){
        this.diff = new HashMap<>();
    }

    public static DiffRange intersection(DiffRange diffRangeA, DiffRange diffRangeB){
        log.info("Calculating the intersection...");
        DiffRange res = new DiffRange();
        for(String s:diffRangeA.getDiff().keySet()){
            List<SideLocation> leftSideLocations = new LinkedList<>();
            List<SideLocation> rightSideLocations = new LinkedList<>();

            for(SideLocation sideLocationA: diffRangeA.getDiff().get(s).leftSideLocations){
                for(SideLocation sideLocationB: diffRangeB.getDiff().get(s).leftSideLocations){
                    if(sideLocationA.getPath().equals(sideLocationB.getPath())){
                        if(sideLocationA.getRange().isConnected(sideLocationB.getRange())){
                            leftSideLocations.add(new SideLocation(sideLocationA.getPath(),sideLocationA.getRange().intersection(sideLocationB.getRange())));
                        }
                    }
                }
            }

            for(SideLocation sideLocationA: diffRangeA.getDiff().get(s).rightSideLocations){
                for(SideLocation sideLocationB: diffRangeB.getDiff().get(s).rightSideLocations){
                    if(sideLocationA.getPath().equals(sideLocationB.getPath())){
                        if(sideLocationA.getRange().isConnected(sideLocationB.getRange())){
                            rightSideLocations.add(new SideLocation(sideLocationA.getPath(),sideLocationA.getRange().intersection(sideLocationB.getRange())));
                        }
                    }
                }
            }

            res.getDiff().put(s, new PairSideLocation(leftSideLocations, rightSideLocations));
        }
        return res;
    }


    public Map<String, ChangeRanges> extractRange(){
        log.info("Extracting ranges...");
        Map<String, ChangeRanges> commitChangeMap = new HashMap<>();
        for(String commit:diff.keySet()){
            HashMap<String, RangeSet<Integer>> leftSide = new HashMap<>();
            HashMap<String, RangeSet<Integer>> rightSide = new HashMap<>();
            PairSideLocation pairSideLocation = diff.get(commit);
            for(SideLocation sideLocation:pairSideLocation.leftSideLocations){
                String filePath = sideLocation.getPath().toString();
                if(!leftSide.containsKey(filePath)){
                    leftSide.put(filePath, TreeRangeSet.create());
                }
                leftSide.get(filePath).add(sideLocation.getRange());
            }
            for(SideLocation sideLocation:pairSideLocation.rightSideLocations){
                String filePath = sideLocation.getPath().toString();
                if(!rightSide.containsKey(filePath)){
                    rightSide.put(filePath, TreeRangeSet.create());
                }
                rightSide.get(filePath).add(sideLocation.getRange());
            }
            commitChangeMap.put(commit, new ChangeRanges(leftSide, rightSide));
        }
        return commitChangeMap;
    }


}
