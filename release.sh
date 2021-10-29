#!/bin/bash
# This simple script increments the versions in the java and javascript projects, then tags the release.
# It doesn't push the changes so that you can check it's done things correctly.

# Abort on an error
set -e

die () {
    echo >&2 "$@"
    exit 1
}

echo $1 | grep -E -q '^[0-9\.]+$' || die "Numeric argument required, '$1' provided"

version=$1

echo Releasing $1

(cd frontend; npm --no-git-tag-version version $version; git add package.json package-lock.json)
(cd backend; mvn versions:set -DnewVersion=$1; git add pom.xml)
git commit -m "Release $1"
git tag $1
(cd backend; mvn versions:set -DnextSnapshot=true; git add pom.xml)
git commit -m "Increment version"
