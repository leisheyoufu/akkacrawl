#!/bin/bash

SCRIPT="$0"
AC_HOME=`dirname "$SCRIPT"`/..

# makeAC_HOME absolute
AC_HOME=`cd "$AC_HOME"; pwd`
AC_CLASSPATH=$AC_HOME/libs

if [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA="$JAVA_HOME/bin/java"
else
    JAVA=`which java`
fi

"$JAVA" -Des.path.home="$AC_HOME" -cp "$AC_CLASSPATH" com.lei.akkacrawl.AkkaCrawl