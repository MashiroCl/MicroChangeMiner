package org.mashirocl.refactoringminer;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Range;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.jgit.diff.DiffEntry;

import java.nio.file.Path;

/**
 * @author mashirocl@gmail.com
 * @since 2024/03/11 20:24
 */

@AllArgsConstructor
@Getter
@Setter
@ToString
public class SideLocation{
    private Path path;
    private Range<Integer> range;

    public SideLocation(JsonNode sideLocation) {
        path = Path.of(sideLocation.get("filePath").toString());
        range = Range.closed(sideLocation.get("startLine").intValue(), sideLocation.get("endLine").intValue());
    }

    public SideLocation(String path, Range<Integer> range){
        this.path = Path.of(path);
        this.range = range;
    }

}
