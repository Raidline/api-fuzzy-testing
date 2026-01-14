package pt.raidline.api.fuzzy.runner.component;

import pt.raidline.api.fuzzy.model.ApiDefinition;

@FunctionalInterface
public interface OnReference {
    ApiDefinition.Schema onReferenceFound(String key);
}
