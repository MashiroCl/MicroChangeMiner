package org.mashirocl.command;

import java.nio.file.Path;

/**
 * @author mashirocl@gmail.com
 * @since 2024/01/23 14:37
 */
public class CommandGenerator {

    public void getCommitMap(String repoPath, String outputPath){
        System.out.printf("java -jar build/libs/miner-1.0-SNAPSHOT-all.jar commitMap -p %s -o %s\n", repoPath, outputPath);
    }

    public void getCommitMap(String repoPath, String outputPath, String repo){
        System.out.printf("nohup java -jar build/libs/miner-1.0-SNAPSHOT-all.jar commitMap -p %s -o %s >%s 2>&1 &\n", repoPath, outputPath, "runLog/"+repo+"_mine.txt");
    }

    public void mineMicroChangeWithMap(String repoPath, String outputJsonPath, String csvOutput, String mapPath){
        System.out.printf("java -jar  build/libs/miner-1.0-SNAPSHOT-all.jar mine %s %s --csv %s --map %s \n", repoPath, outputJsonPath, csvOutput, mapPath);
    }
    public void mineMicroChangeWithMap(String repoPath, String outputJsonPath, String csvOutput, String mapPath, String repo){
        System.out.printf("nohup java -jar  build/libs/miner-1.0-SNAPSHOT-all.jar mine %s %s --csv %s --map %s >%s 2>&1 & \n", repoPath, outputJsonPath, csvOutput, mapPath, "runLog/"+repo+"_mine.txt");
    }

    public void mineRefactoring(String repoPath, String outputDirectory, String refactoringMinerPath, String repo){
        String titanProjectPath = "cd /home/salab/chenlei/semantic_lifter/mine/MicroChangeMiner && ";
        System.out.printf(titanProjectPath + "java -jar  build/libs/miner-1.0-SNAPSHOT-all.jar refmine %s %s %s >./runLog/%s_rm.log 2>&1\n",refactoringMinerPath, repoPath,outputDirectory , repo);
    }

    public void mineMicroChangeWithMapWithRefactoring(String repoPath, String outputJsonPath, String csvOutput, String mapPath, String refactoringPath, String originalRepoGit, String notCoveredPath){
        System.out.printf("nohup java -jar  build/libs/miner-1.0-SNAPSHOT-all.jar mine %s %s --csv %s --map %s --refactoring %s --original %s --notCoveredPath %s >runLog/%s.log 2>&1 & \n", repoPath, outputJsonPath, csvOutput, mapPath, refactoringPath, originalRepoGit,notCoveredPath, Path.of(repoPath).getParent().getFileName());
    }

    public void mineMicroChangeWithMapWithRefactoringSingleCommit(String repoPath, String outputJsonPath, String csvOutput, String mapPath, String refactoringPath, String originalRepoGit){
        System.out.printf("java -jar  build/libs/miner-1.0-SNAPSHOT-all.jar mine %s %s --csv %s --map %s --refactoring %s --original %s >runLog/temp/%s.log\n", repoPath, outputJsonPath, csvOutput, mapPath, refactoringPath, originalRepoGit, Path.of(repoPath).getParent().getFileName());
    }

    public void toMethodLevel(String repoPath, String outputRepoPath){
        String gitSteinPath = "/evo/homes/leichen/project/SemanticLifter/mine/git-stein/build/libs";
//        System.out.printf("java -jar %s/git-stein.jar %s -o %s @historage-jdt --no-original --no-classes --no-fields --parsable --mapping\n", gitSteinPath, repoPath, outputRepoPath);
        System.out.printf("nohup java -jar %s/git-stein.jar %s -o %s @historage-jdt --no-original --no-classes --no-fields --parsable --mapping 2>&1 &\n", gitSteinPath, repoPath, outputRepoPath);

    }

    public void toMethodLevel(String repoPath, String outputRepoPath, String gitSteinPath){
        System.out.printf("nohup java -jar %s/git-stein.jar %s -o %s --stream-size-limit=2000M @historage-jdt --no-original --no-classes --no-fields --parsable --mapping 2>&1 &\n", gitSteinPath, repoPath, outputRepoPath);
    }

