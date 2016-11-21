package io.rapidpro.surveyor.net;

import com.google.gson.JsonElement;

import java.util.List;

import io.rapidpro.flows.utils.JsonUtils;
import io.rapidpro.flows.utils.Jsonizable;

/**
 * A file containing multiple definitions, the standard
 * export format for RapidPro.
 */
public class Definitions implements Jsonizable {
    public int version;
    public List<FlowDefinition> flows;

    @Override
    public JsonElement toJson() {
        return JsonUtils.object(
                "version", version,
                "flows", JsonUtils.toJsonArray(flows)
        );
    }

    public String toString() {
        return toJson().toString();
    }

}
