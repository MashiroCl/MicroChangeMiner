package org.mashirocl.textualdiff;

/**
 * @author mashirocl@gmail.com
 * @since 2024/03/23 16:29
 */
import java.util.List;

public interface Differencer<T> {
    List<Chunk> computeDiff(List<T> source, List<T> target);
}