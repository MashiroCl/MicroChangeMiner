package org.mashirocl.location;

import com.github.gumtreediff.tree.Tree;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.mashirocl.microchange.SrcDstRange;

/**
 * @author mashirocl@gmail.com
 * @since 2024/02/25 9:53
 */
public class RangeOperations {
    public static Range<Integer> toRange(Tree node) {
        return Range.closedOpen(node.getPos(), node.getEndPos());
    }

    public static Range<Integer> toLineRange(Range<Integer> range, CompilationUnit cu) {
        return Range.closed(cu.getLineNumber(range.lowerEndpoint()), cu.getLineNumber(range.upperEndpoint()));
    }

    public static RangeSet<Integer> toLineRange(RangeSet<Integer> ranges, CompilationUnit cu) {
        return TreeRangeSet.create(ranges.asRanges().stream().map(r -> toLineRange(r, cu)).toList());
    }

    public static SrcDstRange toLineRange(SrcDstRange srcDstRange, CompilationUnit srcCU, CompilationUnit dstCU){
        return new SrcDstRange(toLineRange(srcDstRange.getSrcRange(), srcCU), toLineRange(srcDstRange.getDstRange(),dstCU));
    }

    public static Range<Integer> firstPositiontoRange(Tree node) {
        return Range.closedOpen(node.getPos(), node.getPos()+1);
    }

    public static RangeSet<Integer> deepCopyRangeSet(RangeSet<Integer> original) {
        RangeSet<Integer> copy = TreeRangeSet.create();
        for (Range<Integer> range : original.asRanges()) {
            copy.add(range);
        }
        return copy;
    }
}
