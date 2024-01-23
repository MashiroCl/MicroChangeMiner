package org.mashirocl.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * map of the commitID of method-level repository with the original repository
 * @author mashirocl@gmail.com
 * @since 2024/01/23 13:21
 */
@Slf4j
@Getter
public class CommitMapper {

    private Map<String, String> map;

    public CommitMapper(){

    }

    /**
     * load the map from a json file
     * @param input
     */
    public CommitMapper(String input){
        ObjectMapper objectMapper = new ObjectMapper();
        try{
            log.info("Loading commit hash map from {}", input);
            map = objectMapper.readValue(new File(input), objectMapper.getTypeFactory().constructType(Map.class));
        }catch (IOException e){
            log.error(e.getMessage(),e);
        }

    }

    public Map<String, String> extractMap(String gitPath){
        log.info("Obtaining commit hash map for {}", gitPath);
        map = new HashMap<>();
        try {
            Process process = new ProcessBuilder("git", "log").directory(new File(gitPath)).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while((line = reader.readLine())!=null){
                if(line.startsWith("commit ")){
                    String commithash = line.substring(7);
                    String originCommitHash = extractNote(commithash, gitPath);
                    map.put(commithash, originCommitHash);
                    map.put(originCommitHash, commithash); //bidirectional mapping
                }
            }
        }
        catch (IOException e){
            log.error(e.getMessage(),e);
        }
        return map;
    }

    private String extractNote(String commitHash, String gitPath){
        try {
            Process process = new ProcessBuilder("git","notes","show", commitHash).directory(new File(gitPath)).start();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            return bufferedReader.readLine();
        }
        catch (IOException e){
            log.error(e.getMessage(), e);
            return "Not found";
        }
    }

    /**
     * write the hash map to a json file
     * @param output
     */
    public void write(String output){
        log.info("Writing commit hash map to {}", output);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writeValue(new FileWriter(output),map);
        }catch (IOException e){
            log.error(e.getMessage(), e);
        }
    }

 }
