package org.mashirocl.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.mashirocl.dao.CommitDAO;
import org.mashirocl.dao.MicroChangeDAO;
import org.mashirocl.dao.MinedMicroChange;
import org.mashirocl.microchange.MicroChangeFileSpecified;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/22 15:40
 */
@Slf4j
public class CSVWriter {

    public static void writeMicroChange2Json(List<MinedMicroChange> microChanges, String outputPath){
        log.info("Writing {} micro-changes to {}", microChanges.size(), outputPath);
        ObjectMapper objectMapper = new ObjectMapper();
        try{
            Path path = Paths.get(outputPath);
            Files.createDirectories(path.getParent());
            Files.createFile(path);
            File f = new File(outputPath);
            objectMapper.writeValue(f, microChanges);
        } catch (IOException e){
            log.error(e.getMessage(), e);
        }
    }

    public static void writeCommit2Json(List<CommitDAO> commitDAOs, String outputPath){
        log.info("Writing {} commits to {}", commitDAOs.size(), outputPath);
        ObjectMapper objectMapper = new ObjectMapper();
        try{
            Path path = Path.of(outputPath);
            if(!Files.exists(path)) {
                Files.createDirectories(path.getParent());
                Files.createFile(path);
            }
            File f = new File(outputPath);
            objectMapper.writeValue(f, commitDAOs);
        } catch (IOException e){
            log.error(e.getMessage(), e);
        }
    }

    public static void writeCommit2CSV(String inputPath, String outputPath){
        log.info("read from {} and write to {}", inputPath, outputPath);
        try( com.opencsv.CSVWriter csvWriter = new com.opencsv.CSVWriter(new FileWriter(outputPath))){
            ObjectMapper objectMapper = new ObjectMapper();
            List<CommitDAO> commitDAOs = objectMapper.readValue(
                    new File(inputPath),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, CommitDAO.class));

            String [] header = {"Method-level CommitID", "Url","M/R", "Type","OldPosition", "NewPosition","Review" , "Note"};
            csvWriter.writeNext(header);
            for(CommitDAO commitDAO:commitDAOs){
                commitDAO.getMicroChanges().forEach(p -> {
                    String[] data = {
                            commitDAO.getSha1(),
                            commitDAO.getUrl(),
                            "M",
                            p.getType(),
                            p.getLeftSideLocations().toString(),
                            p.getRightSideLocations().toString(),
                            "",
                            ""
                    };
                    csvWriter.writeNext(data);
                });
                commitDAO.getRefactorings().forEach(p -> {
                    String[] data = {
                            commitDAO.getSha1(),
                            commitDAO.getUrl(),
                            "R",
                            p.getType(),
                            p.getLeftSideLocations().toString(),
                            p.getRightSideLocations().toString(),
                            "",
                            ""
                    };
                    csvWriter.writeNext(data);
                });
            }
        }
        catch (IOException e){
            log.error(e.getMessage(),e);
        }
    }

    /**
     * input a json file path and convert it to csv
     * @param inputPath json file path
     * @param outputPath csv file path
     */
    public static void writeMircoChangesToCsv(String inputPath, String outputPath, URL link){
        log.info("read from {} and write to {}", inputPath, outputPath);
        try( com.opencsv.CSVWriter csvWriter = new com.opencsv.CSVWriter(new FileWriter(outputPath))){
            ObjectMapper objectMapper = new ObjectMapper();
            List<MinedMicroChange> microChanges = objectMapper.readValue(
                    new File(inputPath),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, MinedMicroChange.class));

            String [] header = {"Repository", "CommitID", "Type", "Review" ,"OldPath", "NewPath","Position", "Note", "Action"};
            csvWriter.writeNext(header);
            microChanges.forEach(
                    p-> {
                        String[] data = {
                                p.getRepository(),
                                LinkAttacher.attachLink(p.getCommitID(), link.toString()),
                                p.getType(),
                                "",  // placeholder for manually review confusion matrix
                                p.getOldPath(),
                                p.getNewPath(),
                                p.getPosition(),
                                "",  // placeholder for note
                                p.getAction()
                        };
                        csvWriter.writeNext(data);
                    });
        }
        catch (IOException e){
            log.error(e.getMessage(),e);
        }
    }


    /**
     * input a json file path and convert it to csv
     * @param inputPath json file path
     * @param outputPath csv file path
     * @param commitMapPath commit map path
     */
    @Deprecated
    public static void writeMircoChangesToCsv(String inputPath, String outputPath, String commitMapPath){
        log.info("read from {} and write to {} using map {}", inputPath, outputPath, commitMapPath);
        File commitMapFile = new File(commitMapPath);
        if(!commitMapFile.exists()){
            log.error("Commit map {} not exists", commitMapPath);
            return;
        }
        CommitMapper commitMapper = new CommitMapper(commitMapPath);
        try( com.opencsv.CSVWriter csvWriter = new com.opencsv.CSVWriter(new FileWriter(outputPath))){
            ObjectMapper objectMapper = new ObjectMapper();
            List<MinedMicroChange> microChanges = objectMapper.readValue(new File(inputPath), objectMapper.getTypeFactory().constructCollectionType(List.class, MinedMicroChange.class));

            String [] header = {"Repository", "CommitID", "Type", "OldPath", "NewPath", "Action"};
            csvWriter.writeNext(header);
            microChanges.forEach(
                    p-> {
                        String[] data = {
                                p.getRepository(),
                                commitMapper.getMap().getOrDefault( p.getCommitID(),"no-mapping-found"),
                                p.getType(),
                                p.getOldPath(),
                                p.getNewPath(),
                                p.getAction()};
                        csvWriter.writeNext(data);
                    });
        }
        catch (IOException e){
            log.error(e.getMessage(),e);
        }
    }

    /**
     * convert the method-level commit hash to original hash
     * @param commitID
     * @return
     */
    public static String convertCommit(String commitID, String repository, CommitMapper commitMapper){
        return LinkAttacher.attachLink( commitMapper.getMap().getOrDefault( commitID, "no-mapping-found"),
                LinkAttacher.searchLink(repository));
    }


    public static void writeCSV(List<MicroChangeFileSpecified> microChangeFileSpecifiedList){
        microChangeFileSpecifiedList.stream().map(p -> new MicroChangeDAO(p.getType(),p.getLeftSideLocations(),p.getRightSideLocations())).toList();
    }


    public static void main(String [] args){
        writeCommit2CSV("/Users/leichen/project/semantic_lifter/SemanticLifter/patternMatch/miner/commitDAOs.json", "/Users/leichen/project/semantic_lifter/SemanticLifter/patternMatch/miner/commitDAOs.csv");
    }

}
