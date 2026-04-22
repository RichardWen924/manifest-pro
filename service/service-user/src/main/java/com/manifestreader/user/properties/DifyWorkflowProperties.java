package com.manifestreader.user.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "manifest.dify")
public class DifyWorkflowProperties {

    private String baseUrl = "";
    private String apiKey = "";
    private String fileUploadPath = "/v1/files/upload";
    private String workflowRunPath = "/v1/workflows/run";
    private String templateFileInputName = "file";
    private String responseMode = "blocking";
    private String user = "manifest-reader";
    private boolean enabled = false;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getFileUploadPath() {
        return fileUploadPath;
    }

    public void setFileUploadPath(String fileUploadPath) {
        this.fileUploadPath = fileUploadPath;
    }

    public String getWorkflowRunPath() {
        return workflowRunPath;
    }

    public void setWorkflowRunPath(String workflowRunPath) {
        this.workflowRunPath = workflowRunPath;
    }

    public String getTemplateFileInputName() {
        return templateFileInputName;
    }

    public void setTemplateFileInputName(String templateFileInputName) {
        this.templateFileInputName = templateFileInputName;
    }

    public String getResponseMode() {
        return responseMode;
    }

    public void setResponseMode(String responseMode) {
        this.responseMode = responseMode;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
