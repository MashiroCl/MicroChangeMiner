package org.mashirocl.microchange;

import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import lombok.*;

/**
 * @author mashirocl@gmail.com
 * @since 2024/02/20 10:47
 */



@Getter
@Setter
@ToString
@AllArgsConstructor
public class SrcDstRange {
    private RangeSet<Integer> srcRange;
    private RangeSet<Integer> dstRange;

    public SrcDstRange(){
        srcRange = TreeRangeSet.create();
        dstRange = TreeRangeSet.create();
    }

    public boolean isEmpty(){
        return srcRange.isEmpty() && dstRange.isEmpty();
    }
}
