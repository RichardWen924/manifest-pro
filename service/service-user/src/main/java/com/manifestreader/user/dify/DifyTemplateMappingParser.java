package com.manifestreader.user.dify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.exception.ErrorCode;
import com.manifestreader.user.model.vo.TemplateFieldMappingVO;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DifyTemplateMappingParser {

    private final ObjectMapper objectMapper;

    public DifyTemplateMappingParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParsedMappings parse(String difyResponse) {
        JsonNode responseRoot = readJson(difyResponse, "解析 Dify workflow 响应失败");
        String text = readFirstText(responseRoot,
                "/text",
                "/answer",
                "/data/text",
                "/data/answer",
                "/data/outputs/text",
                "/data/outputs/answer",
                "/outputs/text",
                "/outputs/answer",
                "/result/text");
        JsonNode mappingRoot = StringUtils.hasText(text)
                ? readJson(extractJsonPayload(text), "解析 Dify text JSON 失败")
                : responseRoot;
        JsonNode mappings = readFirstArray(mappingRoot,
                "/mappings",
                "/fields",
                "/field_mappings",
                "/fieldMappings",
                "/data/mappings",
                "/result/mappings",
                "/outputs/mappings",
                "/data/outputs/mappings");
        if (mappings == null) {
            mappings = findMappingArray(mappingRoot);
        }

        List<TemplateFieldMappingVO> result = new ArrayList<>();
        if (mappings != null) {
            int index = 1;
            for (JsonNode item : mappings) {
                String originalText = readText(item, "original_text", "originalText", "source_text", "sourceText", "value", "text");
                String placeholderKey = defaultText(
                        readText(item, "placeholder_key", "placeholderKey", "field_key", "fieldKey", "key", "name"),
                        "field_" + index
                );
                result.add(new TemplateFieldMappingVO(
                        originalText,
                        placeholderKey,
                        defaultText(readText(item, "data_type", "dataType", "type"), "string"),
                        readText(item, "description", "field_name", "fieldName", "label")
                ));
                index++;
            }
        }
        return new ParsedMappings(result, StringUtils.hasText(text) ? text : difyResponse);
    }

    private JsonNode readJson(String json, String errorMessage) {
        if (!StringUtils.hasText(json)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), errorMessage);
        }
        try {
            return objectMapper.readTree(json);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), errorMessage);
        }
    }

    private String extractJsonPayload(String value) {
        String text = value.trim();
        if (text.startsWith("```")) {
            int firstLineEnd = text.indexOf('\n');
            int lastFenceStart = text.lastIndexOf("```");
            if (firstLineEnd >= 0 && lastFenceStart > firstLineEnd) {
                return text.substring(firstLineEnd + 1, lastFenceStart).trim();
            }
        }
        int objectStart = text.indexOf('{');
        int arrayStart = text.indexOf('[');
        int start = objectStart >= 0 && arrayStart >= 0 ? Math.min(objectStart, arrayStart) : Math.max(objectStart, arrayStart);
        if (start >= 0) {
            int objectEnd = text.lastIndexOf('}');
            int arrayEnd = text.lastIndexOf(']');
            int end = Math.max(objectEnd, arrayEnd);
            if (end > start) {
                return text.substring(start, end + 1).trim();
            }
        }
        return text;
    }

    private String readFirstText(JsonNode root, String... pointers) {
        for (String pointer : pointers) {
            JsonNode node = root.at(pointer);
            if (!node.isMissingNode() && StringUtils.hasText(node.asText())) {
                return node.asText();
            }
        }
        return "";
    }

    private JsonNode readFirstArray(JsonNode root, String... pointers) {
        for (String pointer : pointers) {
            JsonNode node = root.at(pointer);
            if (node.isArray()) {
                return node;
            }
        }
        return null;
    }

    private JsonNode findMappingArray(JsonNode root) {
        if (root == null || root.isNull()) {
            return null;
        }
        if (root.isArray() && looksLikeMappingArray(root)) {
            return root;
        }
        if (root.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                JsonNode value = field.getValue();
                String name = field.getKey();
                if (value.isArray() && isMappingArrayName(name) && looksLikeMappingArray(value)) {
                    return value;
                }
                JsonNode nested = findMappingArray(value);
                if (nested != null) {
                    return nested;
                }
            }
        } else if (root.isArray()) {
            for (JsonNode item : root) {
                JsonNode nested = findMappingArray(item);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private boolean isMappingArrayName(String name) {
        String normalized = name == null ? "" : name.replace("_", "").replace("-", "").toLowerCase();
        return normalized.contains("mapping") || normalized.contains("field");
    }

    private boolean looksLikeMappingArray(JsonNode node) {
        if (!node.isArray() || node.isEmpty()) {
            return false;
        }
        JsonNode first = node.get(0);
        return first != null
                && first.isObject()
                && (hasAny(first, "placeholder_key", "placeholderKey", "field_key", "fieldKey", "key", "name")
                || hasAny(first, "original_text", "originalText", "source_text", "sourceText", "value", "text"));
    }

    private boolean hasAny(JsonNode item, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (item.has(fieldName)) {
                return true;
            }
        }
        return false;
    }

    private String readText(JsonNode item, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode node = item.get(fieldName);
            if (node != null && !node.isNull()) {
                return node.asText("");
            }
        }
        return "";
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    public record ParsedMappings(
            List<TemplateFieldMappingVO> mappings,
            String rawText
    ) {
    }
}
