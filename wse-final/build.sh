#!/bin/bash

javac -cp "./evaluator:./util:./indexer:./htmlparser:./Lucene/lucene-core-5.4.1.jar:./Lucene/lucene-analyzers-common-5.4.1.jar:./Lucene/lucene-queryparser-5.4.1.jar:./JTidy/jtidy-r938.jar:./cli/commons-cli-1.3.1.jar:./jwi/edu.mit.jwi_2.4.0.jar:." evaluator/*.java util/*.java indexer/*.java htmlparser/*.java retriever/*.java

echo "Ran build commands."