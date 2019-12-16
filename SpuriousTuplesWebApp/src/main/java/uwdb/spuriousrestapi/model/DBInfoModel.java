package uwdb.spuriousrestapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import uwdb.discovery.dependency.approximate.entropy.NewSmallDBInMemory;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DBInfoModel {
    public String name;
    public int numAttributes;
    public long numTuples;
    public long numCells;

    public DBInfoModel(NewSmallDBInMemory db) {
        this.name = db.filename;
        this.numAttributes = db.numAtt;
        this.numTuples = db.numTuples;
        this.numCells = db.numCells;
    }
}