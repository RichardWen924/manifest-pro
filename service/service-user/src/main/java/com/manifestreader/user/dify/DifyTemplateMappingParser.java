package com.manifestreader.user.dify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.exception.ErrorCode;
import com.manifestreader.user.model.vo.TemplateFieldMappingVO;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
                "/data/outputs/text",
                "/data/outputs/answer",
                "/outputs/text",
                "/result/text");
        JsonNode mappingRoot = StringUtils.hasText(text)
                ? readJson(stripMarkdownJsonFence(text), "解析 Dify text JSON 失败")
                : responseRoot;
        JsonNode mappings = readFirstArray(mappingRoot,
                "/mappings",
                "/data/mappings",
                "/result/mappings",
                "/outputs/mappings");

        List<TemplateFieldMappingVO> result = new ArrayList<>();
        if (mappings != null) {
            mappings.forEach(item -> result.add(new TemplateFieldMappingVO(
                    readText(item, "original_text", "originalText"),
                    readText(item, "placeholder_key", "placeholderKey"),
                    defaultText(readText(item, "data_type", "dataType"), "string"),
                    readText(item, "description")
            )));
        }
        return new ParsedMappings(result, text);
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

    private String stripMarkdownJsonFence(String value) {
        String text = value.trim();
        if (text.startsWith("```")) {
            int firstLineEnd = text.indexOf('\n');
            int lastFenceStart = text.lastIndexOf("```");
            if (firstLineEnd >= 0 && lastFenceStart > firstLineEnd) {
                return text.substring(firstLineEnd + 1, lastFenceStart).trim();
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
