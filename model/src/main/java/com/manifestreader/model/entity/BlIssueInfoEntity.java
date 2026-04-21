package com.manifestreader.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("bl_issue_info")
public class BlIssueInfoEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long blDocumentId;
    private String serviceType;
    private String serviceMode;
    private String revenueTons;
    private String issuePlace;
    private String ladenOnBoard;
    private String originalBlCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBlDocumentId() {
        return blDocumentId;
    }

    public void setBlDocumentId(Long blDocumentId) {
        this.blDocumentId = blDocumentId;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getServiceMode() {
        return serviceMode;
    }

    public void setServiceMode(String serviceMode) {
        this.serviceMode = serviceMode;
    }

    public String getRevenueTons() {
        return revenueTons;
    }

    public void setRevenueTons(String revenueTons) {
        this.revenueTons = revenueTons;
    }

    public String getIssuePlace() {
        return issuePlace;
    }

    public void setIssuePlace(String issuePlace) {
        this.issuePlace = issuePlace;
    }

    public String getLadenOnBoard() {
        return ladenOnBoard;
    }

    public void setLadenOnBoard(String ladenOnBoard) {
        this.ladenOnBoard = ladenOnBoard;
    }

    public String getOriginalBlCount() {
        return originalBlCount;
    }

    public void setOriginalBlCount(String originalBlCount) {
        this.originalBlCount = originalBlCount;
    }
}
