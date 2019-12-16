package uwdb.spuriousrestapi;

import uwdb.discovery.dependency.approximate.entropy.NewSmallDBInMemory;
import uwdb.spuriousrestapi.api.SpuriousTuples;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

/**
 * Main class.
 *
 */
@Slf4j
public class Main {
    // Base URI the Grizzly HTTP server will listen on
    public static String BASE_URI;

    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
     * 
     * @return Grizzly HTTP server.
     */
    public static HttpServer startServer(NewSmallDBInMemory db) {
        // create a resource config that scans for JAX-RS resources and providers
        // in uwdb package
        SpuriousTuples.setUpDB(db);
        final ResourceConfig rc = new JerseyRestApplication(db).packages("uwdb");
        // MyApplication());
        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
    }

    /**
     * Main method.
     * 
     * @param args
     * @throws Exception
     * @throws NumberFormatException
     */
    public static void main(String[] args) {
        HttpServer server = null;
        try {
            if (args.length < 4) {
                throw new IllegalArgumentException();
            }
            int portNum = Integer.parseInt(args[3]);
            if (portNum < 0 || portNum > 65535) {
                throw new IllegalArgumentException();
            }
            BASE_URI = "http://localhost:" + portNum + "/";
            try (NewSmallDBInMemory db = new NewSmallDBInMemory(args[0], Integer.parseInt(args[1]),
                    Boolean.valueOf(args[2]));) {
                server = startServer(db);
                HttpHandler httpHandler =
                        new CLStaticHttpHandler(HttpServer.class.getClassLoader(), "/swagger/");
                server.getServerConfiguration().addHttpHandler(httpHandler, "/swagger/");

                log.info(String.format(
                        "Jersey app started with WADL available at " + "%sapplication.wadl",
                        BASE_URI));
                log.info(String.format(
                        "REST API gateway swagger can be accessed by the url \n    %sswagger/\nHit enter to stop server...",
                        BASE_URI));
                System.in.read();
            } catch (Exception e) {
                if (e instanceof IllegalArgumentException) {
                    throw (IllegalArgumentException) e;
                }

                log.error("Error starting server: " + e.getLocalizedMessage(), e);
            } finally {
                if (server != null) {
                    server.shutdownNow();
                }
            }
        } catch (IllegalArgumentException e) {
            usage();
            System.exit(1);
        }
    }

    public static void usage() {
        System.out.println(
                "Main <database.csv> <number of attributes> <has header (true/false)> <port number>");
    }
}

