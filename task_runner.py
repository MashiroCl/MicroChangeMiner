import subprocess
from multiprocessing import Pool
import os

def execute_java_command(command):
    try:
        print(f"Executing: {command}")
        subprocess.run(command, shell=True)
    except Exception as e:
        print(f"Error executing: {command}\nError message: {str(e)}")


def load_commands(path):
    with open(path) as f:
        data = f.readlines()
    return [each.strip() for each in data]


if __name__ == "__main__":
    command_paths = "./commands.txt"
    java_commands = load_commands(command_paths)
    print(java_commands)
    p = Pool()
    results = p.map(execute_java_command, java_commands)
    p.close()
    p.join()
    print("All Java commands executed.")
