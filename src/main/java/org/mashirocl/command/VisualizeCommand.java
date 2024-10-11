package org.mashirocl.command;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

/**
 * @author mashirocl@gmail.com
 * @since 2024/04/22 14:15
 */

@Slf4j
@CommandLine.Command(name = "visualize", description = "Process related to visualize micro-changes")
public class VisualizeCommand {
    public static class Config {
        @CommandLine.Parameters(index = "0", description = "")
        String refactoringMinerPath;

        @CommandLine.Parameters(index = "1", description = "")
        String repositoryPath;

        @CommandLine.Parameters(index = "2", description = "")
        String outputPath;

    }
}
