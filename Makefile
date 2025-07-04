# Default paths
EXPERIMENT_DIR := experiment
DATASET_DIR := $(EXPERIMENT_DIR)/dataset
RAW_DIR := $(DATASET_DIR)/raw
RUNLOG := runLog
METHOD_LEVEL_DIR := $(DATASET_DIR)/method_level
OUTPUT_DIR := $(EXPERIMENT_DIR)/output
COMMITMAP_DIR := $(OUTPUT_DIR)/commitMap
REFACTORING_DIR := $(OUTPUT_DIR)/refactoring
MICRO_CHANGE_JSON_PATH := $(OUTPUT_DIR)/micro-change/json
MICRO_CHNANGE_CSV_PATH := $(OUTPUT_DIR)/micro-change/csv
NOT_COVERED_PATH := $(OUTPUT_DIR)/not_covered
GIT_STEIN_JAR := ./git-stein/build/libs/git-stein-all.jar
REFACTORINGMINER_DIR := ./RefactoringMiner/RefactoringMiner-3.0.4/bin/
EXAMPLE_REPO := $(RAW_DIR)/mbassador
MINER_JAR := build/libs/miner-1.0-SNAPSHOT-all.jar 

git-stein:
	git clone https://github.com/sh5i/git-stein.git
	cd git-stein && ./gradlew executableJar

refactoring-miner:
	curl -LO https://github.com/tsantalis/RefactoringMiner/releases/download/3.0.4/RefactoringMiner-3.0.4.zip
	unzip RefactoringMiner-3.0.4.zip -d RefactoringMiner

build:
	./gradlew shadowJar

prepare_directory:
	mkdir -p $(RAW_DIR) $(METHOD_LEVEL_DIR) $(COMMITMAP_DIR) $(REFACTORING_DIR) $(RUNLOG) $(MICRO_CHANGE_JSON_PATH) $(NOT_COVERED_PATH) $(MICRO_CHNANGE_CSV_PATH)

clone_example_repo:
	cd $(RAW_DIR) && git clone git@github.com:bennidi/mbassador.git

convert_example_to_method_level:
	java -jar $(GIT_STEIN_JAR) $(RAW_DIR)/mbassador -o $(METHOD_LEVEL_DIR)/mbassador @historage-jdt --no-original --no-classes --no-fields --parsable --mapping 

commit_map_example:
	java -jar $(MINER_JAR) commitMap -p $(METHOD_LEVEL_DIR)/mbassador/.git -o $(COMMITMAP_DIR)/mbassador.json 
	
mine_refactoring_example:
	java -jar $(MINER_JAR) refmine $(REFACTORINGMINER_DIR) $(RAW_DIR)/mbassador/ $(REFACTORING_DIR)/mbassador >$(RUNLOG)/mbassador_rm.log

mine_micro_change_example:
	java -jar  $(MINER_JAR) mine $(METHOD_LEVEL_DIR)/mbassador/.git $(MICRO_CHANGE_JSON_PATH)/mbassador.json --csv $(MICRO_CHNANGE_CSV_PATH)/mbassador.csv --map  $(COMMITMAP_DIR)/mbassador.json --refactoring $(REFACTORING_DIR)/mbassador --original $(RAW_DIR)/mbassador/.git  --notCoveredPath  $(NOT_COVERED_PATH)/mbassador.json >runLog/mbassador.log 


example-analysis:
	git-stein refactoring-miner build prepare_directory clone_example_repo convert_example_to_method_level commit_map_example mine_refactoring_example mine_micro_change_example

.PHONY: git-stein refactoring-miner build prepare_directory clone_example_repo convert_example_to_method_level commit_map_example mine_refactoring_example mine_micro_change_example