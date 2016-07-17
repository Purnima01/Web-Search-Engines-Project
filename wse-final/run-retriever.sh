#!/bin/bash

java -cp ".:./Lucene/lucene-core-5.4.1.jar:./Lucene/lucene-analyzers-common-5.4.1.jar:./Lucene/lucene-queryparser-5.4.1.jar:./JTidy/jtidy-r938.jar:./cli/commons-cli-1.3.1.jar" retriever.Retriever -i $1 -q "$2" 