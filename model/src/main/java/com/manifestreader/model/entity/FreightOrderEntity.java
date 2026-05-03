package com.manifestreader.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("freight_order")
public class FreightOrderEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long demandId;
    private Long acceptedQuoteId;
    private Long publisherCompanyId;
    private Long publisherUserId;
    private Long agentCompanyId;
    private Long agentUserId;
    private String orderStatus;
    private Integer deleted;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getDemandId() { return demandId; }
    public void setDemandId(Long demandId) { this.demandId = demandId; }
    public Long getAcceptedQuoteId() { return acceptedQuoteId; }
    public void setAcceptedQuoteId(Long acceptedQuoteId) { this.acceptedQuoteId = acceptedQuoteId; }
    public Long getPublisherCompanyId() { return publisherCompanyId; }
    public void setPublisherCompanyId(Long publisherCompanyId) { this.publisherCompanyId = publisherCompanyId; }
    public Long getPublisherUserId() { return publisherUserId; }
    public void setPublisherUserId(Long publisherUserId) { this.publisherUserId = publisherUserId; }
    public Long getAgentCompanyId() { return agentCompanyId; }
    public void setAgentCompanyId(Long agentCompanyId) { this.agentCompanyId = agentCompanyId; }
    public Long getAgentUserId() { return agentUserId; }
    public void setAgentUserId(Long agentUserId) { this.agentUserId = agentUserId; }
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
