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
$ java -jar build/libs/miner-1.0-SNAPSHOT-all.jar commitMap <repo_path/.git> -o <output_jsonfile.json>
```

### 3. Mine refactorings
```shell
$ java -jar  <RefactoringMiner_path/build/libs/miner-1.0-SNAPSHOT-all.jar> refmine <original_repo_path> <output_path> >./runLog/<repo_name>_rm.log
```
### 4. Mine micro-changes
```shell
$ java -jar  build/libs/miner-1.0-SNAPSHOT-all.jar mine <method_level_repo_path> <output_json_path.json> --csv <output_csv_path.csv> --map <commit_map_path> --refactoring <mined_refactoring_directory> --original <original_repo_path/.git> --notCoveredPath <output_path_for_uncovered> >runLog/<repo_name>.log 
```
e.g. for repository `mbassador`
```shell
$ java -jar  build/libs/miner-1.0-SNAPSHOT-all.jar mine ./method_level/mbassador/.git ./mined/mbassador.json --csv ./mined/mbassador.csv --map ./commitMap/mbassador.json --refactoring ./minedRefactoring/mbassador --original ./OSS/mbassador/.git --notCoveredPath ./notCovered/mbassador.json >runLog/mbassador.log
```