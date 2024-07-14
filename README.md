# MicroChangeMiner

## MicroChange


## Installation
```bash
./gradlew shadowJar
```

## Preparation
### 1. RefactoringMiner
### 2. git-stein

## Usage
### 1. Mine Refactorings
```bash
java -jar  build/libs/miner-1.0-SNAPSHOT-all.jar refmine ./RefactoringMiner-3.0.4/bin <target_repository>
```
### 2. Convert repository to method-level
```bash
java -jar <git-stein.jar> <target_repository> -o <output_path> @historage-jdt --no-original --no-classes --no-fields --parsable --mapping
```

### 3. Build commit map
```bash
java -jar build/libs/miner-1.0-SNAPSHOT-all.jar commitMap -p <method_level_repo_git_path (target_repo/.git)> -o <commit_map_json>
```

### 4. Mine micro-changes
```bash
java -jar  build/libs/miner-1.0-SNAPSHOT-all.jar mine <method_level_repo_git_path (target_repo/.git)> <output_json_path> --csv <output_csv_path> --map <commit_map_json> --refactoring <refactoring_directory> --original <original_repo_git_path> --notCoveredPath <output_not_covered_json>
```
