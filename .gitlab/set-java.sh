#!/bin/bash
if [ "$1" -eq "17" ]; then
  echo 'export JAVA_HOME="/usr/lib/jvm/java-17-openjdk/"' > ~/.mavenrc
elif [ "$1" -eq "11" ]; then
  echo 'export JAVA_HOME="/usr/lib/jvm/java-11-openjdk/"' > ~/.mavenrc
fi