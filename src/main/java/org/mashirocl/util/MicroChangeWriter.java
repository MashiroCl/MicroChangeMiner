package org.mashirocl.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.mashirocl.microchange.MicroChange;
import org.mashirocl.dao.MinedMicroChange;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/22 15:40
 */
@Slf4j
public class MicroChangeWriter {

    public static void writeJson(List<MinedMicroChange> microChanges, String outputPath){
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

    /**
     * input a json file path and convert it to csv
     * @param inputPath json file path
     * @param outputPath csv file path
     */
    public static void writeCsv(String inputPath, String outputPath, URL link){
        log.info("read from {} and write to {}", inputPath, outputPath);
        try( CSVWriter csvWriter = new CSVWriter(new FileWriter(outputPath))){
            ObjectMapper objectMapper = new ObjectMapper();
            List<MinedMicroChange> microChanges = objectMapper.readValue(new File(inputPath), objectMapper.getTypeFactory().constructCollectionType(List.class, MinedMicroChange.class));

            String [] header = {"Repository", "CommitID", "Type", "Review" ,"OldPath", "NewPath", "Note", "Action"};
            csvWriter.writeNext(header);
            microChanges.forEach(
                    p-> {
                        String[] data = {
                                p.getRepository(),
                                LinkAttacher.attachLink(p.getCommitID(), link.toString()),
                                p.getMicroChange().getType(),
                                "",  // placeholder for manually review confusion matrix
                                p.getOldPath(),
                                p.getNewPath(),
                                "",  // placeholder for note
                                p.getMicroChange().getAction()
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
    public static void writeCsv(String inputPath, String outputPath, String commitMapPath){
        log.info("read from {} and write to {} using map {}", inputPath, outputPath, commitMapPath);
        File commitMapFile = new File(commitMapPath);
        if(!commitMapFile.exists()){
            log.error("Commit map {} not exists", commitMapPath);
            return;
        }
        CommitMapper commitMapper = new CommitMapper(commitMapPath);
        try( CSVWriter csvWriter = new CSVWriter(new FileWriter(outputPath))){
            ObjectMapper objectMapper = new ObjectMapper();
            List<MinedMicroChange> microChanges = objectMapper.readValue(new File(inputPath), objectMapper.getTypeFactory().constructCollectionType(List.class, MinedMicroChange.class));

            String [] header = {"Repository", "CommitID", "Type", "OldPath", "NewPath", "Action"};
            csvWriter.writeNext(header);
            microChanges.forEach(
                    p-> {
                        String[] data = {
                                p.getRepository(),
                                commitMapper.getMap().getOrDefault( p.getCommitID(),"no-mapping-found"),
                                p.getMicroChange().getType(),
                                p.getOldPath(),
                                p.getNewPath(),
                                p.getMicroChange().getAction()};
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

}
