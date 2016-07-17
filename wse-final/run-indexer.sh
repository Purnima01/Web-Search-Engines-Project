#!/bin/bash
# rm -f $2/*

INPUT_DIR=output-wiki
INDEX_DIR=index

rm -rf $INDEX_DIR

mkdir $INDEX_DIR

java -cp ".:./cli/commons-cli-1.3.1.jar:./JTidy/jtidy-r938.jar:./jwi/edu.mit.jwi_2.4.0.jar:./Lucene/lucene-core-5.4.1.jar:./Lucene/lucene-analyzers-common-5.4.1.jar" indexer.Main -docs $INPUT_DIR -index $INDEX_DIR
