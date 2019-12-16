package uwdb.spuriousrestapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import uwdb.discovery.dependency.approximate.entropy.NewSmallDBInMemory.DecompositionInfo;
import uwdb.discovery.dependency.approximate.entropy.NewSmallDBInMemory.DecompositionRunStatus;
import uwdb.discovery.dependency.approximate.entropy.NewSmallDBInMemory.DecompositionRunStatus.StatusCode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class SpuriousResponseModel {
    public StatusCode status;
    public DecompositionInfo dInfo;

    public SpuriousResponseModel(DecompositionRunStatus runStatus) {
        status = null;
        dInfo = null;
        synchronized (runStatus) {
            status = runStatus.status();

            if (status == StatusCode.FINISHED) {
                dInfo = runStatus.dInfo();
            }
        }
    }
}
