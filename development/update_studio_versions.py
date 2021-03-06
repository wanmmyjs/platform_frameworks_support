#!/usr/bin/env python3

import os, re, sys
from urllib.request import urlopen

#### ####
# This script attempts to scrape the Android Studio release archive to find the matching version of
# Studio for a given version of the Android Gradle Plugin, and then update
# studio_versions.properties accordingly.
#
# Example usage: './development/update_studio_versions.py 3.4.0-beta03' where 3.4.0-beta03
# is the AGP version.
#### ####

def usage():
    print(USAGE_MESSAGE)
    sys.exit(1)

def get_studio_version_string(agp_version):
    """Returns the named Android Studio version for a given AGP version"""
    split = agp_version.split("-")
    # No stability suffix, i.e agp_version is similar to '3.4.0', not '3.4.0-beta03'
    if len(split) == 1:
        return agp_version

    # Split up the version into two parts, prefix being similar to '3.4.0', and suffix 'beta03'
    prefix, suffix = split

    # Remove the patch number as this is not used by Studio outside of stable releases
    major_version = prefix[:-2]

    suffix = suffix.replace("alpha", "Alpha")
    suffix = suffix.replace("beta", "Beta")
    suffix = suffix.replace("canary", "Canary")
    suffix = suffix.replace("rc", "RC")

    release_type = suffix[:-2]

    minor_version = suffix[-2:]
    if minor_version[:1] == "0":
        minor_version = minor_version[-1:]

    studio_version_string = "Android Studio %s %s %s" % (major_version, release_type, minor_version)
    return studio_version_string

def parse_studio_information(studio_version_string):
    """Finds the download link and corresponding information for a given Android Studio version"""
    with urlopen('https://developer.android.com/studio/archive.html') as response:
        html = response.read().decode("utf8")
        inner_frame_url = re.findall(r'iframe src="(.*?)"', html, re.MULTILINE)[0]
        with urlopen(inner_frame_url) as response:
            html = response.read().decode("utf8")
            version_information = re.findall(studio_version_string + REGEX, html, re.MULTILINE)[0]
            return {
                "version": version_information[0],
                "idea_major_version": version_information[1],
                "build_number": version_information[2]
            }

def update_studio_versions(agp_version, studio_information):
    """Updates studio_versions.properties with the given AGP version and Studio information"""
    script_path = os.path.dirname(__file__)
    if script_path != "":
        os.chdir(script_path)

    version = studio_information["version"]
    idea_major_version = studio_information["idea_major_version"]
    build_number = studio_information["build_number"]

    studio_versions_file = open("../buildSrc/studio_versions.properties", "w")
    studio_versions_file.write(
        STUDIO_VERSIONS_TEMPLATE % (agp_version, version, idea_major_version, build_number))

REGEX = r"[\s\S]+?https://dl.google.com/dl/android/studio/ide-zips/" \
        r"(.*?)/android-studio-ide-(.*?)\.(.*?)-windows.zip"

USAGE_MESSAGE = """Usage: ./development/update_studio_versions.py <agp_version>
Example usage: ./development/update_studio_versions.py 3.4.0-beta03

This script attempts to scrape the Android Studio release archive to find the matching version of
Studio for a given version of the Android Gradle Plugin, and then update studio_versions.properties
accordingly.
"""

SUCCESS_MESSAGE = """
Successfully updated studio_versions.properties.
Run \033[92m./studiow\033[0m and \033[92m./gradlew assembleDebug\033[0m to verify nothing broke!

Note: you may get warnings about gradle being unable to find artifacts: this is fine; run
\033[92m./development/importMaven/import_maven_artifacts.py --name=<missing_artifact>\033[0m
to add them to the prebuilts directory - you will need to commit these as well. If this command
fails, appending \033[92m:linux\033[0m to the end of the artifact name sometimes works.
"""

STUDIO_VERSIONS_TEMPLATE = """# WARNING: This file is automatically generated.
# To update, use './development/update_studio_versions.py <agp_version>'
# This file specifies the version of the Android Gradle Plugin and Android Studio to use.

# Android Gradle Plugin version
agp=%s
# Version properties for ./studiow, which correspond to the version of AGP
studio_version=%s
idea_major_version=%s
studio_build_number=%s
"""

def main(args):
    if len(args) != 2:
        usage()
    agp_version = args[1]
    studio_version_string = get_studio_version_string(agp_version)
    studio_information = parse_studio_information(studio_version_string)
    update_studio_versions(agp_version, studio_information)
    print(SUCCESS_MESSAGE)

main(sys.argv)
