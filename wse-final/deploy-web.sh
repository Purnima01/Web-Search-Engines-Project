#!/bin/bash


cd /home/es2697/public_html

rm -rf docs
mkdir docs
cp ~/wse-final/output-wiki/*.htm* docs/
chmod a+r -R docs
chmod a+x -R docs

cp -f ~/wse-final/www/csci2580_final.html .

chmod a+r csci2580_final.html
chmod a+x csci2580_final.html

cd /home/es2697/public_html/cgi-bin

cp -f ~/wse-final/www/csci2580_final.cgi . 

chmod a+r csci2580_final.cgi
chmod a+x csci2580_final.cgi

rm -rf csci2580-final
mkdir csci2580-final

cp -R ~/wse-final/index ./csci2580-final/

cp -R ~/wse-final/Lucene/ ./csci2580-final/

cp -R ~/wse-final/JTidy/ ./csci2580-final/

cp -R ~/wse-final/cli/ ./csci2580-final/

mkdir csci2580-final/retriever
mkdir csci2580-final/util

cp ~/wse-final/retriever/*.class csci2580-final/retriever
cp ~/wse-final/util/*.class csci2580-final/util

chmod a+r -R csci2580-final/

chmod a+x -R csci2580-final/