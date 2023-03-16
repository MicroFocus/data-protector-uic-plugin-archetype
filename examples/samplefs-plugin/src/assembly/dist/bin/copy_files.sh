#!/bin/bash

sourceDir=$1
destDir=$2
count=0
mkdir -p "$destDir/temp"
while read srcFile; do
  cp -p --parents "$srcFile" "$destDir/temp"
  ((count++))
done
mv "$destDir/temp/$sourceDir"/* "$destDir"
rm -r "$destDir/temp"
echo "$count"
