package org.mashirocl.utildiff;

/**
 * @author mashirocl@gmail.com
 * @since 2024/05/25 13:47
 */
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.core.internal.resources.File;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class UtilDiff {
    public FileDiffLocation calcFileDiffLocation(String preCommitSourceCode, String postCommitSourceCode){
        List<String> preCommitLines = List.of(preCommitSourceCode.split("\\R"));
        List<String> postCommitLines = List.of(postCommitSourceCode.split("\\R"));
        Patch<String> patch = DiffUtils.diff(preCommitLines, postCommitLines);

        FileDiffLocation fileDiffLocation = new FileDiffLocation();

        for (AbstractDelta<String> delta : patch.getDeltas()) {
            switch (delta.getType()) {
                case INSERT:
                    for(int i = delta.getTarget().getPosition();i<delta.getTarget().getPosition()+delta.getTarget().getLines().size();i++){
                        fileDiffLocation.getAdded().add(i);
                    }
                    break;
                case DELETE:
                    for(int i = delta.getSource().getPosition();i<delta.getSource().getPosition()+delta.getSource().getLines().size();i++){
                        fileDiffLocation.getRemoved().add(i);
                    }
                    break;
                case CHANGE:
                    for(int i = delta.getSource().getPosition();i<delta.getSource().getPosition()+delta.getSource().getLines().size();i++){
                        fileDiffLocation.getModifiedLeft().add(i);
                        System.out.println(delta.getSource().getLines());
                    }
                    for(int i = delta.getTarget().getPosition();i<delta.getTarget().getPosition()+delta.getTarget().getLines().size();i++){
                        fileDiffLocation.getModifiedRight().add(i);
                    }
                    break;
                default:
                    break;
            }
        }
        log.info("fileDiffLocation: {}",fileDiffLocation);
        return fileDiffLocation;

    }

    public static void main(String[] args) throws IOException {
        // Read pre-commit and post-commit file contents
        String preCommitFilePath = "/Users/leichen/data/OSS/mbassador/pre-MesagePublicationTest.txt";
        String postCommitFilePath = "/Users/leichen/data/OSS/mbassador/post-MessagePubliationTest.txt";

        UtilDiff utilDiff = new UtilDiff();
        FileDiffLocation fileDiffLocation = utilDiff.calcFileDiffLocation(Files.readString(Path.of(preCommitFilePath)), Files.readString(Path.of(postCommitFilePath)));
        System.out.println(fileDiffLocation.getAdded());
        System.out.println(fileDiffLocation.getModifiedLeft());
        System.out.println(fileDiffLocation.getModifiedRight());

    }
}
