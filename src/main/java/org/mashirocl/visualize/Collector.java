package org.mashirocl.visualize;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.diff.DiffFormatter;
import org.mashirocl.dao.CommitDAO;
import org.mashirocl.dao.MicroChangeDAO;
import org.mashirocl.dao.RefactoringDAO;
import org.mashirocl.match.ActionLocator;
import org.mashirocl.refactoringminer.MethodLevelConvertor;
import org.mashirocl.util.CSVWriter;
import org.mashirocl.util.CommitMapper;
import org.mashirocl.util.RepositoryAccess;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author mashirocl@gmail.com
 * @since 2024/04/22 14:18
 */
@Slf4j
public class Collector {

    /**
     * remove the same micro-changes/refactorings
     * if the micro-change/refactoring in the same commit contains
     * the same type, locations, and the same action/descriptions, they are regarded as the duplicated
     * @param commit
     */
    public static void removeDuplicate(Commit commit){
        Set<MicroChangeDAO> uniqueChanges = new LinkedHashSet<>(commit.getMicroChanges());
        commit.setMicroChanges(new LinkedList<>(uniqueChanges));
        Set<RefactoringDAO> uniqueRefactorings = new LinkedHashSet<>(commit.getRefactorings());
        commit.setRefactorings(new LinkedList<>(uniqueRefactorings));
    }

    public static HashMap<String, Commit> collectMCfromJson(String jsonPath){
        List<CommitDAO> commitDAOList = CSVWriter.readCommitDAOsFromJson(jsonPath);
        HashMap<String, Commit> res = new HashMap<>();
        for(CommitDAO p: commitDAOList){
            if(!res.containsKey(p.getSha1())){
                res.put(p.getSha1(),
                        new Commit(p.getRepository(), p.getSha1(), p.getUrl(), p.getMicroChanges(), p.getRefactorings()));
            }
            else {
                Commit commit = res.get(p.getSha1());
                commit.getMicroChanges().addAll(p.getMicroChanges());
                commit.getRefactorings().addAll(p.getRefactorings());
            }
        }

        // remove "duplicate" micro-changes/refactorings
        res.keySet().forEach(p->removeDuplicate(res.get(p)));

        return res;
    }

    //todo remove the comments
    public static void getDiff(List<Commit> commits, String methodLevelGitPath, String fileLevelGitPath, String commitMap){
        RepositoryAccess methodLevelRepositoryAccess = new RepositoryAccess(Path.of(methodLevelGitPath));
        RepositoryAccess fileLevelRepositoryAccess = new RepositoryAccess(Path.of(fileLevelGitPath));

        final DiffFormatter methodLevelDiffFormatter = new DiffFormatter(System.out);
        methodLevelDiffFormatter.setRepository(methodLevelRepositoryAccess.getRepository());
        final DiffFormatter fileLevelDiffFormatter = new DiffFormatter(System.out);
        fileLevelDiffFormatter.setRepository(fileLevelRepositoryAccess.getRepository());

        EditScriptGenerator editScriptGenerator = new SimplifiedChawatheScriptGenerator();

        ActionLocator actionLocator = new ActionLocator();

        CommitMapper commitMapper = new CommitMapper(commitMap);
        MethodLevelConvertor methodLevelConvertor = new MethodLevelConvertor(commitMapper);

        DiffProcessor methodLevelDiffProcessor = new DiffProcessor(methodLevelDiffFormatter, editScriptGenerator, actionLocator);
        DiffRange treeDiff =  methodLevelDiffProcessor.getTreeDiff(methodLevelRepositoryAccess, methodLevelConvertor, methodLevelGitPath);
        // refresh the ra
        methodLevelRepositoryAccess = new RepositoryAccess(Path.of(methodLevelGitPath));
        DiffRange methodLevelTextualDiff =  methodLevelDiffProcessor.getTextualDiff(methodLevelRepositoryAccess, methodLevelConvertor, methodLevelGitPath);

        DiffProcessor fileLevelDiffProcessor = new DiffProcessor(fileLevelDiffFormatter, editScriptGenerator, actionLocator);
        Map<String, Map<String, Map<String,String>>> sourceCodes = fileLevelDiffProcessor.getSource(fileLevelGitPath);
        // refresh the ra
        fileLevelRepositoryAccess = new RepositoryAccess(Path.of(fileLevelGitPath));
        DiffRange fileLevelDiff = fileLevelDiffProcessor.getTextualDiff(fileLevelRepositoryAccess);
        Map<String, ChangeRanges> fullChangeRange = fileLevelDiff.extractRange();

        DiffRange intersected =  DiffRange.intersection(treeDiff, methodLevelTextualDiff);
        Map<String, ChangeRanges> intersectedRange =  intersected.extractRange();

        for(Commit commit:commits){
            commit.setFullChangeRanges(fullChangeRange.get(commit.getSha1()));

            String methodLevelCommit = commitMapper.getMap().get(commit.getSha1());
            if(sourceCodes.containsKey(commit.getSha1())){
                for(String file:sourceCodes.get(commit.getSha1()).get("preChange").keySet()){
                    commit.getPreChangeSourceCode().put(file,sourceCodes.get(commit.getSha1()).get("preChange").get(file));
                }
                for(String file:sourceCodes.get(commit.getSha1()).get("postChange").keySet()){
                    commit.getPostChangeSourceCode().put(file,sourceCodes.get(commit.getSha1()).get("postChange").get(file));
                }
            }

            if(intersectedRange.containsKey(methodLevelCommit)){
                commit.setChangeRanges(intersectedRange.get(methodLevelCommit));
            }
            else {
                log.info("commit {} does not have a range", commit.getSha1());
            }
        }
    }


