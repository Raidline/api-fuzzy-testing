package pt.raidline.api.fuzzy.model;

import java.util.function.Function;

public enum HttpOperation {
    GET(ApiDefinition.PathItem::get),
    POST(ApiDefinition.PathItem::post),
    PUT(ApiDefinition.PathItem::put),
    DELETE(ApiDefinition.PathItem::delete);

    public final Function<ApiDefinition.PathItem, ApiDefinition.Operation> fromItemToOp;

    HttpOperation(Function<ApiDefinition.PathItem, ApiDefinition.Operation> fromItemToOp) {
        this.fromItemToOp = fromItemToOp;
    }
}
