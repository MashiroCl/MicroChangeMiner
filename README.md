## Getting Started

This is the repository for Micro-change Miner.

## Prerequisites
* [git-stein](https://github.com/sh5i/git-stein): Convert Git repository to method-level
```shell
$ git clone https://github.com/sh5i/git-stein.git
$ cd git-stein
$ ./gradlew executableJar
```
* [RefactoringMiner](https://github.com/tsantalis/RefactoringMiner): The state of the art refactoring mining tool
  * We extend the RefactoringMiner3.0.4 by considering the refactoring affected elements' locations
* [gradle](https://gradle.org/install/)
```shell
$ brew install gradle
```

## Build
```shell
$ git clone git@github.com:MashiroCl/MicroChangeMiner.git
$ cd MicroChangeMiner
$ ./gradlew shadowJar
```

## How to mine micro-changes from a repository
### 1. Convert the target git repository to method-level repository
```shell
$ java -jar <git-stein_path/build/libs/git-stein.jar> <target_repo_path> -o <output_repo_path> @historage-jdt --no-original --no-classes --no-fields --parsable --mapping 
```

### 2. Get commit-map
Obtain a map from a method-level repository whose key is the original repository sha1, value is the corresponding method-level repository sha1, i.e. <original_sha1:method-level_sha1>
```shell
$ java -jar build/libs/miner-1.0-SNAPSHOT-all.jar commitMap -p <method_level_repo_path/.git> -o <output_jsonfile.json>
```

### 3. Mine refactorings
```shell
$ java -jar build/libs/miner-1.0-SNAPSHOT-all.jar refmine <RefactoringMiner_path>  <original_repo_path> <output_dir> >./runLog/<repo_name>_rm.log
```
### 4. Mine micro-changes
```shell
$ java -jar  build/libs/miner-1.0-SNAPSHOT-all.jar mine <method_level_repo_path/.git> <output_json_path.json> --csv <output_csv_path.csv> --map <commit_map_path> --refactoring <mined_refactoring_directory> --original <original_repo_path/.git> --notCoveredPath <output_path_for_uncovered> >runLog/<repo_name>.log 
```
e.g. for repository `mbassador`
```shell
$ java -jar  build/libs/miner-1.0-SNAPSHOT-all.jar mine ./method_level/mbassador/.git ./mined/mbassador.json --csv ./mined/mbassador.csv --map ./commitMap/mbassador.json --refactoring ./minedRefactoring/mbassador --original ./OSS/mbassador/.git --notCoveredPath ./notCovered/mbassador.json >runLog/mbassador.log
```


## How to define your own micro-change types
### 1. Implement the micro-change in package `org.mashirocl.microchange`
```java
public class YourMicroChange implements MicroChangePattern{
    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings) {
      // Implement the match strategy here 
      // if you need the actions for all, implement the same name method below
        return false;
    }

    @Override
    public boolean matchConditionGumTree(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions) {
      // Implement the match strategy here
        return false;
    }

    @Override
    public SrcDstRange getSrcDstRange(Action action, Map<Tree, Tree> mappings, Map<Tree, List<Action>> nodeActions, EditScriptStorer editScriptStorer) {
        // get the line range of this micro-change
        SrcDstRange srcDstRange = new SrcDstRange();
        // left side range

        // right side range

        return srcDstRange;
    }
}

```

### 2. Declare your own micro-change if you want it to be mined in `org.mashirocl.command.MineCommand#loadMicroChanges()`
```java
    public static void loadMicroChanges(PatternMatcher patternMatcherGumTree) {
        patternMatcherGumTree.addMicroChange(new AddConjunctOrDisjunct());
        ...
        patternMatcherGumTree.addMicroChange(new YourMicroChange());

    }
```

## Complete Micro-Change Catalog
You can refer to the complete catalog of micro-change in [Catalog](https://github.com/salab/Micro-Change-Catalog/).

## Publications
The following article includes the details of the micro-changes and the miner.
We encourage contributions to the micro-changes or the miner.

Lei Chen, Michele Lanza, Shinpei Hayashi: [''Understanding Code Change with Micro-Changes'']( https://www.arxiv.org/abs/2409.09923). In Proceedings of the 40th IEEE International Conference on Software Maintenance and Evolution (ICSME 2024). Flagstaff, AZ, USA, oct, 2024.
```
@inproceedings{chen2024understanding,
  title={Understanding Code Change with Micro-Changes},
  author={Chen, Lei and Lanza, Michele and Hayashi, Shinpei},
  booktitle={Proceedings of the 40th International Conference on Software Maintenance and Evolution },
  pages={363--374},
  year={2024},
  keywords = {Software maintenance;Codes;Natural languages;Focusing;Detectors;Programming;Cognitive science;Logic},
  doi = {10.1109/ICSME58944.2024.00041},
  url = {https://doi.ieeecomputersociety.org/10.1109/ICSME58944.2024.00041},
  organization={IEEE}
}
```