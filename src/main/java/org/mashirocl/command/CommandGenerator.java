package org.mashirocl.command;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/23 14:37
 */
public class CommandGenerator {

    public void getCommitMap(String repoPath, String outputPath){
        System.out.printf("java -jar build/libs/miner-1.0-SNAPSHOT-all.jar commitMap -p %s -o %s\n", repoPath, outputPath);
    }

    public void mineMicroChangeWithMap(String repoPath, String outputJsonPath, String csvOutput, String mapPath){
        System.out.printf("java -jar  build/libs/miner-1.0-SNAPSHOT-all.jar mine %s %s --csv %s --map %s \n", repoPath, outputJsonPath, csvOutput, mapPath);
    }

    public static void main(String [] args){
        CommandGenerator cg = new CommandGenerator();

        String repo = "jfinal";
        String repoPath = "/Users/leichen/data/parsable_method_level/"+repo+"/.git";
        String mapPath = "/Users/leichen/project/semantic_lifter/SemanticLifter/patternMatch/miner/commitMap/"+repo+".json";

        String csvOutput = "/Users/leichen/project/semantic_lifter/SemanticLifter/patternMatch/miner/mined/"+repo+".csv";
        String jsonOutput = "/Users/leichen/project/semantic_lifter/SemanticLifter/patternMatch/miner/mined/"+repo+".json";

        cg.getCommitMap(repoPath, mapPath);
        cg.mineMicroChangeWithMap(repoPath, jsonOutput, csvOutput, mapPath);

    }
}
