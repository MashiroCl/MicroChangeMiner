package org.mashirocl.visualize;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.mashirocl.dao.MicroChangeDAO;
import org.mashirocl.dao.RefactoringDAO;
import org.mashirocl.dao.SideLocationDAO;
import org.mashirocl.refactoringminer.MethodLevelConvertor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/05/03 22:28
 */
@Getter
@Setter
@NoArgsConstructor
@Slf4j
@ToString
public class SimplifiedCommit {
    @JsonProperty("repository")
    private String repository;
    @JsonProperty("sha1")
    private String sha1;
    @JsonProperty("url")
    private String url;
    @JsonProperty("preChangeSourceCode")
    private Map<String, String> preChangeSourceCode;
    @JsonProperty("postChangeSourceCode")
    private Map<String, String> postChangeSourceCode;
    @JsonProperty("preTexturalChangeRange")
    private Map<String, List<List<Integer>>> preTexturalChangeRange;
    @JsonProperty("postTexturalChangeRange")
    private Map<String, List<List<Integer>>> postTexturalChangeRange;
    @JsonProperty("preChangeRange")
    private Map<String, List<List<Integer>>> preChangeRange;
    @JsonProperty("postChangeRange")
    private Map<String, List<List<Integer>>> postChangeRange;

    @JsonProperty("microChanges")

    private List<SpecialChange> microChanges;
    @JsonProperty("refactorings")

    private List<SpecialChange> refactorings;


    public SimplifiedCommit(Commit commit){
        this.repository = commit.getRepository();
        this.sha1 = commit.getSha1();
        this.url = commit.getUrl();
        this.preChangeSourceCode = commit.getPreChangeSourceCode();
        this.postChangeSourceCode = commit.getPostChangeSourceCode();
        buildChangeRangeMap(commit);
        log.info("commit.getFullChangeRanges() {}", commit.getFullChangeRanges());
        buildTextualChangeRangeMap(commit);
//        for(String filePath:commit.getChangeRanges().getLeftSide().keySet()){
//            String fileLevelFilePath = MethodLevelConvertor.convertMethodLevelFileToFileLevelFile(filePath);
//            if(!this.preChangeRange.containsKey(fileLevelFilePath)){
//                this.preChangeRange.put(fileLevelFilePath, new LinkedList<>());
//            }
//            this.preChangeRange.get(fileLevelFilePath).addAll(commit.getChangeRanges().getLeftSide().get(filePath).asRanges().stream()
//                    .map(r->List.of(r.lowerEndpoint(),r.upperEndpoint())).toList());
//        }
//        for(String filePath:commit.getChangeRanges().getRightSide().keySet()){
//            String fileLevelFilePath = MethodLevelConvertor.convertMethodLevelFileToFileLevelFile(filePath);
//            if(!this.postChangeRange.containsKey(fileLevelFilePath)){
//                this.postChangeRange.put(fileLevelFilePath, new LinkedList<>());
//            }
//            this.postChangeRange.get(fileLevelFilePath).addAll(commit.getChangeRanges().getRightSide().get(filePath).asRanges().stream()
//                    .map(r->List.of(r.lowerEndpoint(),r.upperEndpoint())).toList());
//        }
        this.microChanges = new LinkedList<>();
        commit.getMicroChanges().forEach(p->this.microChanges.add(new SpecialChange(p)));
        this.microChanges.forEach(SpecialChange::convertMethodLevelToFileLevel);
        this.refactorings = new LinkedList<>();
        commit.getRefactorings().forEach(p->this.refactorings.add(new SpecialChange(p)));
        this.refactorings.forEach(SpecialChange::convertMethodLevelToFileLevel);
    }


    private void buildChangeRangeMap(Commit commit){
        this.preChangeRange = new HashMap<>();
        this.postChangeRange = new HashMap<>();
        for(String filePath:commit.getChangeRanges().getLeftSide().keySet()){
            String fileLevelFilePath = MethodLevelConvertor.convertMethodLevelFileToFileLevelFile(filePath);
            if(!this.preChangeRange.containsKey(fileLevelFilePath)){
                this.preChangeRange.put(fileLevelFilePath, new LinkedList<>());
            }
            this.preChangeRange.get(fileLevelFilePath).addAll(commit.getChangeRanges().getLeftSide().get(filePath).asRanges().stream()
                    .map(r->List.of(r.lowerEndpoint(),r.upperEndpoint())).toList());
        }
        for(String filePath:commit.getChangeRanges().getRightSide().keySet()){
            String fileLevelFilePath = MethodLevelConvertor.convertMethodLevelFileToFileLevelFile(filePath);
            if(!this.postChangeRange.containsKey(fileLevelFilePath)){
                this.postChangeRange.put(fileLevelFilePath, new LinkedList<>());
            }
            this.postChangeRange.get(fileLevelFilePath).addAll(commit.getChangeRanges().getRightSide().get(filePath).asRanges().stream()
                    .map(r->List.of(r.lowerEndpoint(),r.upperEndpoint())).toList());
        }
    }

    private void buildTextualChangeRangeMap(Commit commit){
        this.preTexturalChangeRange = new HashMap<>();
        this.postTexturalChangeRange = new HashMap<>();
        for(String filePath:commit.getFullChangeRanges().getLeftSide().keySet()){
            if(!this.preTexturalChangeRange.containsKey(filePath)){
                this.preTexturalChangeRange.put(filePath, new LinkedList<>());
            }
            this.preTexturalChangeRange.get(filePath).addAll(commit.getFullChangeRanges().getLeftSide().get(filePath).asRanges().stream()
                    .map(r->List.of(r.lowerEndpoint(),r.upperEndpoint())).toList());
        }
        for(String filePath:commit.getFullChangeRanges().getRightSide().keySet()){
            if(!this.postTexturalChangeRange.containsKey(filePath)){
                this.postTexturalChangeRange.put(filePath, new LinkedList<>());
            }

            this.postTexturalChangeRange.get(filePath).addAll(commit.getFullChangeRanges().getRightSide().get(filePath).asRanges().stream()
                    .map(r->List.of(r.lowerEndpoint(),r.upperEndpoint())).toList());
        }
    }
}

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
class SpecialChange{
    @JsonProperty("type")
    private String type;
    // micro change types now will have only one set of left/right side location, set it as a list for the future
    @JsonProperty("leftSideLocations")
    private List<SideLocationDAO> leftSideLocations;
    @JsonProperty("rightSideLocations")
    private List<SideLocationDAO> rightSideLocations;

    public SpecialChange(MicroChangeDAO microChangeDAO){
        this.type = microChangeDAO.getType();
        this.leftSideLocations = microChangeDAO.getLeftSideLocations();
        this.rightSideLocations = microChangeDAO.getRightSideLocations();
    }

    public SpecialChange(RefactoringDAO refactoringDAO){
        this.type = refactoringDAO.getType();
        this.leftSideLocations = refactoringDAO.getLeftSideLocations();
        this.rightSideLocations = refactoringDAO.getRightSideLocations();
    }

    public void convertMethodLevelToFileLevel(){
        for(SideLocationDAO sideLocationDAO: leftSideLocations){
            sideLocationDAO.setFilePath(MethodLevelConvertor.convertMethodLevelFileToFileLevelFile(sideLocationDAO.getFilePath()));
        }
        for(SideLocationDAO sideLocationDAO: rightSideLocations){
            sideLocationDAO.setFilePath(MethodLevelConvertor.convertMethodLevelFileToFileLevelFile(sideLocationDAO.getFilePath()));
        }
    }
}
