package org.mashirocl.dao;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.mashirocl.refactoringminer.SideLocation;

/**
 * @author mashirocl@gmail.com
 * @since 2024/03/18 15:28
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SideLocationDAO {
    @JsonProperty("path")
    private String filePath;
    @JsonProperty("startLine")
    private int startLine;
    @JsonProperty("endLine")
    private int endLine;

    public SideLocationDAO(String filePath, Range<Integer> range){
        this.filePath = filePath;
        startLine = range.lowerEndpoint();
        endLine = range.upperEndpoint();
    }

    public SideLocationDAO(SideLocation sideLocation){
        filePath = sideLocation.getPath().toString();
        startLine = sideLocation.getRange().lowerEndpoint();
        endLine = sideLocation.getRange().upperEndpoint();
    }

    public String toString(){
        return filePath+"["+startLine+","+endLine+"]";
    }

}
