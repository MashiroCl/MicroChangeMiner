package org.mashirocl.refactoringminer;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Range;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.mashirocl.editscript.DiffEditScriptWithSource;
import org.mashirocl.microchange.SrcDstRange;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mashirocl@gmail.com
 * @since 2024/03/24 7:56
 */

@Getter
@Setter
@Slf4j
public class RenameRefactoring extends Refactoring{
    private String oldName;
    private String newName;
    private String rename;

    public RenameRefactoring(String type, String description, List<SideLocation> leftSideLocations, List<SideLocation> rightSideLocations) {
        super(type, description, leftSideLocations, rightSideLocations);
    }

    public RenameRefactoring(JsonNode refactoringNode) {
        super(refactoringNode);
        String [] names = parseRenaming(refactoringNode);
        oldName = names[0];
        newName = names[1];
    }

    private String [] parseRenaming(JsonNode refactoringNode){
        //Rename Class Package Variable Attribute
        String leftSideCodeElement = refactoringNode.get("leftSideLocations").get(0).get("codeElement").toString();
        String originalName = parseName(leftSideCodeElement);
        String rightSideCodeElement = refactoringNode.get("rightSideLocations").get(0).get("codeElement").toString();
        String renamedName= parseName(rightSideCodeElement);
        rename = originalName+"@"+renamedName;
        return new String []{originalName,renamedName};
    }


    private String parseName(String sideCodeElement){
        String name = "";
        sideCodeElement = sideCodeElement.replace("\"","");
        switch (this.getType().replace("\"","")){
            case "Rename Class", "Move and Rename Class":
                name = sideCodeElement.substring(sideCodeElement.lastIndexOf(".")+1);
                break;
            case "Rename Attribute", "Rename Parameter", "Move and Rename Attribute":
                name = sideCodeElement.substring(0,sideCodeElement.lastIndexOf(" :")+1);
                break;
            case "Rename Method", "Move and Rename Method":
                //e.g. protected addAsynchronousDeliveryRequest(request MessagePublication) : MessagePublication
                name = sideCodeElement.substring(sideCodeElement.indexOf(" ")+1,sideCodeElement.indexOf("("));
                break;
        }
        return name;
    }

    public static boolean isRenameRefactoring(String type){
        return type.equals("Rename Class") || type.equals("Move and Rename Class")
                || type.equals("Rename Attribute") || type.equals("Rename Parameter")
                || type.equals("Move and Rename Attribute") || type.equals("Rename Method") || type.equals("Move and Rename Method");
    }

    public void attachLineRange(DiffEditScriptWithSource diffEditScriptWithSource, SrcDstRange range){
        Path oldPath = Path.of(diffEditScriptWithSource.getDiffEntry().getOldPath());
        Path newPath = Path.of(diffEditScriptWithSource.getDiffEntry().getNewPath());
        range.getSrcRange().asRanges().forEach(p->this.getLeftSideLocations().add(new SideLocation(oldPath,p)));
        range.getDstRange().asRanges().forEach(p->this.getRightSideLocations().add(new SideLocation(newPath,p)));
    }
}
