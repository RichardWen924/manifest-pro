package com.manifestreader.model.dto.template;

import jakarta.validation.constraints.NotBlank;

public class TemplateSaveDTO {

    @NotBlank(message = "templateCode cannot be blank")
    private String templateCode;

    @NotBlank(message = "templateName cannot be blank")
    private String templateName;

    private String templateType;
    private String remark;

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
