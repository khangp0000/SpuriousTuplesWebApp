
#!/bin/bash
# usage: runTestServer.sh <database.csv> <number of attributes> <has header (true/false)> <port number>
# example: ./runTestServer.sh  adult.csv 15 false
# with no argument, it is ran with stock "nursery.csv 9 false 9876"

if ! [[ $# -eq 0 ]] ; then
    db=$(realpath "$1")
    if [[ $? -ne 0 ]] ; then 
        echo "file \"$1\" does not exist."
        exit 1
    fi
fi
script=$(realpath "$0")
cd "$(dirname "$script")"
cd RelationalDecomposition
mvn install
cd ../SpuriousTuplesWebApp
if [[ $# -eq 0 ]] ; then
    echo "Running with nursery.csv on port 9874"
    mvn compile exec:java
else
    mvn compile exec:java -Dexec.args="\"$db\" $2 $3 $4"
fi
