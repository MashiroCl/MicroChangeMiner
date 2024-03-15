package org.mashirocl.refactoringminer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mashirocl.util.CommitMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.*;

/**
 * @author mashirocl@gmail.com
 * @since 2024/03/08 16:28
 */
@Slf4j
@AllArgsConstructor
public class MethodLevelConvertor {

    public CommitMapper commitMapper;
    public static ObjectMapper objectMapper = new ObjectMapper();

    /**
     * convert the file level line range to method level line range
     */
//    public void convert(Refactoring refactoring){
//        //TODO
//        // 1. refactoring one leftSideLocation path+commit -> locate a mapping file
//        // 2. find whether the method level if is in the refactoring's range
//        refactoring.getLeftSideLocations().get(0).getPath()
//    }


    /**
     * replace the file level location info in refactoring with method level, note that the fields are excluded
     *
     * @param fileLevelGitFile
     * @param methodLevelRepoFile
     * @param refactoring
     * @param commitID
     */
    public Refactoring replaceRefFileLevelWithMethodLevel(File fileLevelGitFile, File methodLevelRepoFile, Refactoring refactoring, String commitID) {

        // refactoring left side
        List<SideLocation> leftSideMethodLevelLocations = new LinkedList<>();
        List<SideLocation> leftSideFileLevelLocations = refactoring.getLeftSideLocations();
        for (SideLocation sideLocation : leftSideFileLevelLocations) {
            String relativePath = sideLocation.getPath().toString().replace("\"", "");
            Range refRange = sideLocation.getRange();
            Map<String, Range> methodsRange = getMethodLevelMappingFile(methodLevelRepoFile,
                    new File(relativePath.substring(0, relativePath.lastIndexOf(".java")) + ".mapping"),
                    commitMapper.getMap().get(getParentCommit(fileLevelGitFile, commitID)));
            convertFileLevelRefToMethodLevel(refRange, methodsRange);

            for (String methodFile : methodsRange.keySet()) {
                leftSideMethodLevelLocations.add(new SideLocation(Path.of(relativePath).resolveSibling(methodFile.replace("\"", "")), methodsRange.get(methodFile)));
            }
        }

        // refactoring right side
        List<SideLocation> rightSideMethodLevelLocations = new LinkedList<>();
        List<SideLocation> rightSideFileLevelLocations = refactoring.getLeftSideLocations();
        for (SideLocation sideLocation : rightSideFileLevelLocations) {
            String relativePath = sideLocation.getPath().toString().replace("\"", "");
            Range refRange = sideLocation.getRange();
            Map<String, Range> methodsRange = getMethodLevelMappingFile(methodLevelRepoFile,
                    new File(relativePath.substring(0, relativePath.lastIndexOf(".java")) + ".mapping"),
                    commitMapper.getMap().get(commitID));
            convertFileLevelRefToMethodLevel(refRange, methodsRange);

            for (String methodFile : methodsRange.keySet()) {
                rightSideMethodLevelLocations.add(new SideLocation(Path.of(relativePath).resolveSibling(methodFile.replace("\"", "")), methodsRange.get(methodFile)));
            }
        }
        refactoring.setLeftSideLocations(leftSideMethodLevelLocations);
        refactoring.setRightSideLocations(rightSideMethodLevelLocations);
        return refactoring.getLeftSideLocations().isEmpty()&&refactoring.getRightSideLocations().isEmpty()?null:refactoring;
    }

    private void convertFileLevelRefToMethodLevel(Range refRange, Map<String, Range> methodsRange) {
        Iterator<Map.Entry<String,Range>> itr = methodsRange.entrySet().iterator();
        // get the range of refactoring in methodsRange
        while(itr.hasNext()){
            Map.Entry<String, Range> entry = itr.next();
            // remove non-method files & remove files has no intersection with refactoring range
            if(!entry.getKey().contains(".mjava") || !refRange.isConnected(entry.getValue())){
                itr.remove();
            }else {
                // if refactoring and method has intersection, update the line range as the intersection
                Range<Integer> intersection =  refRange.intersection(entry.getValue());
                // convert from file level line range to method level line range
                entry.setValue(convertFileLevelLineNumberToMethodLevel(intersection, entry.getValue()));
            }
        }
    }

    /**
     * convert the intersected ref range (ref intersects with method code) from file level to method-file level
     * the method declaration and class declaration are counted as 2 lines
     */
    private Range<Integer> convertFileLevelLineNumberToMethodLevel(Range<Integer> ref, Range<Integer> method) {
        return Range.closed(
                ref.lowerEndpoint()-method.lowerEndpoint()+1+2,
                ref.upperEndpoint()-method.lowerEndpoint()+1+2
        );
    }

    private Range<Integer> convertMethodLevelLineNumberToFileLevel(Range<Integer> range, Range<Integer> fileLevelRange) {
        return Range.closed(
                range.lowerEndpoint()-3+fileLevelRange.lowerEndpoint(),
                range.upperEndpoint()-3+fileLevelRange.lowerEndpoint()
        );
    }

    /**
     * exclude some types of refactoring
     * @return
     */
    private boolean isExcludedRefactoringType(Refactoring refactoring){
        String type = refactoring.getType();
        return !type.contains("class");
    }


