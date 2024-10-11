package org.mashirocl.utildiff;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

/**
 * @author mashirocl@gmail.com
 * @since 2024/06/08 3:45
 */

@Getter
@Setter
public class FileDiffLocation {
    List<Integer> added;
    List<Integer> removed;
    List<Integer> modifiedLeft;
    List<Integer> modifiedRight;

    public FileDiffLocation(){
        added = new LinkedList<>();
        removed = new LinkedList<>();
        modifiedLeft = new LinkedList<>();
        modifiedRight = new LinkedList<>();
    }
}
