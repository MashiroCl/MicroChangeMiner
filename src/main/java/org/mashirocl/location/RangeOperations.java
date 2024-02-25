package org.mashirocl.location;

import com.github.gumtreediff.tree.Tree;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import org.eclipse.jdt.core.dom.CompilationUnit;

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
}