    public String getParentCommit(File gitFile, String commitID) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("git", "rev-parse", commitID + "^");
            processBuilder.directory(gitFile);
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            if ((line = reader.readLine()) != null) {
                return line;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public Map<String, Range> getMethodLevelMappingFile(File methodLevelRepoFile, File mappingFile, String commitID) {
        Map<String, Range> mapping = new HashMap<>();
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("git", "show", commitID + ":" + mappingFile);
            processBuilder.directory(methodLevelRepoFile);
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                mapping.putAll(loadMethodLevelMappingUnit(line));
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return mapping;
    }

    public Map<String, Range> loadMethodLevelMappingUnit(String line) {
        Map<String, Range> map = new HashMap<>();
        try {
            JsonNode mappingUnit = objectMapper.readTree(line);
            map.put(mappingUnit.get("filename").toString(),
                    Range.closed(
                            mappingUnit.get("beginLine").intValue(),
                            mappingUnit.get("endLine").intValue()
                    ));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return map;
    }


    public Map<String, List<Refactoring>> getMethodLevelRefactorings(String refDirectory, String commitMapPath, String methodLevelRepoPath, String fileLevelGitPath){
        Map<String, List<Refactoring>> originalRefMap = RefactoringLoader.loadFromDirectory(Path.of(refDirectory));
        Map<String, List<Refactoring>> methodLevelRefMap = new HashMap<>();
        CommitMapper commitMapper = new CommitMapper(commitMapPath);
        MethodLevelConvertor methodLevelConvertor = new MethodLevelConvertor(commitMapper);
        File methodLevelRepoFile = new File(methodLevelRepoPath);
        File fileLevelGit = new File(fileLevelGitPath);
        for (String commit : originalRefMap.keySet()) {
            // remove the refactorings that are not inside method & convert the refactorings to method-level
            List<Refactoring> temp = new LinkedList<>();
            for (int i = 0; i < originalRefMap.get(commit).size(); i++) {
                Refactoring convertedRef = methodLevelConvertor.replaceRefFileLevelWithMethodLevel(fileLevelGit, methodLevelRepoFile, originalRefMap.get(commit).get(i), commit);
                if(convertedRef!=null) temp.add(convertedRef);
            }
            methodLevelRefMap.put(commitMapper.getMap().get(commit), temp);
        }
        return methodLevelRefMap;
    }


    public RangeSet covertMethodLevelRangeToFileLevel(String methodLevelCommit, String methodLevelRepoPath, String methodLevelfilePath, RangeSet<Integer> rangeSet){
        Map<String, Range> map =  getMethodLevelMappingFile(new File(methodLevelRepoPath),
                                    new File(convertMethodLevelFileToMapFile(methodLevelfilePath)),
                                    methodLevelCommit);
        RangeSet<Integer> res = TreeRangeSet.create();
        if(map.isEmpty()){
            log.error("Failed to find the mapping file using command: git show {}:{}", methodLevelCommit, convertMethodLevelFileToMapFile(methodLevelfilePath));
        }else{
            for(Range<Integer> range:rangeSet.asRanges()){
                res.add(convertMethodLevelLineNumberToFileLevel(range, map.get("\""+Path.of(methodLevelfilePath).getFileName().toString()+"\"")));
            }
        }
        return res;
    }

    /**
     * for method-level file path, it should be src/main/java/.../Class#method().mjava or  src/main/java/.../Class.NestedClass#method().mjava
     * convert the file path to src/main/java/.../Class.mapping
     * @param path
     * @return
     */
    public String convertMethodLevelFileToMapFile(String path){
        String fileName = Path.of(path).getFileName().toString();
        String className = fileName.substring(0, Math.min(fileName.indexOf("#"), fileName.indexOf(".")));
        return Path.of(path).resolveSibling(className)+".mapping";
    }

    public static void main(String[] args) {
        String refDirectory = "/Users/leichen/project/semantic_lifter/SemanticLifter/patternMatch/minedRefactoring/my-refactoring-toy-example";
        String commitMapPath = "/Users/leichen/project/semantic_lifter/SemanticLifter/patternMatch/miner/commitMap/my-refactoring-toy-example.json";
        String methodLevelRepo = "/Users/leichen/data/parsable_method_level/my-refactoring-toy-example/";
        String fileLevelGit = "/Users/leichen/data/OSS/my-refactoring-toy-example/.git";

        MethodLevelConvertor methodLevelConvertor = new MethodLevelConvertor(new CommitMapper(commitMapPath));
//        Map<String, List<Refactoring>> map = methodLevelConvertor.getMethodLevelRefactorings(refDirectory, commitMapPath, methodLevelRepo, fileLevelGit);
//        for(String commit:map.keySet()){
//            System.out.println(commit);
//            System.out.println(map.get(commit));
//        }

        String methodLevelFilePath = "src/main/java/net/engio/mbassy/IPublicationErrorHandler.ConsoleLogger#method1.mjava";
        String methodLevelFilePath2 = "src/main/java/net/engio/mbassy/IPublicationErrorHan#method1.mjava";
        System.out.println(methodLevelConvertor.convertMethodLevelFileToMapFile(methodLevelFilePath));
        System.out.println(methodLevelConvertor.convertMethodLevelFileToMapFile(methodLevelFilePath2));
    }
}