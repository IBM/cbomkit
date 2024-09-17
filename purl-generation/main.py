# Press ⌃R to execute it or replace it with your code.
# Press Double ⇧ to search everywhere for classes, files, tool windows, actions, and settings.

import os
import json
import yaml

resource_directory = "./resource"
version = "1.1"


def createJsonFile():
    json_object = dict()
    json_object["version"] = version

    for projectName in os.listdir(resource_directory):
        path = os.path.join(resource_directory, projectName)
        try:
            directories = os.listdir(path)

            for directory in directories:
                dir_path = os.path.join(path, directory)
                purl_file_path = os.path.join(dir_path, "purls.yml")
                entity = list()

                with open(purl_file_path, "r") as stream:
                    purls = yaml.safe_load(stream)
                    json_object[directory] = purls["purls"]
        except Exception as err:
            print("could not handle " + path + "\n " + str(err))

    # Serializing json
    json_object = json.dumps(json_object, indent=2)

    # Writing to sample.json
    with open("./purls.json", "w") as outfile:
        outfile.write(json_object)


def delete_ds_store_files():
    for root, dirs, files in os.walk(resource_directory):
        for file in files:
            if file == ".DS_Store":
                file_path = os.path.join(root, file)
                os.remove(file_path)


if __name__ == "__main__":
    delete_ds_store_files()
    createJsonFile()
