package org.mashirocl.textualdiff;

import com.google.common.collect.Range;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.Arrays;
import java.util.List;

/**
 * @author mashirocl@gmail.com
 * @since 2024/03/23 16:30
 */
@RequiredArgsConstructor
@Value
public class Chunk {
    public enum Type {
        MOD, EQL, DEL, INS
    }

    public Type type;
    public int sourceStart;
    public int sourceEnd;
    public int targetStart;
    public int targetEnd;

    public List<Range<Integer>> convertToSrcDstRange(){
        // plus 1 to convert the 0-index to 1-index
        return Arrays.asList(Range.closedOpen(sourceStart+1, sourceEnd+1), Range.closedOpen(targetStart+1, targetEnd+1));
    }

}