    public static void commitsToCSV(List<Commit> commits, String outputPath){
        log.info("write to csv {}", outputPath);
        try( com.opencsv.CSVWriter csvWriter = new com.opencsv.CSVWriter(new FileWriter(outputPath))){

            String [] header = {"Method-level Commit sha1", "Url","prechange","postchange", "range", "MicroChanges","Refactorings"};
            csvWriter.writeNext(header);
            for(Commit commit: commits){
                String [] data = {
                        commit.getSha1(),
                        commit.getUrl(),
                        commit.getPreChangeSourceCode().toString(),
                        commit.getPostChangeSourceCode().toString(),
                        commit.getChangeRanges().toString(),
                        commit.getMicroChanges().toString(),
                        commit.getRefactorings().toString()
                };
                csvWriter.writeNext(data);
            }
        }
        catch (IOException e){
            log.error(e.getMessage(),e);
        }
    }

    public static void commitsToJson(List<Commit> commits, String outputPath){
        log.info("write to json {}", outputPath);
        ObjectMapper objectMapper = new ObjectMapper();
        try{
            Path path = Paths.get(outputPath);
            Files.createDirectories(path.getParent());
            Files.createFile(path);
            File f = new File(outputPath);
            objectMapper.writeValue(f, commits);
        } catch (IOException e){
            log.error(e.getMessage(), e);
        }
    }

    public static void commitsToSimplifiedJson(List<Commit> commits, String outputPath){
        log.info("write to json {}", outputPath);
        ObjectMapper objectMapper = new ObjectMapper();
        try{
            Path path = Paths.get(outputPath);
            Files.createDirectories(path.getParent());
            Files.createFile(path);
            File f = new File(outputPath);
            List<SimplifiedCommit> simplifiedCommits = new LinkedList<>();
            commits.forEach(p->simplifiedCommits.add(new SimplifiedCommit(p)));
            log.info("simplifiedCommits size {}", simplifiedCommits.size());
            objectMapper.writeValue(f, simplifiedCommits);
        } catch (IOException e){
            log.error(e.getMessage(), e);
        }

    }

    public static void main(String [] args){
        String mbassadorJsonPath = "/Users/leichen/project/semantic_lifter/SemanticLifter/mined/2024.4.10_lut/mined_4_10/minedJSON/mbassador.json";
        HashMap<String, Commit> res = collectMCfromJson(mbassadorJsonPath);
        List<Commit> commits = new LinkedList<>();
        res.keySet().forEach(p->commits.add(res.get(p)));

        String methodLevelGitPath = "/Users/leichen/data/parsable_method_level/mbassador/.git";
        String fileLevelGitPath = "/Users/leichen/data/OSS/mbassador/.git";
        String commitMap = "/Users/leichen/project/semantic_lifter/SemanticLifter/mined/2024.4.10_lut/commitMap/mbassador.json";

        getDiff(commits, methodLevelGitPath, fileLevelGitPath, commitMap);
        //todo use a database? I have so many source code
//        commitsToCSV(commits, "/Users/leichen/project/semantic_lifter/visualize/mined_microchanges/mbassador_test2.csv");
        // just to json and then load, I needn't a large repo after all
        commitsToSimplifiedJson(commits, "/Users/leichen/project/semantic_lifter/visualize/mined_microchanges/mbassador.json");
    }
}
