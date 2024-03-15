package org.mashirocl.refactoringminer;

import org.w3c.dom.ranges.Range;

import java.util.List;

/**
 * @author mashirocl@gmail.com
 * @since 2024/03/10 15:39
 */
public class MethodLevelMapping {
    public List<MappingUnit> mappingUnitList;

}

class MappingUnit{
    public String fileName;
    public Range range;
}
