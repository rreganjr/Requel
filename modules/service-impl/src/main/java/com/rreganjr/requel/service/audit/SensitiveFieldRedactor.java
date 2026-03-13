package com.rreganjr.requel.service.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generic JSON redactor that scans field names and replaces values matching
 * sensitive patterns with "[REDACTED]". No command-type-specific branching —
 * just pattern matching on key names.
 * <p>
 * Sensitive keys are configurable via {@code requel.audit.sensitive-keys} in
 * application.properties (comma-separated, case-insensitive). Defaults to:
 * password, repassword, secret, token, credential, credentials, apikey, api_key
 */
@Component
public class SensitiveFieldRedactor {

    private static final String REDACTED = "[REDACTED]";

    private final ObjectMapper objectMapper;
    private final Set<String> sensitiveKeys;

    public SensitiveFieldRedactor(
            ObjectMapper objectMapper,
            @Value("${requel.audit.sensitive-keys:password,repassword,secret,token,credential,credentials,apikey,api_key}")
            List<String> sensitiveKeys) {
        this.objectMapper = objectMapper;
        this.sensitiveKeys = sensitiveKeys.stream()
                .map(String::toLowerCase)
                .map(String::trim)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Serialize the input object to JSON, redact sensitive fields, and return
     * the redacted JSON string. Returns null if input is null.
     */
    public String redact(Object input) {
        if (input == null) {
            return null;
        }
        try {
            JsonNode tree = objectMapper.valueToTree(input);
            redactNode(tree);
            return objectMapper.writeValueAsString(tree);
        } catch (Exception e) {
            return "{\"_redactionError\": \"Could not serialize input\"}";
        }
    }

    private void redactNode(JsonNode node) {
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = obj.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (sensitiveKeys.contains(field.getKey().toLowerCase())) {
                    obj.put(field.getKey(), REDACTED);
                } else {
                    redactNode(field.getValue());
                }
            }
        } else if (node.isArray()) {
            ArrayNode arr = (ArrayNode) node;
            for (int i = 0; i < arr.size(); i++) {
                redactNode(arr.get(i));
            }
        }
    }
}
