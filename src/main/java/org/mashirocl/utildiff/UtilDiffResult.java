package org.mashirocl.utildiff;

import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * @author mashirocl@gmail.com
 * @since 2024/05/26 16:27
 */

@Getter
@Setter
public class UtilDiffResult {
    private Map<Path, List<Integer>> added;
    private Map<Path, List<Integer>> removed;
    private Map<Path, List<Integer>> modifiedLeft;
    private Map<Path, List<Integer>> modifiedRight;

    public UtilDiffResult(){
        added = new HashMap<>();
        removed = new HashMap<>();
        modifiedLeft = new HashMap<>();
        modifiedRight = new HashMap<>();
    }

    public UtilDiffResult(FileDiffLocation fileDiffLocation, Path oldPath, Path newPath){
        added = new HashMap<>();
        removed = new HashMap<>();
        modifiedLeft = new HashMap<>();
        modifiedRight = new HashMap<>();
        added.put(newPath, fileDiffLocation.getAdded());
        removed.put(oldPath, fileDiffLocation.getRemoved());
        modifiedLeft.put(oldPath, fileDiffLocation.getModifiedLeft());
        modifiedRight.put(newPath, fileDiffLocation.getModifiedRight());
    }
}