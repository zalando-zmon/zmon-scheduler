#!/bin/sh

exec java $JAVA_OPTS $(java-dynamic-memory-opts) -jar zmon-scheduler.jar
