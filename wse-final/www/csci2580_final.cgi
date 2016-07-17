#!/bin/bash

# This section borrowed from: http://www.yolinux.com/TUTORIALS/BashShellCgi.html
# Parses the parameters passed to the script

# Save the old internal field separator.
OIFS="$IFS"

# Set the field separator to & and parse the QUERY_STRING at the ampersand.
IFS="${IFS}&"
set $QUERY_STRING
Args="$*"
IFS="$OIFS"

# Next parse the individual "name=value" tokens.
QUERY=""
for i in $Args ;do
    # Set the field separator to =
    IFS="${OIFS}="
    set $i
    IFS="${OIFS}"
    # QUERY="`echo $2 | sed 's|%20| |g'`"
    case $1 in 
	query) QUERY="`echo $2 | sed 's|%20| |g' | sed 's|+| |g' `"
            ;;
        *)     echo "<hr>Warning:"\
                    "<br>Unrecognized variable \'$1\' passed by FORM in QUERY_STRING.<hr>"
            ;;
        esac
done
# End of borrowed section

echo Content-type: text/html
echo ""

cd csci2580-final

/usr/bin/java -Xmx512M -cp ".:./Lucene/lucene-core-5.4.1.jar:./Lucene/lucene-analyzers-common-5.4.1.jar:./Lucene/lucene-queryparser-5.4.1.jar:./JTidy/jtidy-r938.jar:./cli/commons-cli-1.3.1.jar" retriever.Retriever -q "$QUERY" -i index

cd -
