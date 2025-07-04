package org.mashirocl.refactoringminer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.revwalk.RevCommit;
import org.mashirocl.util.RepositoryAccess;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * @author mashirocl@gmail.com
 * @since 2024/03/04 10:14
 */

@AllArgsConstructor
@Getter
@Setter
@Slf4j
public class RefactoringMiner {
    private String minerPath;

    public void mine(Path repoPath, String commitID, Path output){
        try{
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("./"+minerPath, "-c", repoPath.toString(), commitID, "-json", output.resolve(commitID+".json").toString());
            Process process = processBuilder.start();
            int exitValue = process.waitFor();
            if(exitValue!=0){
                log.error("Refactoring mining on {} exits with non-zero value", commitID);
            }
        }catch (IOException | InterruptedException e){
            log.error(e.getMessage(), e);
        }
    }


    /**
     * Mine refactorings for all the commits in a respository
     * @param path
     * @param output
     */
    public void mine(Path path, Path output){
        // check if the remote not exists, add one
        addRemote(path);
        // create the output directory if not exists
        if(!Files.exists(output)){
            try {
                Files.createDirectory(output);
            }
            catch (IOException e){
                log.error(e.getMessage(), e);
            }
        }
        Path gitPath = path.toAbsolutePath().resolve(".git");
        RepositoryAccess ra = new RepositoryAccess(gitPath);
        Iterable<RevCommit> walk = ra.walk(null, "HEAD");
        // Use a threadPool to avoid consuming too much memory
        int numberOfThreads = Runtime.getRuntime().availableProcessors()/2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        for (RevCommit commit : walk) {
            final String commitID = commit.getId().getName();
            executorService.submit(()-> mine(gitPath,commitID,output));
        }

        executorService.shutdown();
        try {
            // Wait for all tasks to finish execution or timeout after a certain period
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            // Restore interrupted state...
            Thread.currentThread().interrupt();
        }
    }


    public void mine(String path, String output){
        mine(Path.of(path), Path.of(output));
    }

    private void addRemote(Path repoPath){
        try{
            // check remote exists or not
            ProcessBuilder checkRemote = new ProcessBuilder();
            checkRemote.command("git", "remote");
            Process checkRemoteprocess = checkRemote.directory(new File(repoPath.toString())).start();
            checkRemoteprocess.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(checkRemoteprocess.getInputStream()));
            String line;
            if((line = reader.readLine())==null){
                ProcessBuilder processBuilder = new ProcessBuilder();
                processBuilder.command("git", "remote", "add", "origin", "https://example.jp/dummy_url.git");
                processBuilder.directory(new File(repoPath.toString())).start();
            }
        }
        catch (IOException | InterruptedException e){
            log.error(e.getMessage(),e);
        }
    }


    public static void main(String [] args){
        RefactoringMiner miner = new RefactoringMiner("/Users/leichen/project/semantic_lifter/RefactoringMiner-3.0.2/bin/");
        String repository = "zuul";
        Path outputDirectory = Path.of("/Users/leichen/project/semantic_lifter/SemanticLifter/patternMatch/miner/minedRefactoring/").resolve(repository);
        miner.mine(Path.of("/Users/leichen/data/OSS/").resolve(repository), outputDirectory);
    }

}
