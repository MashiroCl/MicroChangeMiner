package org.mashirocl.util;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/08 10:52
 */
@Slf4j
public class RepositoryAccess implements AutoCloseable {
    @Getter
    private final Repository repository;

    @Getter
    private RevWalk walkCache;

    @Getter
    private ObjectReader readerCache;

    public RepositoryAccess(final Repository repository) {
        this.repository = repository;
        repository.incrementOpen();
    }

    public RepositoryAccess(final Path path) {
        this.repository =  createRepository(path);
    }

    public RepositoryAccess inherit() {
        return new RepositoryAccess(repository);
    }

    /**
     * Opens Git repository.
     */
    private static Repository createRepository(final Path path) {
        try {
            final FileRepositoryBuilder builder = new FileRepositoryBuilder();
            return builder.setGitDir(path.toFile()).readEnvironment().findGitDir().build();
        } catch (final IOException e) {
            log.error("Invalid repository path: {}", path);
            return null;
        }
    }

    /**
     * Creates the formatter.
     */
    private static DiffFormatter createFormatter(final Repository repo) {
        final DiffFormatter formatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
        formatter.setRepository(repo);
        formatter.setDiffComparator(RawTextComparator.DEFAULT);
        formatter.setDetectRenames(true);
        return formatter;
    }

    /**
     * Obtains a RevWalk.
     */
    protected RevWalk getWalk() {
        if (walkCache == null) {
            walkCache = new RevWalk(repository);
        }
        return walkCache;
    }

    /**
     * Obtains an ObjectReader.
     */
    protected ObjectReader getReader() {
        if (readerCache == null) {
            readerCache = repository.newObjectReader();
        }
        return readerCache;
    }

    /**
     * Resolves a revision name to a RevCommit.
     */
    public RevCommit resolve(final String name) {
        try {
            final ObjectId commitId = repository.resolve(name);
            return getWalk().parseCommit(commitId);
        } catch (final IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Walk commits.
     */
    public Iterable<RevCommit> walk(final String commitFrom, final String commitTo) {
        final RevWalk walk = getWalk();

        // from: exclusive (from, to]
        if (commitFrom != null) {
            try {
                final RevCommit c = walk.parseCommit(repository.resolve(commitFrom));
                log.info("Range from [exclusive]: {} ({})", commitFrom, c.getId().name());
                walk.markUninteresting(c);
            } catch (final IOException e) {
                log.error("Invalid rev: {} ({})", commitFrom, e);
            }
        }

        // end: inclusive (from, to]
        if (commitTo != null) {
            try {
                final RevCommit c = walk.parseCommit(repository.resolve(commitTo));
                log.info("Range to (inclusive): {} ({})", commitTo, c.getId().name());
                walk.markStart(c);
            } catch (final IOException e) {
                log.error("Invalid rev: {} ({})", commitTo, e);
            }
        }
        walk.setRevFilter(RevFilter.NO_MERGES);
        return walk;
    }

    /**
     * Reads a blob object.
     */
    public String readBlob(final ObjectId blobId) {
        try {
            final ObjectLoader loader = getReader().open(blobId, Constants.OBJ_BLOB);
            final RawText rawText = new RawText(loader.getCachedBytes());
            // TODO UTF-8 only
            return rawText.getString(0, rawText.size(), false);
        } catch (final IOException e) {
            log.error(e.getMessage(), e);
            return "";
        }
    }

    /**
     * Gets the changes done in a commit, compared to its first parent.
     */
    public List<DiffEntry> getChanges(final RevCommit c) {
        try (final DiffFormatter fmt = createFormatter(repository)) {
            // gives null for a root commit
            final ObjectId parentId = c.getParentCount() == 1 ? c.getParent(0).getId() : null;
            return fmt.scan(parentId, c.getId());
        } catch (final IOException e) {
            log.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public String readFile(final String commitName, final String path) {
        try {
            final ObjectId commitId = repository.resolve(commitName);
            RevWalk revWalk = new RevWalk(repository);
            final RevCommit commit = getWalk().parseCommit(commitId);
            final TreeWalk treeWalk = new TreeWalk(repository);
            treeWalk.addTree(commit.getTree());
            treeWalk.setRecursive(true);
            treeWalk.setFilter(PathFilter.create(path));
            if (!treeWalk.next()) {
                return null;
            }
            ObjectId blobId = treeWalk.getObjectId(0);
            return readBlob(blobId);
        } catch (final IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void close() {
        if (readerCache != null) {
            readerCache.close();
        }
        if (walkCache != null) {
            walkCache.close();
        }
        repository.close();
    }
}

