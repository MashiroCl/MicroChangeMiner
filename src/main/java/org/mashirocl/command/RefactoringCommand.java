package org.mashirocl.command;

import lombok.extern.slf4j.Slf4j;
import org.mashirocl.refactoringminer.RefactoringMiner;
import picocli.CommandLine;

import java.util.concurrent.Callable;

/**
 * @author mashirocl@gmail.com
 * @since 2024/03/06 20:40
 */


@Slf4j
@CommandLine.Command(name = "refmine", description = "Mine refactorings for a repository")
public class RefactoringCommand implements Callable<Integer>{
    public static class Config {
        @CommandLine.Parameters(index = "0", description = "Refactoring Miner path")
        String refactoringMinerPath;

        @CommandLine.Parameters(index = "1", description = "Mining micro-changes in this repository")
        String repositoryPath;

        @CommandLine.Parameters(index = "2", description = "Output mining result in json file")
        String outputPath;

    }

    @CommandLine.Mixin
    protected Config config = new Config();

    @Override
    public Integer call() throws Exception {
        RefactoringMiner refactoringMiner = new RefactoringMiner(config.refactoringMinerPath);
        log.info("Start mining for {}", config.repositoryPath);
        refactoringMiner.mine(config.repositoryPath, config.outputPath);
        log.info("Refactoring Mining finished, mined result: {}", config.outputPath);
        return 0;
    }
}
