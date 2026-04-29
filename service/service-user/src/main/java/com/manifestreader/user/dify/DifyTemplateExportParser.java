package com.manifestreader.user.dify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.exception.ErrorCode;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DifyTemplateExportParser {

    private final ObjectMapper objectMapper;

    public DifyTemplateExportParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParsedExportFields parse(String response) {
        if (!StringUtils.hasText(response)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "Dify 导出工作流响应为空");
        }
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode candidate = findFieldObject(root);
            if (candidate == null || !candidate.isObject()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "Dify 导出响应中未找到字段 JSON");
            }
            Map<String, Object> fields = new LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> iterator = candidate.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                fields.put(entry.getKey(), toPlainValue(entry.getValue()));
            }
            return new ParsedExportFields(fields, toReadableRaw(candidate));
        } catch (JsonProcessingException ex) {
            String stripped = stripMarkdownFence(response);
            if (!stripped.equals(response)) {
                return parse(stripped);
            }
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "Dify 导出响应不是合法 JSON");
        }
    }

    private JsonNode findFieldObject(JsonNode root) throws JsonProcessingException {
        if (root == null || root.isMissingNode() || root.isNull()) {
            return null;
        }
        String text = readFirstText(root, "/text", "/data/outputs/text", "/outputs/text", "/answer");
        if (StringUtils.hasText(text)) {
            JsonNode parsedText = objectMapper.readTree(stripMarkdownFence(text));
            if (looksLikeFieldObject(parsedText)) {
                return parsedText;
            }
            JsonNode nested = findFieldObject(parsedText);
            if (nested != null) {
                return nested;
            }
        }
        JsonNode outputs = root.at("/data/outputs");
        if (outputs.isMissingNode()) {
            outputs = root.at("/outputs");
        }
        if (outputs.isObject()) {
            if (looksLikeFieldObject(outputs)) {
                return outputs;
            }
            Iterator<Map.Entry<String, JsonNode>> iterator = outputs.fields();
            while (iterator.hasNext()) {
                JsonNode value = iterator.next().getValue();
                if (value.isTextual() && StringUtils.hasText(value.asText())) {
                    JsonNode nested = objectMapper.readTree(stripMarkdownFence(value.asText()));
                    if (looksLikeFieldObject(nested)) {
                        return nested;
                    }
                }
                if (looksLikeFieldObject(value)) {
                    return value;
                }
            }
        }
        if (looksLikeFieldObject(root)) {
            return root;
        }
        return null;
    }

    private boolean looksLikeFieldObject(JsonNode node) {
        if (node == null || !node.isObject() || node.has("mappings") || node.has("fields")) {
            return false;
        }
        if (node.has("task_id") || node.has("workflow_run_id") || node.has("data") || node.has("outputs")) {
            return false;
        }
        int scalarCount = 0;
        boolean hasBillField = false;
        Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            if (entry.getKey().contains("_") && !entry.getValue().isObject() && !entry.getValue().isArray()) {
                scalarCount++;
            }
            if (isBillFieldKey(entry.getKey())) {
                hasBillField = true;
            }
        }
        return hasBillField && scalarCount > 0;
    }

    private boolean isBillFieldKey(String key) {
        return switch (key) {
            case "bl_no", "booking_no", "doc_no", "serial_no", "shipper", "consignee", "notify_party",
                    "delivery_agent", "carrier_agent", "vessel_voyage", "port_of_loading",
                    "port_of_discharge", "place_of_delivery", "container_no", "seal_no",
                    "goods_description", "package_quantity", "gross_weight_kgs", "measurement_cbm",
                    "freight_term", "payable_at" -> true;
            default -> false;
        };
    }

    private String readFirstText(JsonNode root, String... pointers) {
        for (String pointer : pointers) {
            JsonNode node = root.at(pointer);
            if (!node.isMissingNode() && node.isTextual() && StringUtils.hasText(node.asText())) {
                return node.asText();
            }
        }
        return "";
    }

    private Object toPlainValue(JsonNode value) throws JsonProcessingException {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.numberValue();
        }
        if (value.isBoolean()) {
            return value.booleanValue();
        }
        if (value.isTextual()) {
            return value.asText();
        }
        return objectMapper.treeToValue(value, Object.class);
    }

    private String toReadableRaw(JsonNode node) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (IOException ex) {
            return node.toString();
        }
    }

    private String stripMarkdownFence(String text) {
        String value = text == null ? "" : text.trim();
        if (!value.startsWith("```")) {
            return value;
        }
        value = value.replaceFirst("^```(?:json)?\\s*", "");
        value = value.replaceFirst("\\s*```$", "");
        return value.trim();
    }

    public record ParsedExportFields(
            Map<String, Object> fields,
            String rawText
    ) {
    }
}
