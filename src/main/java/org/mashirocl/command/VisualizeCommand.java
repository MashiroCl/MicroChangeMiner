package org.mashirocl.command;

import lombok.extern.slf4j.Slf4j;
import org.mashirocl.visualize.Collector;
import org.mashirocl.visualize.UtilCommit;
import picocli.CommandLine;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.mashirocl.visualize.Collector.*;

/**
 * @author mashirocl@gmail.com
 * @since 2024/04/22 14:15
 */

@Slf4j
@CommandLine.Command(name = "visualize", description = "Use the mined result to visualize different types code changes")
public class VisualizeCommand implements Callable<Integer> {
    public static class Config {
        @CommandLine.Parameters(index = "0", description = "The path to the mined micro-change json file, which is the output of micro-change miner")
        String microChangeJsonPath;

        @CommandLine.Parameters(index = "1", description = "The directory to the mined refactoring, which is the output of micro-change miner")
        String refactoringDirectory;

        @CommandLine.Parameters(index = "2", description = "The path to the file-level repository git")
        String fileLevelGitPath;

        @CommandLine.Parameters(index = "3", description = "The json file path for output")
        String outputPath;

    }

    @CommandLine.Mixin
    protected VisualizeCommand.Config config = new VisualizeCommand.Config();

    @Override
    public Integer call() throws Exception{
        // Conllect the mining result
        HashMap<String, UtilCommit> res = collectMCfromJson(config.microChangeJsonPath, config.refactoringDirectory);
        List<UtilCommit> commits = new LinkedList<>();
        res.keySet().forEach(p->commits.add(res.get(p)));

        calcUtilDiff(commits, config.fileLevelGitPath);
        commitsToSimplifiedUtilJson(commits, "/Users/leichen/project/semantic_lifter/visualize/mined_microchanges/mbassador.json");
        return  0;
    }
}
