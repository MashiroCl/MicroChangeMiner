package org.mashirocl.dao;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.RangeSet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.checkerframework.checker.units.qual.N;

import java.util.LinkedList;
import java.util.List;

/**
 * @author mashirocl@gmail.com
 * @since 2024/03/20 10:14
 */

@Getter
@Setter
@NoArgsConstructor
public class NotCoveredDAO {
    @JsonProperty("repository")
    private String repository;
    @JsonProperty("sha1")
    private String sha1;
    @JsonProperty("url")
    private String url;
    @JsonProperty("leftSideLocation")
    private List<SideLocationDAO> leftSideLocation;
    @JsonProperty("rightSideLocation")
    private List<SideLocationDAO> rightSideLocation;

    public NotCoveredDAO(String repository, String sha1, String url,String oldPath, String newPath, RangeSet<Integer> leftSideLocation, RangeSet<Integer> rightSideLocation){
        this.repository = repository;
        this.sha1 = sha1;
        this.url = url;
        this.leftSideLocation = new LinkedList<>();
        this.rightSideLocation = new LinkedList<>();
        leftSideLocation.asRanges().forEach(p->this.leftSideLocation.add(new SideLocationDAO(oldPath,p)));
        rightSideLocation.asRanges().forEach(p->this.rightSideLocation.add(new SideLocationDAO(newPath,p)));
    }
}
