# Micro-Change Miner
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](./LICENSE)
[![Build Status](https://github.com/MashiroCl/MicroChangeMiner/actions/workflows/ci.yml/badge.svg)](https://github.com/MashiroCl/MicroChangeMiner/actions)
[![Release](https://img.shields.io/github/v/release/MashiroCl/MicroChangeMiner.svg)](https://github.com/MashiroCl/MicroChangeMiner/releases)

This is the repository for Micro-change Miner.

`Micro-changes` are a set of code change
operations described in natural language, designed to bridge
the cognitive divide by translating the textual diffs into more
understandable natural-language described operations


---

## 📋 Table of Contents

- [Micro-Change Miner](#micro-change-miner)
  - [📋 Table of Contents](#-table-of-contents)
  - [📦 Quickstart with `make`](#-quickstart-with-make)
  - [🧰 Prerequisites](#-prerequisites)
    - [1. git-stein](#1-git-stein)
    - [2. RefactoringMiner](#2-refactoringminer)
    - [3. Gradle](#3-gradle)
  - [🔧 Build](#-build)
  - [🛠️ How to Mine Micro-Changes from a Repository (Step-by-Step)](#️-how-to-mine-micro-changes-from-a-repository-step-by-step)
    - [Step 1. Convert the target git repository to method-level repository](#step-1-convert-the-target-git-repository-to-method-level-repository)
    - [Step 2. Get commit-map](#step-2-get-commit-map)
    - [Step 3. Mine refactorings](#step-3-mine-refactorings)
    - [Step 4. Mine micro-changes](#step-4-mine-micro-changes)
  - [🧩 How to Define Your Own Micro-Change Types](#-how-to-define-your-own-micro-change-types)
    - [1. Implement the micro-change in package `org.mashirocl.microchange`](#1-implement-the-micro-change-in-package-orgmashiroclmicrochange)
    - [2. Register your own micro-change for mining in `org.mashirocl.command.MineCommand#loadMicroChanges()`](#2-register-your-own-micro-change-for-mining-in-orgmashiroclcommandminecommandloadmicrochanges)
  - [📚 Complete Micro-Change Catalog](#-complete-micro-change-catalog)
  - [📄 Publications](#-publications)

---


## 📦 Quickstart with `make`
If you're using macOS or Linux and have make installed, you can skip manual setup. Just run:
```bash
$ make example-analysis
```
This will:
- Clone and build required tools (`git-stein`, `RefactoringMiner`)
- Set up directories
- Mine micro-changes from an example repository [mbassador](https://github.com/bennidi/mbassador)
---

<small> If you prefer to install things manually, follow the steps below instead of using `make` <small>
## 🧰 Prerequisites
Install the following tools before using Micro-Change Miner

### 1. git-stein

Used to convert Git repositories into method-level history.

* [git-stein](https://github.com/sh5i/git-stein): Convert Git repository to method-level
```shell
$ git clone https://github.com/sh5i/git-stein.git
$ cd git-stein
$ ./gradlew executableJar
```

### 2. RefactoringMiner

We use the [RefactoringMiner](https://github.com/tsantalis/RefactoringMiner), which is the state of the art refactoring mining tool, to capture refactorings.

  * Note that in the paper,  we extend the RefactoringMiner3.0.4 by considering the refactoring affected elements' locations

### 3. Gradle
Install via Homebrew:
* [gradle](https://gradle.org/install/)
```shell
$ brew install gradle
```

---
## 🔧 Build
Clone and build the project:

```shell
$ git clone git@github.com:MashiroCl/MicroChangeMiner.git
$ cd MicroChangeMiner
$ ./gradlew shadowJar
```


---

## 🛠️ How to Mine Micro-Changes from a Repository (Step-by-Step)

### Step 1. Convert the target git repository to method-level repository
```shell
$ java -jar <git-stein_path/build/libs/git-stein.jar> <target_repo_path> -o <output_repo_path> @historage-jdt --no-original --no-classes --no-fields --parsable --mapping 
```

### Step 2. Get commit-map
Obtain a map from a method-level repository whose key is the original repository sha1, value is the corresponding method-level repository sha1, i.e. <original_sha1:method-level_sha1>
```shell
$ java -jar build/libs/miner-1.0-SNAPSHOT-all.jar commitMap -p <method_level_repo_path/.git> -o <output_jsonfile.json>
```

### Step 3. Mine refactorings
```shell
$ java -jar build/libs/miner-1.0-SNAPSHOT-all.jar refmine <RefactoringMiner_path>  <original_repo_path> <output_dir> >./runLog/<repo_name>_rm.log
```
### Step 4. Mine micro-changes
```shell
$ java -jar  build/libs/miner-1.0-SNAPSHOT-all.jar mine <method_level_repo_path/.git> <output_json_path.json> --csv <output_csv_path.csv> --map <commit_map_path> --refactoring <mined_refactoring_directory> --original <original_repo_path/.git> --notCoveredPath <output_path_for_uncovered> >runLog/<repo_name>.log 
```

Example for `mbassador`:
```shell
$ java -jar  build/libs/miner-1.0-SNAPSHOT-all.jar mine ./method_level/mbassador/.git ./mined/mbassador.json --csv ./mined/mbassador.csv --map ./commitMap/mbassador.json --refactoring ./minedRefactoring/mbassador --original ./OSS/mbassador/.git --notCoveredPath ./notCovered/mbassador.json >runLog/mbassador.log
```

---
## 🧩 How to Define Your Own Micro-Change Types
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

### 2. Register your own micro-change for mining in `org.mashirocl.command.MineCommand#loadMicroChanges()`
```java
    public static void loadMicroChanges(PatternMatcher patternMatcherGumTree) {
        patternMatcherGumTree.addMicroChange(new AddConjunctOrDisjunct());
        ...
        patternMatcherGumTree.addMicroChange(new YourMicroChange());

    }
```

---

## 📚 Complete Micro-Change Catalog
You can refer to the complete catalog of micro-change in [Catalog](https://github.com/salab/Micro-Change-Catalog/).

---
## 📄 Publications
The following article includes the details of the micro-changes and the miner.
We encourage contributions to the micro-changes or the miner.

Lei Chen, Michele Lanza, Shinpei Hayashi: [''Understanding Code Change with Micro-Changes''](https://conf.researchr.org/details/icsme-2024/icsme-2024-papers/31/Understanding-Code-Change-with-Micro-Changes). In Proceedings of the 40th IEEE International Conference on Software Maintenance and Evolution (ICSME 2024). Flagstaff, AZ, USA, oct, 2024.

📖 [Read on arXiv](https://www.arxiv.org/abs/2409.09923)

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