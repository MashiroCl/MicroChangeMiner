package org.mashirocl.command;

import lombok.extern.slf4j.Slf4j;
import org.mashirocl.util.CommitMapper;
import picocli.CommandLine;

import java.util.concurrent.Callable;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/23 14:07
 */

@Slf4j
@CommandLine.Command(name = "commitMap", description = "get the commit hash map from method-level repository to original commit")
public class CommitMapCommand implements Callable<Integer> {
    public static class Config{
        @CommandLine.Option(names = {"-p", "--gitpath"}, description = {"path to the method-level repository git"})
        String gitPath;
        @CommandLine.Option(names = {"-o", "--output"}, description = {" output path of the commit map"})
        String outputPath;

        @CommandLine.Option(names = {"-c", "--csv"}, description = "convert the json commit map to csv")
        String csvPath;

    }

    @CommandLine.Mixin
    protected Config config;

    @Override
    public Integer call() throws Exception {
        CommitMapper commitMapper = new CommitMapper();
        commitMapper.extractMap(config.gitPath);
        commitMapper.write(config.outputPath);
        return 0;
    }

}
