package uwdb.spuriousrestapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletProperties;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.extern.slf4j.Slf4j;
import uwdb.discovery.dependency.approximate.entropy.NewSmallDBInMemory;
import uwdb.spuriousrestapi.jersey.CorsFilter;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;


@Slf4j
public class JerseyRestApplication extends ResourceConfig {
    public JerseyRestApplication(NewSmallDBInMemory db) {
        log.info("Setting up hk2");

        packages("uwdb.spuriousrestapi");
        JacksonJaxbJsonProvider jacksonJaxbJsonProvider = new JacksonJaxbJsonProvider();
        ObjectMapper oMapper = JsonMapper.builder().addModule(new ParameterNamesModule())
                .addModule(new Jdk8Module()).addModule(new JavaTimeModule()).build();

        jacksonJaxbJsonProvider.setMapper(oMapper);

        register(jacksonJaxbJsonProvider);
        register(new CorsFilter());
        openApiLoader(db.filename);
        property(ServletProperties.FILTER_FORWARD_ON_404, true);
    }

    private void openApiLoader(String dbname) {
        OpenAPI oas = new OpenAPI();
        Info info = new Info().title("Swagger Spurious Tuples Web App For " + dbname)
                .description("Get data info and spurious tuples from " + dbname).version("v1");

        oas.info(info);
        OpenApiResource oApiResource = new OpenApiResource();
        SwaggerConfiguration oasConfig = new SwaggerConfiguration().openAPI(oas).prettyPrint(true)
                .resourcePackages(java.util.stream.Stream.of("uwdb.spuriousrestapi.api")
                        .collect(java.util.stream.Collectors.toSet()));
                        
        oApiResource.setOpenApiConfiguration(oasConfig);
        register(oApiResource);
    }
}
