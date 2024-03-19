package org.mashirocl.refactoringminer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.gumtreediff.tree.Tree;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

/**
 * @author mashirocl@gmail.com
 * @since 2024/03/04 17:12
 */
@Slf4j
@AllArgsConstructor
public class RefactoringLoader {

    /**
     * load refactorings from the output json of RefactoringMiner
     * @param path
     * @return
     */
    public static List<Refactoring> load(Path path){
        List<Refactoring> refactoringList = new LinkedList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(new File(path.toString()));
            // the initial commit
            if(!rootNode.has("commits") || rootNode.get("commits").isEmpty()){
                return refactoringList;
            }
            JsonNode refactorings =  rootNode.get("commits").get(0).get("refactorings");
            for(JsonNode refactoringNode:refactorings){
                refactoringList.add(new Refactoring(refactoringNode));
            }
        } catch (Exception e) {
            log.error("Error for {}, {}", path, e.getMessage(), e);
        }
        return refactoringList;
    }

    /**
     * load the refactoring json files under the certain directory
     * @param directory
     */
    public static Map<String, List<Refactoring>> loadFromDirectory(Path directory) {
        File dir = new File(directory.toString());
        File[] directoryListing = dir.listFiles();
        Map<String, List<Refactoring>> map = new HashMap<>();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                List<Refactoring> refs = load(child.toPath());
                if(!refs.isEmpty()){
                    map.put(child.getName().substring(0,40), refs);
                }
            }
        }
        return map;
    }

    /**
     * Exclude the refactorings which across more than (>) threshold lines
     * If the refactoring contains multiple LeftSideLocations/rightSideLocations,
     * exclude it if only all the LeftSideLocations/RightSideLocations exceeds the threshold lines,
     * or just exclude the exceeded LeftSideLocations/RightSideLocations
     */
    public static void excludeRefactoringsAccordingToLineRanges(Map<String, List<Refactoring>> map, int threshold){
        Iterator<Map.Entry<String, List<Refactoring>>> iteratorMap = map.entrySet().iterator();
        while(iteratorMap.hasNext()){
            Map.Entry<String, List<Refactoring>> curIteratorMap = iteratorMap.next();
            Iterator<Refactoring> iteratorRef = map.get(curIteratorMap.getKey()).iterator();
            while(iteratorRef.hasNext()){
                Refactoring ref = iteratorRef.next();
                // special cases
                if(isSpecialCases(ref))
                    continue;
                // remove leftSideLocations
                Iterator<SideLocation> iteratorLeftSideLocation = ref.getLeftSideLocations().iterator();
                while(iteratorLeftSideLocation.hasNext()){
                    SideLocation temp = iteratorLeftSideLocation.next();
                    if((temp.getRange().upperEndpoint()-temp.getRange().lowerEndpoint()+1)>threshold){
                        iteratorLeftSideLocation.remove();
                    }
                }
                // remove rightSideLocations
                Iterator<SideLocation> iteratorRightSideLocation = ref.getRightSideLocations().iterator();
                while(iteratorRightSideLocation.hasNext()){
                    SideLocation temp = iteratorRightSideLocation.next();
                    if((temp.getRange().upperEndpoint()-temp.getRange().lowerEndpoint()+1)>threshold){
                        iteratorRightSideLocation.remove();
                    }
                }
                //remove the refactoring if no locations remain
                if(ref.getLeftSideLocations().isEmpty() && ref.getRightSideLocations().isEmpty()){
                    iteratorRef.remove();
                }
            }
            if(curIteratorMap.getValue().isEmpty()){
                iteratorMap.remove();
            }
        }
    }

    public static boolean isSpecialCases(Refactoring refactoring){
        // is Extract related (e.g. Extract Method, Extract Method)
        return refactoring.getType().contains("Extract");

    }

    public static void main(String [] args){
        Map<String,List<Refactoring>> map =  RefactoringLoader.loadFromDirectory(Path.of("/Users/leichen/project/semantic_lifter/SemanticLifter/patternMatch/minedRefactoring/retrolambda"));
        RefactoringLoader.excludeRefactoringsAccordingToLineRanges(map, 3);
        for(String commit:map.keySet()){
            System.out.println(map.get(commit));
        }
    }


}
