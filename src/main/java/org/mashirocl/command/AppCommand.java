package org.mashirocl.command;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/17 22:42
 */

@Command(sortOptions = false, subcommands = {MineCommand.class, CommitMapCommand.class, RefactoringCommand.class, VisualizeCommand.class})
public class AppCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        return 0;
    }
}
