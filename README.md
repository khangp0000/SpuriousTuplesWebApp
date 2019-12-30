Make sure to have submodule download and updated using:

```
git submodule update --init --recursive
```
## Run using script for linux (require maven)
```
runTestServer.sh <database.csv> <number of attributes> <has header (true/false)> <port number>
```

## Run using maven
Install dependency (Only needed to do once for each version):
```
cd RelationalDecomposition
mvn install
```
Then run the server:
```
cd SpuriousTuplesWebApp
mvn exec:java -Dexec.args="<database.csv> <number of attributes> <has header (true/false)> <port number>"
```
## Run using jar with dependencies
Download on release page of github.

```
java -cp SpuriousTuplesWebApp-***-jar-with-dependencies.jar uwdb.spuriousrestapi.Main <database.csv> <number of attributes> <has header (true/false)> <port number>
```
