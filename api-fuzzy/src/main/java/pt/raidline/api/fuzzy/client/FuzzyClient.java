package pt.raidline.api.fuzzy.client;

import pt.raidline.api.fuzzy.logging.CLILogger;
import pt.raidline.api.fuzzy.model.AppArguments;
import pt.raidline.api.fuzzy.processors.paths.model.Path;
import pt.raidline.api.fuzzy.processors.schema.component.ComponentBuilder;
import pt.raidline.api.fuzzy.processors.schema.model.SchemaBuilderNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;

import static pt.raidline.api.fuzzy.assertions.AssertionUtils.aggregateErrors;
import static pt.raidline.api.fuzzy.assertions.AssertionUtils.internalAssertion;

//todo: the httpclient creation needs more care.. this is just to make a POC of this
public class FuzzyClient {

    private static final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(20))
            //.authenticator(Authenticator.getDefault()) //todo: see about this
            .build();

    public void executeRequest(Map<String, SchemaBuilderNode> schemaGraph,
                               Iterator<Path> paths, AppArguments.Arg server) {

        aggregateErrors("Client Preparation")
                .onError("Schema cannot be null or empty",
                        () -> schemaGraph != null && !schemaGraph.isEmpty())
                .onError("Paths cannot be null or empty",
                        () -> paths != null && paths.hasNext())

                .complete();
        //We have the paths, we have the schemas, we have the server
        //Now we just need to create the multiple requests from one client

        //Here is a draft of that

        var requestBuilder = HttpRequest.newBuilder()
                .timeout(Duration.ofMinutes(2)) //todo: see about this
                .header("Content-Type", "application/json");
        //.POST(BodyPublishers.ofFile(Paths.get("file.json")))


        do {
            var path = paths.next();

            //todo: just for testing find a path that has a post (for body)

            if (path.operations()
                    .stream()
                    .noneMatch(po -> po.op().isPost())) {
                continue;
            }

            var request = buildRequest(server.value(), requestBuilder,
                    path, schemaGraph);

            //todo: just to get quick feedback
            /*client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(CLILogger::info);*/

            try {
                CLILogger.debug("Sending request : [%s]", request);

                var res = client.send(request, HttpResponse.BodyHandlers.ofString());

                CLILogger.info("Response for server : [%s]", res.body());
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            //.thenApply(HttpResponse::body)
            //.thenAccept(CLILogger::info);
        } while (paths.hasNext());
    }

    private HttpRequest buildRequest(String basePath, HttpRequest.Builder builder,
                                     Path path, Map<String, SchemaBuilderNode> graph) {

        //todo: we are just doing a post for now to be sure we can make the request
        //todo: next step is todo all operations for a path


        var postOpOpt = path.operations()
                .stream()
                .filter(po -> po.op().isPost())
                .findFirst();

        internalAssertion("Testing Get Operation",
                postOpOpt::isPresent);

        var postOp = postOpOpt.get();

        var body = graph.get(
                ComponentBuilder.trimSchemaKeyFromRef(postOp.request().ref()));

        //todo: do not forget headers, resolve path params!
        return builder
                .uri(URI.create(basePath + path.key()))
                .POST(HttpRequest.BodyPublishers.ofString(
                        body.buildSchema()
                )).build();
    }
}
