package uwdb.spuriousrestapi.api;

import uwdb.discovery.dependency.approximate.common.sets.IAttributeSet;
import uwdb.discovery.dependency.approximate.common.sets.AttributeSet;
import uwdb.discovery.dependency.approximate.entropy.NewSmallDBInMemory;
import uwdb.discovery.dependency.approximate.entropy.NewSmallDBInMemory.CanceledJobException;
import uwdb.discovery.dependency.approximate.entropy.NewSmallDBInMemory.DecompositionInfo;
import uwdb.discovery.dependency.approximate.entropy.NewSmallDBInMemory.DecompositionRunStatus;
import uwdb.spuriousrestapi.model.DBInfoModel;
import uwdb.spuriousrestapi.model.ErrorModel;
import uwdb.spuriousrestapi.model.SpuriousResponseModel;
import java.lang.IllegalStateException;
import java.sql.SQLException;
import java.lang.IllegalArgumentException;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("api/spurioustuples")
public class SpuriousTuples {
    private static NewSmallDBInMemory db = null;

    public static void setUpDB(NewSmallDBInMemory _db) {
        if (db != null) {
            throw new IllegalStateException("Database already initialized");
        }

        if (_db == null) {
            throw new IllegalArgumentException("Database cannot be null");
        }

        db = _db;
    }

    private static ExecutorService service = Executors.newWorkStealingPool();

    @POST
    @Path("/decomasync")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"Spurious Tuples"}, summary = "Asynchronous Decomposition Test",
            description = "Asynchronously get decomposition information, may need to poll multiple time to get final result.")

    @ApiResponse(responseCode = "400", description = "Invalid set of clusters",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorModel.class)))
    @ApiResponse(responseCode = "500", description = "Server error",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorModel.class)))
    @ApiResponse(responseCode = "200",
            description = "Receive feedback right away, either PENDING, RUNNING, FINISHED, FAILED or CANCELED.",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = SpuriousResponseModel.class)))

    public Response processClustersAsync(@Parameter(description = "List of seperated cluster",
            array = @ArraySchema(arraySchema = @Schema(type = "integer", format = "int32",
                    example = "[[0,1,2,3,4],[3,4,5,6,7,8]]")),
            required = true) List<List<Integer>> clusters) {
        Set<IAttributeSet> clustersSet = new HashSet<>();
        AttributeSet temp = new AttributeSet(db.numAtt);
        try {
            for (List<Integer> cluster : clusters) {
                AttributeSet clusterBit = new AttributeSet(cluster, db.numAtt);
                clustersSet.add(clusterBit);
                temp.or(clusterBit);
            }

            if (temp.cardinality() != db.numAtt) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(
                    new ErrorModel("Union of attributes must be [0.." + (db.numAtt - 1) + "]"))
                    .build();
        }

        try {
            DecompositionRunStatus dRunStatus = db.submitJob(clustersSet);
            return Response.ok(new SpuriousResponseModel(dRunStatus)).build();
        } catch (InterruptedException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorModel("Interrupted, please retry")).build();
        }
    }

    @POST
    @Path("/decom")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"Spurious Tuples"}, summary = "Asynchronous Decomposition Test",
            description = "Synchronously get decomposition information, will wait until finish, failed or canceled. Limited to 5 minutes.")
    @ApiResponse(responseCode = "400", description = "Invalid set of clusters",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorModel.class)))
    @ApiResponse(responseCode = "500", description = "Server error",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorModel.class)))
    @ApiResponse(responseCode = "550",
            description = "Job failed (memory, int 64 bit constraint, database error), no retry needed.",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorModel.class)))
    @ApiResponse(responseCode = "555", description = "Job canceled. Can retry if needed.",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorModel.class)))
    @ApiResponse(responseCode = "555", description = "Timeout, 5 minutes passed and no data",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorModel.class)))
    @ApiResponse(responseCode = "200", description = "Successfully ran and return data",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = DecompositionInfo.class)))

    public Response processClustersSync(@Parameter(description = "List of seperated cluster",
            array = @ArraySchema(arraySchema = @Schema(type = "integer", format = "int32",
                    example = "[[0,1,2,3,4],[3,4,5,6,7,8]]")),
            required = true) List<List<Integer>> clusters) {
        Set<IAttributeSet> clustersSet = new HashSet<>();
        AttributeSet temp = new AttributeSet(db.numAtt);
        try {
            for (List<Integer> cluster : clusters) {
                AttributeSet clusterBit = new AttributeSet(cluster, db.numAtt);
                clustersSet.add(clusterBit);
                temp.or(clusterBit);
            }

            if (temp.cardinality() != db.numAtt) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(
                    new ErrorModel("Union of attributes must be [0.." + (db.numAtt - 1) + "]"))
                    .build();
        }

        Future<Response> future = service.submit(() -> {
            try {
                DecompositionInfo dInfo = db.submitJobSynchronous(clustersSet);
                return Response.ok(dInfo).build();
            } catch (InterruptedException e) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(new ErrorModel("Interrupted, please retry")).build();
            } catch (CanceledJobException e1) {
                return Response.status(555).entity(new ErrorModel("Job canceled")).build();
            } catch (Exception e2) {
                return Response.status(550).entity(new ErrorModel("Execution failed")).build();
            }
        });

        try {
            return future.get(5, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            try {
                db.cancelJob(clustersSet);
            } catch (InterruptedException | SQLException e1) {
            }
            return Response.status(560).entity(new ErrorModel("Timeout!!!")).build();
        } catch (Exception e1) {
            try {
                db.cancelJob(clustersSet);
            } catch (InterruptedException | SQLException e2) {
            }
            return Response.status(500).entity(new ErrorModel("Unknown error")).build();
        }
    }

    @POST
    @Path("/cancel")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"Spurious Tuples"}, summary = "Asynchronous Decomposition Test",
            description = "Synchronously get decomposition information, will wait until finish, failed or canceled. Limited to 5 minutes.")
    @ApiResponse(responseCode = "400", description = "Invalid set of clusters",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorModel.class)))
    @ApiResponse(responseCode = "500", description = "Server error",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorModel.class)))
    @ApiResponse(responseCode = "200", description = "Successfully cancel if such job exists")

    public Response cancelClustersDecompJob(@Parameter(description = "List of seperated cluster",
            array = @ArraySchema(arraySchema = @Schema(type = "integer", format = "int32",
                    example = "[[0,1,2,3,4],[3,4,5,6,7,8]]")),
            required = true) List<List<Integer>> clusters) {
        Set<IAttributeSet> clustersSet = new HashSet<>();
        AttributeSet temp = new AttributeSet(db.numAtt);
        try {
            for (List<Integer> cluster : clusters) {
                AttributeSet clusterBit = new AttributeSet(cluster, db.numAtt);
                clustersSet.add(clusterBit);
                temp.or(clusterBit);
            }

            if (temp.cardinality() != db.numAtt) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(
                    new ErrorModel("Union of attributes must be [0.." + (db.numAtt - 1) + "]"))
                    .build();
        }

        try {
            db.cancelJob(clustersSet);
            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorModel("Unknown error occurred")).build();
        }
    }

    @GET
    @Path("/db")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"Database"}, summary = "Get Database Information",
            description = "Data include name, number of attributes, number of tuples, number of cells")
    @ApiResponse(responseCode = "200", description = "This always success",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = DBInfoModel.class)))
    public DBInfoModel getDBInfo() {
        return new DBInfoModel(db);
    }
}
