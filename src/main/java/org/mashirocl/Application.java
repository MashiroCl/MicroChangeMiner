package org.mashirocl;

import org.mashirocl.command.AppCommand;
import picocli.CommandLine;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/17 16:16
 */
public class Application {
    public static void main(String [] args){
        final AppCommand app = new AppCommand();
        final CommandLine cmdline = new CommandLine(app);
        cmdline.setExecutionStrategy(new CommandLine.RunAll());
        cmdline.setExpandAtFiles(false);

        final int status = cmdline.execute(args);
        if (status != 0) {
            System.exit(status);
        }
    }
}
