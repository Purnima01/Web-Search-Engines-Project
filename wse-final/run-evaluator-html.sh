#!/bin/bash
# rm -f $2/*

INPUT_DIR=wiki-input
OUTPUT_DIR=output-wiki

rm -rf $OUTPUT_DIR

mkdir $OUTPUT_DIR

java -cp ".:./cli/commons-cli-1.3.1.jar:./JTidy/jtidy-r938.jar:./jwi/edu.mit.jwi_2.4.0.jar:./Lucene/lucene-core-5.4.1.jar:./Lucene/lucene-analyzers-common-5.4.1.jar" evaluator.DuplicateGenerator -i $INPUT_DIR -o $OUTPUT_DIR -d dict -m html
