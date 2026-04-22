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
    private String exportWorkflowRunPath = "/v1/workflows/run";
    private String exportFileInputName = "file";
    private String exportResponseMode = "blocking";
    private String exportUser = "manifest-reader-export";
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

    public String getExportWorkflowRunPath() {
        return exportWorkflowRunPath;
    }

    public void setExportWorkflowRunPath(String exportWorkflowRunPath) {
        this.exportWorkflowRunPath = exportWorkflowRunPath;
    }

    public String getExportFileInputName() {
        return exportFileInputName;
    }

    public void setExportFileInputName(String exportFileInputName) {
        this.exportFileInputName = exportFileInputName;
    }

    public String getExportResponseMode() {
        return exportResponseMode;
    }

    public void setExportResponseMode(String exportResponseMode) {
        this.exportResponseMode = exportResponseMode;
    }

    public String getExportUser() {
        return exportUser;
    }

    public void setExportUser(String exportUser) {
        this.exportUser = exportUser;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
