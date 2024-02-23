import csv
from pathlib import Path

LOG_PATH = "./runLog/"

CSV_HEADER = [
    'repo', 'github_commit','processed_commit','processed_method','removed_lines','added_lines', "#_of_changed_lines", "average_#_of_changed_lines",
    "#_of_edit_actions", "average_#_of_edit_actions", 'micro_change_contained_actions',"micro-change_ratio_on_edit_action_level",
    'removed_lines_covered_by_action', 'added_lines_covered_by_action',  "#_of_lines_coverd_by_edit_actions",
    'removed_lines_covered_by_micro_change','added_lines_covered_by_micro_change',"#_of_lines_covered_by_micro-changes","micro_change_ratio_on_line_level"
]

def load_logs():
    res = {}
    root_path = Path(LOG_PATH)
    for p in root_path.iterdir():
        res[p.stem] = mine_data_in_log(p)
    return res

def mine_data_in_log(p:Path):
    res = {}
    with open(p,"r") as f:
        data = f.readlines()
        for line in data:
            if "Edit Script obtained for " in line:
                res["processed_commit"] = int(line.split("Edit Script obtained for ")[1].split("commits")[0])
            elif "Edit script computed, " in line:
                res["processed_method"] = int(line.split("Edit script computed, ")[1].split(" methods processed")[0])
            elif "Total number of actions: " in line:
                res["#_of_edit_actions"] = int(line.split("Total number of actions: ")[1])
            elif "Micro-change contained actions: " in line:
                res["micro_change_contained_actions"] = int(line.split("Micro-change contained actions: ")[1])
            elif "total tree removed lines: " in line:
                res["removed_lines_covered_by_action"] = int(line.split("total tree removed lines: ")[1])
            elif "total tree added lines: " in line:
                res["added_lines_covered_by_action"] = int(line.split("total tree added lines: ")[1])
            elif "removed lines covered by micro-change: " in line:
                res["removed_lines_covered_by_micro_change"] = int(line.split("removed lines covered by micro-change: ")[1])
            elif "added lines covered by micro-change: " in line:
                res["added_lines_covered_by_micro_change"] = int(line.split("added lines covered by micro-change: ")[1])
    return res


def aggregate_data(log_data, commit_num, lines):
    for repo in log_data:
        log_data[repo]["github_commit"] = commit_num[repo]
        log_data[repo]["removed_lines"] = lines[repo]["removed"]
        log_data[repo]["added_lines"] = lines[repo]["added"]
    
    for repo in log_data:
        log_data[repo]["#_of_changed_lines"] = log_data[repo]["removed_lines"]+log_data[repo]["added_lines"]
        log_data[repo]["average_#_of_changed_lines"] = log_data[repo]["#_of_changed_lines"]/log_data[repo]["processed_method"]
        log_data[repo]["average_#_of_edit_actions"] = log_data[repo]["#_of_edit_actions"]/log_data[repo]["processed_method"]
        log_data[repo]["micro-change_ratio_on_edit_action_level"] = log_data[repo]["micro_change_contained_actions"]/log_data[repo]["#_of_edit_actions"]
        log_data[repo]["#_of_lines_coverd_by_edit_actions"] = log_data[repo]["removed_lines_covered_by_action"]+log_data[repo]["added_lines_covered_by_action"]
        log_data[repo]["#_of_lines_covered_by_micro-changes"] = log_data[repo]["removed_lines_covered_by_micro_change"]+log_data[repo]["added_lines_covered_by_micro_change"]
        log_data[repo]["micro_change_ratio_on_line_level"] = log_data[repo]["#_of_lines_covered_by_micro-changes"]/log_data[repo]["#_of_lines_coverd_by_edit_actions"]
    return log_data

def build_csv(data, csv_file):
    flattened_data = []
    for top_level_key, nested_dict in data.items():
        flattened_row = {'repo': top_level_key}
        for nested_key, value in nested_dict.items():
            flattened_row[nested_key] = value
        flattened_data.append(flattened_row)
    flattened_data_sorted = sorted(flattened_data, key=lambda x: x['github_commit'])
    with open(csv_file, mode='w', newline='') as file:
        writer = csv.DictWriter(file, fieldnames=CSV_HEADER)
        writer.writeheader()
        for row in flattened_data_sorted:
            writer.writerow(row)

if __name__ == "__main__":
    commit_num = {"android-demos":368, "javapoet":919,"mbassador":342,"jfinal":517}
    lines = {
        "android-demos":{"added":414,"removed":526}, 
        "javapoet":{"added":4618,"removed":5127}, 
        "mbassador":{"added":2580,"removed":2334}, 
        "jfinal":{"added":8707,"removed":8063}}

    data_from_log = load_logs()
    aggregated_data = aggregate_data(data_from_log, commit_num, lines)

    build_csv(aggregated_data,"test.csv")