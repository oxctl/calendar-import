#!/bin/bash
# This simple script gets the current release and increments the release number.
# Abort on an error
set -e

die () {
    echo >&2 "$@"
    exit 1
}
if [ "$1" != "Major" ] && [ "$1" != "Minor" ] && [ "$1" != "Patch" ]; then
  die "Only accepts Major, Minor or Patch as argument."
fi

### Increments the part of the string
## $1: version itself
## $2: number of part: 0 – major, 1 – minor, 2 – patch
increment_version() {

  major=`echo "$1" | cut -d "." -f 1  | sed 's/[^0-9]*//g'`
  minor=`echo "$1" | cut -d "." -f 2  | sed 's/[^0-9]*//g'`
  patch=`echo "$1" | cut -d "." -f 3  | sed 's/[^0-9]*//g'`

  case "$2" in
    "Major")
      major=$((major + 1))
      minor=0
      patch=0
      ;;
    "Minor")
      minor=$((minor + 1))
      patch=0
      ;;
    "Patch")
      patch=$((patch + 1))
      ;;
  esac
  echo "$major.$minor.$patch"
}

# This attempts to prevent rouge tags from preventing us from doing a release
LATEST_RELEASE=`git tag -l "[0-9]*.[0-9]*.[0-9]*" --sort=-creatordate | head -1`
NEW_RELEASE=`increment_version $LATEST_RELEASE $1`
echo $NEW_RELEASE

