package org.mashirocl.command;

import org.mashirocl.editscript.ControlStructureType;
import org.mashirocl.microchange.SrcDstRange;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static org.mashirocl.command.MineCommand.coveredLength;

/**
 * @author mashirocl@gmail.com
 * @since 2025/04/22 14:13
 */
public class ProcessingStats {
    int[] totalADCodeChangeLines = new int[]{0, 0};
    Map<ControlStructureType, int[]> structureExpressionChangeLines = new HashMap<>(){
        {
            for(ControlStructureType type:ControlStructureType.values()){
                put(type, new int[]{0, 0});
            }
        }
    };
    int[] microADChangeCoveredLines = new int[]{0, 0};
    int[] mrADChangeCoveredLines = new int[]{0, 0};
    int[] numberOfConditionalExpression = new int[]{0, 0};
    int[] numberOfForExpression = new int[]{0, 0};
    int[] numberOfWhileExpression = new int[]{0, 0};
    int structureExpressionChangeContainedCommit = 0;
    int numberTotalConditionRelatedActionNumber = 0;
    int numberTotalForRelatedActionNumber = 0;
    int numberTotalWhileRelatedActionNumber = 0;
    int numberMicroChangeContainedStructureRelatedAction = 0;
    int numberOfFilesProcessed = 0;

    public void updateNumberOfStructureExpressions(ControlStructureType type, SrcDstRange range){
        switch (type){
            case IF:
                numberOfConditionalExpression[0] +=
                        coveredLength(range.getSrcRange());
                numberOfConditionalExpression[1] +=
                        coveredLength(range.getDstRange());
                break;
            case FOR:
                numberOfForExpression[0] +=
                        coveredLength(range.getSrcRange());
                numberOfForExpression[1] +=
                        coveredLength(range.getDstRange());
                break;
            case WHILE:
                numberOfWhileExpression[0] +=
                        coveredLength(range.getSrcRange());
                numberOfWhileExpression[1] +=
                        coveredLength(range.getDstRange());
                break;
        }
    }

    public void updateStructureActionNumber(ControlStructureType type, int number){
        switch (type){
            case IF:
                numberTotalConditionRelatedActionNumber += number;
                break;
            case FOR:
                numberTotalForRelatedActionNumber += number;
                break;
            case WHILE:
                numberTotalWhileRelatedActionNumber += number;
                break;
        }
    }
}