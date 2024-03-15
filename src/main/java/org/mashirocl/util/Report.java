package org.mashirocl.util;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * @author mashirocl@gmail.com
 * @since 2024/03/14 15:46
 */
@Slf4j
public class Report {

    public static void buildBriefReport(String logPath){
        List<String> res = new LinkedList<>();
        try(BufferedReader bufferedReader = new BufferedReader(new FileReader(logPath))){
            String line;
            while((line = bufferedReader.readLine())!=null){
                if(isSelected(line)){
                    res.add(line);
                }
            }
        } catch (IOException e){
            log.error(e.getMessage(),e);
        }
        res.forEach(System.out::println);
    }


    public static boolean isSelected(String line){
        return line.contains("micro-change covered deleted lines/number of total lines of tree code deleted:")
                || line.contains("refactoring covered deleted lines/number of total lines of tree code deleted:")
                || line.contains("refactoring covered added lines/number of total lines of tree code added:")
                || line.contains("refactoring covered micro-change deleted lines/micro-change covered deleted lines:")
                || line.contains("refactoring covered micro-change added lines/micro-change covered added lines:")
                || line.contains("Total number of actions:")
                || line.contains("Micro-change contained actions:")
                || line.contains("micro-change contained action/number of total actions:")
                || line.contains("not fully covered!:");
    }


    public static void main(String [] args){
        String logPath = "/Users/leichen/project/semantic_lifter/SemanticLifter/patternMatch/miner/runLog/mbassador.log";
//        logPath = "/Users/leichen/project/semantic_lifter/SemanticLifter/patternMatch/miner/runLog/javapoet.log";
//        logPath = "/Users/leichen/project/semantic_lifter/SemanticLifter/patternMatch/miner/runLog/jfinal.log";
//        logPath = "/Users/leichen/project/semantic_lifter/SemanticLifter/patternMatch/miner/runLog/retrolambda.log";
//        logPath = "/Users/leichen/project/semantic_lifter/SemanticLifter/patternMatch/miner/runLog/android-demos.log";

        Report.buildBriefReport(logPath);
    }
}
