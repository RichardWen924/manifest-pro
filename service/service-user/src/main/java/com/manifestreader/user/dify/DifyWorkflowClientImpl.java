package com.manifestreader.user.dify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.exception.ErrorCode;
import com.manifestreader.user.properties.DifyWorkflowProperties;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

@Component
public class DifyWorkflowClientImpl implements DifyWorkflowClient {

    private static final Logger log = LoggerFactory.getLogger(DifyWorkflowClientImpl.class);

    private final DifyWorkflowProperties properties;
    private final ObjectMapper objectMapper;

    public DifyWorkflowClientImpl(DifyWorkflowProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String runTemplateExtraction(MultipartFile file) {
        validateConfig();
        log.info("Dify template extraction start, baseUrl={}, uploadPath={}, workflowPath={}, fileName={}",
                normalizeBaseUrl(properties.getBaseUrl()),
                resolveEndpoint(properties.getFileUploadPath()),
                resolveEndpoint(properties.getWorkflowRunPath()),
                file.getOriginalFilename());
        String uploadFileId = uploadFile(file);
        return runWorkflow(uploadFileId);
    }

    private String uploadFile(MultipartFile file) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("user", properties.getUser());
            body.add("file", new MultipartInputFile(file.getBytes(), file.getOriginalFilename()));

            String response = restClient()
                    .post()
                    .uri(resolveEndpoint(properties.getFileUploadPath()))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            String uploadId = readFirstText(response, "/id", "/data/id", "/file_id");
            if (!StringUtils.hasText(uploadId)) {
                throw new BusinessException("Dify 文件上传响应缺少 id");
            }
            return uploadId;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "读取上传文件失败");
        } catch (RestClientResponseException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), buildDifyError("文件上传", ex));
        }
    }

    private String runWorkflow(String uploadFileId) {
        Map<String, Object> requestBody = Map.of(
                "inputs", Map.of(
                        properties.getTemplateFileInputName(), Map.of(
                                "type", "document",
                                "transfer_method", "local_file",
                                "upload_file_id", uploadFileId
                        )
                ),
                "response_mode", properties.getResponseMode(),
                "user", properties.getUser()
        );

        try {
            String response = restClient()
                    .post()
                    .uri(resolveEndpoint(properties.getWorkflowRunPath()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
            log.info("Dify template extraction workflow completed, uploadFileId={}", uploadFileId);
            return response;
        } catch (RestClientResponseException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), buildDifyError("工作流执行", ex));
        }
    }

    private RestClient restClient() {
        return RestClient.builder()
                .baseUrl(normalizeBaseUrl(properties.getBaseUrl()))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .build();
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String resolveEndpoint(String path) {
        String endpoint = StringUtils.hasText(path) ? path.trim() : "";
        if (!endpoint.startsWith("/")) {
            endpoint = "/" + endpoint;
        }
        String baseUrl = normalizeBaseUrl(properties.getBaseUrl());
        if (baseUrl.endsWith("/v1") && endpoint.startsWith("/v1/")) {
            return endpoint.substring(3);
        }
        return endpoint;
    }

    private String buildDifyError(String stage, RestClientResponseException ex) {
        String responseBody = sanitizeResponseBody(ex.getResponseBodyAsString());
        if (ex.getStatusCode().value() == 404) {
            return "Dify " + stage + "接口 404，请检查 DIFY_BASE_URL 是否为 API 地址，以及路径配置是否正确；当前路径 fileUploadPath="
                    + properties.getFileUploadPath() + ", workflowRunPath=" + properties.getWorkflowRunPath();
        }
        if (StringUtils.hasText(responseBody)) {
            return "Dify " + stage + "失败：" + ex.getStatusCode().value() + " " + responseBody;
        }
        return "Dify " + stage + "失败：" + ex.getStatusCode().value();
    }

    private String sanitizeResponseBody(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return "";
        }
        return responseBody
                .replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void validateConfig() {
        if (!properties.isEnabled()) {
            throw new BusinessException("Dify 工作流未启用，请先配置 manifest.dify.enabled=true");
        }
        if (!StringUtils.hasText(properties.getBaseUrl()) || !StringUtils.hasText(properties.getApiKey())) {
            throw new BusinessException("Dify baseUrl/apiKey 未配置");
        }
    }

    private String readFirstText(String json, String... pointers) {
        try {
            JsonNode root = objectMapper.readTree(json);
            for (String pointer : pointers) {
                JsonNode node = root.at(pointer);
                if (!node.isMissingNode() && StringUtils.hasText(node.asText())) {
                    return node.asText();
                }
            }
            return "";
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "解析 Dify 文件上传响应失败");
        }
    }

    private static class MultipartInputFile extends ByteArrayResource {

        private final String filename;

        MultipartInputFile(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = StringUtils.hasText(filename) ? filename : "template-file";
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
