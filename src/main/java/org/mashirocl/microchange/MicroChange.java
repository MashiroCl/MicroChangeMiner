package org.mashirocl.microchange;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/14 13:40
 */

@AllArgsConstructor
@Getter
@NoArgsConstructor(force = true)
public class MicroChange {
    private final String type;
    private final String action; // if not result of GumTree, use some description of how the micro change is conducted
    private SrcDstRange srcDstRange;

    public MicroChange(final String type, final String action){
        this.type = type;
        this.action = action;
    }

    public static MicroChange of(final String type, final String action){
        return new MicroChange(type, action);
    }
    public static MicroChange of(final String type, final String action, final SrcDstRange srcDstRange){
        return new MicroChange(type, action, srcDstRange);
    }

    @Override
    public String toString(){
        String lineRanges = String.valueOf(srcDstRange);
        return "type: "+ type + "\naction: "+action + "\nline ranges: "+lineRanges;
    }

}
