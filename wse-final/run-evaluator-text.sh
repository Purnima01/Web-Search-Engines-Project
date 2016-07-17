#!/bin/bash
# rm -f $2/*

INPUT_DIR=input-sample
OUTPUT_DIR=output-sample

rm -rf $OUTPUT_DIR

mkdir $OUTPUT_DIR

java -cp ".:./cli/commons-cli-1.3.1.jar:./JTidy/jtidy-r938.jar:./jwi/edu.mit.jwi_2.4.0.jar" evaluator.DuplicateGenerator -i $INPUT_DIR -o $OUTPUT_DIR -d dict -m text
