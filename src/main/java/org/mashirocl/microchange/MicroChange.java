package org.mashirocl.microchange;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/14 13:40
 */

@AllArgsConstructor
@Getter
@ToString
@NoArgsConstructor(force = true)
public class MicroChange {
    private final String type;
    private final String action; // if not result of GumTree, use some description of how the micro change is conducted

    public static MicroChange of(final String type, final String action){
        return new MicroChange(type, action);
    }
}
