package org.mashirocl.command;

import org.mashirocl.dao.CommitDAO;
import org.mashirocl.dao.NotCoveredDAO;

import java.util.List;

/**
 * @author mashirocl@gmail.com
 * @since 2025/04/22 14:13
 */
public class ProcessingResults {
    final List<CommitDAO> commitDAOs;
    final List<NotCoveredDAO> notCovered;
    final ProcessingStats stats;

    public ProcessingResults(List<CommitDAO> commitDAOs, List<NotCoveredDAO> notCovered, ProcessingStats stats) {
        this.commitDAOs = commitDAOs;
        this.notCovered = notCovered;
        this.stats = stats;
    }
}

