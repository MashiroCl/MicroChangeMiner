package org.mashirocl.command;

/**
 * @author mashirocl@gmail.com
 * @since 2025/04/22 14:13
 */
public class ProcessingStats {
    int[] totalADCodeChangeLines = new int[]{0, 0};
    int[] microADChangeCoveredLines = new int[]{0, 0};
    int[] mrADChangeCoveredLines = new int[]{0, 0};
    int[] numberOfConditionalExpression = new int[]{0, 0};
    int conditionalExpressionChangeContainedCommit = 0;
    int numberTotalConditionRelatedActionNumber = 0;
    int numberMicroChangeContainedConditionRelatedAction = 0;
    int numberOfFilesProcessed = 0;
}