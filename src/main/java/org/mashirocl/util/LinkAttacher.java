package org.mashirocl.util;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.core.internal.registry.osgi.OSGIUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/24 15:16
 */
@Slf4j
public class LinkAttacher {

    public static void main(String [] args){

        String link = LinkAttacher.attachLink("2a151b090cf70e012d61b37d1f4dc1039012d173",
                LinkAttacher.searchLink("my-refactoring-toy-example"));
    }

    static final ClassLoader classLoader = LinkAttacher.class.getClassLoader();
    static final String linkResource = "githublinks.txt";

    public static String searchLink(String repoName){
        InputStream inputStream =  classLoader.getResourceAsStream(linkResource);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        try {
            while((line = bufferedReader.readLine())!=null){
                if(line.contains(repoName)){
                    return line;
                }
            }

        }
        catch (IOException e){
            log.error(e.getMessage(),e);
        }
        return "";
    }


    public static String attachLink(String commitID, String link){
        return link.split("\\.git")[0]+"/commit/"+commitID;
    }
}
