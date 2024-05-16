package org.mashirocl.visualize;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.RangeSet;
import lombok.*;

import java.util.HashMap;

/**
 * @author mashirocl@gmail.com
 * @since 2024/04/24 17:29
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ChangeRanges {
    /**
     * {File:{ranges of change}}
     */
    @JsonProperty("leftSide")
    private HashMap<String, RangeSet<Integer>> leftSide;
    @JsonProperty("rightSide")
    private HashMap<String, RangeSet<Integer>> rightSide;

}
