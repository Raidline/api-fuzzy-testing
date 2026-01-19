package pt.raidline.api.fuzzy.processors.paths.model;

import pt.raidline.api.fuzzy.model.ApiDefinition;
import pt.raidline.api.fuzzy.model.HttpOperation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static pt.raidline.api.fuzzy.assertions.AssertionUtils.internalAssertion;

//todo: probably some of the Schemas (query, path, etc..), could be a SchemaBuilder to be invocated later.
public record Path(String key, List<PathOperation> operations) {

    public record PathOperation(HttpOperation op,
                                Map<ParameterLocation, List<PathParameter>> opParams,
                                Map<Integer, OperationExchange> successResponse,
                                Map<Integer, OperationExchange> errorResponses,
                                OperationExchange request) {
    }

    public record OperationExchange(String media, ApiDefinition.SchemaType type, String ref) {

        public boolean isEmptyExchange() {
            return this.media == null && type == null && ref == null;
        }
    }

    public record PathParameter(String key, ApiDefinition.Schema schema) {
    }

    public enum ParameterLocation {
        QUERY("query"), PATH("path"), HEADER("header");

        private final String key;

        ParameterLocation(String key) {
            this.key = key;
        }

        public static ParameterLocation fromString(String value) {
            var optRet = Arrays.stream(values())
                    .filter(v -> v.key.equalsIgnoreCase(value))
                    .findFirst();

            internalAssertion(
                    "Find ParameterLocation",
                    optRet::isPresent,
                    "Could not parse [ParameterLocation] from value : [%s]"
                            .formatted(value));

            return optRet.get();
        }
    }
}