    public static void main(String [] args){
        CommandGenerator cg = new CommandGenerator();

        String repo = "my-refactoring-toy-example";
//        repo = "javapoet";
//        repo = "jfinal";
        repo = "mbassador";
//        repo = "android-demos";
        repo = "retrolambda";
//        repo = "seyren";
//        repo = "truth";
//        repo = "java-algorithms-implementation";
//        repo = "zuul";

        String repoPath;
        String mapPath;
        String csvOutput;
        String jsonOutput;
        String refactoringPath;
        String originalRepoPath;
        String rmOutputDirectory;
        String notCoveredPath;
        String localRefactoringMinerPath = "/Users/leichen/project/semantic_lifter/RefactoringMiner-3.0.4/bin/";


        String [] repos = new String[]{"mbassador","my-refactoring-toy-example","javapoet", "jfinal","android-demos","retrolambda", "zuul"};
        for(String r:repos){
            repoPath = "/Users/leichen/data/parsable_method_level/"+r+"/.git";
            mapPath = "/Users/leichen/project/semantic_lifter/SemanticLifter/patternMatch/miner/commitMap/"+r+".json";
            csvOutput = "/Users/leichen/project/semantic_lifter/SemanticLifter/patternMatch/miner/mined/"+r+".csv";
            jsonOutput = "/Users/leichen/project/semantic_lifter/SemanticLifter/patternMatch/miner/mined/"+r+".json";
            rmOutputDirectory = "/Users/leichen/project/semantic_lifter/SemanticLifter/patternMatch/minedRefactoring/"+r;
            refactoringPath = "/Users/leichen/project/semantic_lifter/SemanticLifter/patternMatch/minedRefactoring/"+r;
            originalRepoPath = "/Users/leichen/data/OSS/"+r;
            notCoveredPath = "/Users/leichen/project/semantic_lifter/SemanticLifter/patternMatch/miner/notCovered/"+r+".json";
//            cg.getCommitMap(repoPath, mapPath);
//            cg.mineRefactoring(originalRepoPath, rmOutputDirectory,localRefactoringMinerPath,repo);
//            cg.mineMicroChangeWithMapWithRefactoring(repoPath, jsonOutput, csvOutput, mapPath, refactoringPath, originalRepoPath+"/.git",notCoveredPath);
//            cg.mineMicroChangeWithMapWithRefactoringSingleCommit(repoPath, jsonOutput, csvOutput, mapPath, refactoringPath, originalRepoPath+"/.git");
        }



        repos = new String[]{"dmix","go-lang-idea-plugin","orientdb","xabber-android","Aeron","camel","sshj","jOOQ",
                "BuildCraft","Terasology","drools","cas","docx4j","rstudio","javapoet","seyren","guacamole-client",
                "graylog2-server","android_frameworks_base","workflow-plugin","voltdb","hazelcast","jetty.project",
                "rest-assured","closure-compiler","deeplearning4j","presto","mbassador","spring-boot","Android-IMSI-Catcher-Detector",
                "grails-core","morphia","drill","crate","Osmand","quasar","hydra","realm-java","open-keychain","bitcoinj",
                "hive","assertj-core","liferay-plugins","elasticsearch","eureka","cascading","BroadleafCommerce","giraph",
                "antlr4","zuul","baasbox","restlet-framework-java","libgdx","aws-sdk-java","fabric8","infinispan",
                "android-async-http","redisson","spring-roo","structr","intellij-plugins","truth","jbpm","jboss-eap-quickstarts",
                "neo4j","robovm","java-algorithms-implementation","OpenTripPlanner","jeromq","core","mongo-java-driver","TextSecure",
                "RoboBinding","helios","querydsl","intellij-erlang","java-driver","languagetool","processing",
                "hibernate-orm","HikariCP","jodd","blueflood","Activiti","intellij-community","testng",
                "rest.li","jfinal","jackson-databind","mockito","jmonkeyengine","MPS","checkstyle","android","goclipse","jedis",
                "sonarqube","cassandra","geoserver","j2objc","buck","openhab","AntennaPod","JGroups","spring-data-neo4j",
                "spring-framework","cgeo","gradle","retrolambda","WordPress-Android","TinkersConstruct","android-demos",
                "k-9","wildfly","ratpack","spring-data-rest","gwt","tachyon","spring-integration"};

        // titan
//        for(String r: repos){
//            String titanRepoPath = "/home/salab/chenlei/semantic_lifter/mine/parsable_method_level/"+r+"/"+r+"/.git";
//            String titanMapPath = "/home/salab/chenlei/semantic_lifter/mine/commitMap/"+r+".json";
//            String titanCsvOutputPath = "/home/salab/chenlei/semantic_lifter/mine/mined/"+r+".csv";
//            String titanJsonOutput = "/home/salab/chenlei/semantic_lifter/mine/mined/"+r+".json";
//
//            cg.getCommitMap(titanRepoPath, titanMapPath, r);
//            cg.mineMicroChangeWithMap(titanRepoPath, titanJsonOutput, titanCsvOutputPath, titanMapPath, r);
//        }

        repos = new String[]{"antlr4","BuildCraft","go-lang-idea-plugin","realm-java","jodd","Android-IMSI-Catcher-Detector","bitcoinj","mockito","dmix","TinkersConstruct","Aeron","testng","guacamole-client","android","drill","quasar","robovm","jackson-databind","Conversations","helios","cascading","AntennaPod","hydra","java-driver","TextSecure","jersey","spring-data-neo4j","HikariCP","intellij-erlang","morphia","workflow-plugin","copycat","goclipse","byte-buddy","eureka","blueflood","jedis","rest-assured","rest.li","assertj-core","baasbox","RoboBinding","giraph","spring-data-rest","xabber-android","android-async-http","redisson","truth","java-algorithms-implementation","sshj","zuul","seyren","aws-sdk-java","retrolambda","javapoet","jeromq","davdroid","mbassador","android-demos","jfinal"};

        // 3/29 after excluding repositories that are not exist/ error when using git-stein
        repos = new String[]{"antlr4","BuildCraft","go-lang-idea-plugin","realm-java","jodd","Android-IMSI-Catcher-Detector","bitcoinj","mockito","dmix","Aeron","testng","guacamole-client","android","drill","quasar","robovm","jackson-databind","helios","cascading","AntennaPod","hydra","java-driver","TextSecure","spring-data-neo4j","HikariCP","intellij-erlang","morphia","workflow-plugin","copycat","goclipse","eureka","blueflood","jedis","rest-assured","rest.li","assertj-core","baasbox","RoboBinding","giraph","spring-data-rest","xabber-android","android-async-http","redisson","truth","java-algorithms-implementation","sshj","zuul","seyren","retrolambda","javapoet","jeromq","davdroid","mbassador","android-demos","jfinal"};
        // 3/29 21 failed repos
        repos = new String[]{"BuildCraft", "go-lang-idea-plugin", "realm-java", "jodd", "bitcoinj", "dmix", "Aeron","testng", "android", "drill", "robovm", "jackson-databind", "cascading", "TextSecure", "intellij-erlang", "morphia", "rest.li", "xabber-android", "redisson", "java-algorithms-implementation","sshj"};

        //3/30 3 failed repos
        repos = new String[]{"jeromq", "dmix", "drill"};
        String usiRefactoringMinerPath = "/evo/homes/leichen/project/SemanticLifter/RefactoringMiner-3.0.4/bin/";

        //4/4 why-we-refactor - 3/30.dataset
        repos = new String[]{"MPS", "liferay-plugins", "neo4j", "camel",
                "processing", "elasticsearch", "JGroups", "Osmand", "wildfly", "sonarqube", "voltdb", "languagetool",
                "grails-core", "hive", "fabric8", "hazelcast", "cassandra", "rstudio", "spring-framework", "drools",
                "BroadleafCommerce", "jetty.project", "restlet-framework-java", "orientdb", "gwt", "hibernate-orm",
                "infinispan", "libgdx", "WordPress-Android", "OpenTripPlanner", "spring-integration", "cgeo",
                "intellij-plugins", "k-9", "querydsl", "checkstyle", "geoserver", "cas", "presto", "openhab",
                "closure-compiler", "Activiti", "mongo-java-driver", "structr", "jmonkeyengine", "crate", "spring-roo",
                "spring-boot", "graylog2-server", "Terasology", "tachyon", "buck", "deeplearning4j", "docx4j",
                "open-keychain", "jOOQ", "jboss-eap-quickstarts", "core", "jbpm", "ratpack", "j2objc"};

        // 4/8 remaining repos commit number< 15k refactoringminer not run
        repos = new String[]{"crate", "jOOQ","Terasology",
                "Activiti","mongo-java-driver","TinkersConstruct","jmonkeyengine",
                "spring-roo","ratpack","j2objc","jbpm",
                "docx4j","core","jboss-eap-quickstarts"};

        // 4/8 remaining repos commit number< 15k git-stein not run
        repos = new String[]{"geoserver","TinkersConstruct", "deeplearning4j"};
        // git-stein failed on deeplearning4j
        repos = new String[]{"geoserver","TinkersConstruct"};


        // 4/9 remaining repos commit number< 15k: micro-change mining
        repos = new String[]{"processing", "checkstyle", "jOOQ",
                "Terasology", "spring-integration", "Activiti",
                "gwt", "restlet-framework-java", "mongo-java-driver", "TinkersConstruct",
                "querydsl", "jmonkeyengine", "spring-roo", "ratpack",
                "j2objc", "jbpm", "docx4j", "core",
                "jboss-eap-quickstarts"};

        // 4/9 18:35 RM on this repository is not finished
//        repos = new String[]{"crate"};

        // 4/10 jOOq is not finished

        //4/10 open-keychain is not runing the micro-change mining
        repos = new String[]{"open-keychain"};

        // usi
        for(String r: repos){
            String usiRepoPath = "/evo/homes/leichen/project/SemanticLifter/mine/dataset/original/"+r;
            usiRepoPath = "/evo/homes/leichen/project/SemanticLifter/EmpiricalStudy/dataset/whyWeRefactor/"+r;
            String usiMapPath = "/evo/homes/leichen/project/SemanticLifter/mine/experiment/commitMap/"+r+".json";
            String methodLevelRepoPath = "/evo/homes/leichen/project/SemanticLifter/mine/dataset/method-level2/"+r;
            String refactoringOutputDirectory = "/evo/homes/leichen/project/SemanticLifter/mine/dataset/minedRefactorings/"+r;

            String usiCsvOutputPath = "/evo/homes/leichen/project/SemanticLifter/mine/experiment/minedCSV/"+r+".csv";
            String usiJsonOutput = "/evo/homes/leichen/project/SemanticLifter/mine/experiment/minedJSON/"+r+".json";
            String usiNotCoveredPath = "/evo/homes/leichen/project/SemanticLifter/mine/experiment/notCovered/"+r+".json";


//            cg.mineRefactoring(usiRepoPath, refactoringOutputDirectory,usiRefactoringMinerPath, r);
//            cg.toMethodLevel(usiRepoPath,methodLevelRepoPath);
//            cg.getCommitMap(methodLevelRepoPath+"/.git", usiMapPath, r);
//            cg.mineMicroChangeWithMapWithRefactoring(methodLevelRepoPath+"/.git", usiJsonOutput, usiCsvOutputPath, usiMapPath, refactoringOutputDirectory, usiRepoPath+"/.git",usiNotCoveredPath);
        }


        // 4/5 usi.lut down use titan
        // 7/14 rerun experiment 73 repos
        repos = new String[]{"TextSecure","processing","checkstyle","jOOQ","Terasology","spring-integration","Activiti","gwt","antlr4","realm-java","restlet-framework-java","AntennaPod","redisson","mongo-java-driver","TinkersConstruct","querydsl","jmonkeyengine","BuildCraft","jackson-databind","open-keychain","guacamole-client","spring-roo","ratpack","mockito","j2objc","jodd","testng","assertj-core","bitcoinj","drill","jbpm","xabber-android","docx4j","morphia","go-lang-idea-plugin","android","core","blueflood","hydra","goclipse","rest.li","HikariCP","robovm","Android-IMSI-Catcher-Detector","dmix","cascading","quasar","helios","jedis","jboss-eap-quickstarts","rest-assured","intellij-erlang","spring-data-neo4j","truth","java-driver","baasbox","eureka","spring-data-rest","zuul","jeromq","giraph","RoboBinding","sshj","javapoet","android-async-http","java-algorithms-implementation","seyren","retrolambda","jfinal","android-demos","mbassador","geoserver","byte-buddy"};
        System.out.println(repos.length);
        for(String r: repos){
            String titanRepoPath = "/home/salab/chenlei/semantic_lifter/mine/dataset/"+r;
            String titanMapPath = "/home/salab/chenlei/semantic_lifter/mine/mined/commitMap/"+r+".json";
            String methodLevelRepoPath = "/home/salab/chenlei/semantic_lifter/mine/parsable_method_level/"+r;
            String refactoringOutputDirectory = "/home/salab/chenlei/semantic_lifter/mine/refactoring/"+r;
            String titanRefactoringMinerPath = "/home/salab/chenlei/semantic_lifter/mine/RefactoringMiner-3.0.4/bin";
            String titanGitStein = "/home/salab/chenlei/semantic_lifter/mine/git-stein/build/libs";

            String titanCsvOutputPath = "/home/salab/chenlei/semantic_lifter/mine/mined/minedCSV/"+r+".csv";
            String titanJsonOutput = "/home/salab/chenlei/semantic_lifter/mine/mined/minedJSON/"+r+".json";
            String titanNotCoveredPath = "/home/salab/chenlei/semantic_lifter/mine/mined/notCovered/"+r+".json";

//            cg.mineRefactoring(titanRepoPath, refactoringOutputDirectory,titanRefactoringMinerPath, r);
//            cg.toMethodLevel(titanRepoPath,methodLevelRepoPath, titanGitStein);
//            cg.getCommitMap(methodLevelRepoPath+"/.git", titanMapPath, r);
            cg.mineMicroChangeWithMapWithRefactoring(methodLevelRepoPath+"/.git", titanJsonOutput, titanCsvOutputPath, titanMapPath, refactoringOutputDirectory, titanRepoPath+"/.git",titanNotCoveredPath);
        }
    }
}
