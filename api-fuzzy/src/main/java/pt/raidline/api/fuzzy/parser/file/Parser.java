package pt.raidline.api.fuzzy.parser.file;

import pt.raidline.api.fuzzy.model.ApiDefinition;

public interface Parser {
    ApiDefinition parse(String file);
}
