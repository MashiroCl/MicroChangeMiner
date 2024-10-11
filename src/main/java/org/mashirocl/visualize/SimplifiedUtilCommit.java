package org.mashirocl.visualize;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/06/08 7:44
 */

@Getter
@Setter
@NoArgsConstructor
public class SimplifiedUtilCommit extends SimplifiedCommit{
    @JsonProperty("addition")
    private Map<String, List<List<Integer>>> addition;
    @JsonProperty("removal")
    private Map<String, List<List<Integer>>> removal;
    @JsonProperty("modificationLeft")
    private Map<String, List<List<Integer>>> modificationLeft;
    @JsonProperty("modificationRight")
    private Map<String, List<List<Integer>>> modificationRight;

    public SimplifiedUtilCommit(UtilCommit commit){
        super(commit);
        buildUtilChangeRangeMap(commit);
    }

    private void buildUtilChangeRangeMap(UtilCommit commit){
        this.addition = new HashMap<>();
        this.removal = new HashMap<>();
        this.modificationLeft = new HashMap<>();
        this.modificationRight = new HashMap<>();
        for(String filePath:commit.getAddition().getRightSide().keySet()){
            if(!this.addition.containsKey(filePath)){
                this.addition.put(filePath, new LinkedList<>());
            }
            this.addition.get(filePath).addAll(commit.getAddition().getRightSide().get(filePath).asRanges().stream()
                    .map(this::convertRangeToList).toList());
        }
        for(String filePath:commit.getRemoval().getLeftSide().keySet()){
            if(!this.removal.containsKey(filePath)){
                this.removal.put(filePath, new LinkedList<>());
            }
            this.removal.get(filePath).addAll(commit.getRemoval().getLeftSide().get(filePath).asRanges().stream()
                    .map(this::convertRangeToList).toList());
        }
        for(String filePath:commit.getModification().getLeftSide().keySet()){
            if(!this.modificationLeft.containsKey(filePath)){
                this.modificationLeft.put(filePath, new LinkedList<>());
            }
            this.modificationLeft.get(filePath).addAll(commit.getModification().getLeftSide().get(filePath).asRanges().stream()
                    .map(this::convertRangeToList).toList());
        }
        for(String filePath:commit.getModification().getRightSide().keySet()){
            if(!this.modificationRight.containsKey(filePath)){
                this.modificationRight.put(filePath, new LinkedList<>());
            }
            this.modificationRight.get(filePath).addAll(commit.getModification().getRightSide().get(filePath).asRanges().stream()
                    .map(this::convertRangeToList).toList());
        }
    }

}
