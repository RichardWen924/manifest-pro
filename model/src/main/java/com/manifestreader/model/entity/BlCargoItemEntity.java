package com.manifestreader.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;

@TableName("bl_cargo_item")
public class BlCargoItemEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long blDocumentId;
    private String containerNo;
    private String sealNo;
    private String containerType;
    private Integer packageQuantity;
    private String packageUnit;
    private String goodsDescription;
    private String marks;
    private BigDecimal containerWeight;
    private BigDecimal vgmWeight;
    private BigDecimal grossWeightKgs;
    private BigDecimal measurementCbm;
    private Integer sortNo;

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

    public String getContainerNo() {
        return containerNo;
    }

    public void setContainerNo(String containerNo) {
        this.containerNo = containerNo;
    }

    public String getSealNo() {
        return sealNo;
    }

    public void setSealNo(String sealNo) {
        this.sealNo = sealNo;
    }

    public String getContainerType() {
        return containerType;
    }

    public void setContainerType(String containerType) {
        this.containerType = containerType;
    }

    public Integer getPackageQuantity() {
        return packageQuantity;
    }

    public void setPackageQuantity(Integer packageQuantity) {
        this.packageQuantity = packageQuantity;
    }

    public String getPackageUnit() {
        return packageUnit;
    }

    public void setPackageUnit(String packageUnit) {
        this.packageUnit = packageUnit;
    }

    public String getGoodsDescription() {
        return goodsDescription;
    }

    public void setGoodsDescription(String goodsDescription) {
        this.goodsDescription = goodsDescription;
    }

    public String getMarks() {
        return marks;
    }

    public void setMarks(String marks) {
        this.marks = marks;
    }

    public BigDecimal getContainerWeight() {
        return containerWeight;
    }

    public void setContainerWeight(BigDecimal containerWeight) {
        this.containerWeight = containerWeight;
    }

    public BigDecimal getVgmWeight() {
        return vgmWeight;
    }

    public void setVgmWeight(BigDecimal vgmWeight) {
        this.vgmWeight = vgmWeight;
    }

    public BigDecimal getGrossWeightKgs() {
        return grossWeightKgs;
    }

    public void setGrossWeightKgs(BigDecimal grossWeightKgs) {
        this.grossWeightKgs = grossWeightKgs;
    }

    public BigDecimal getMeasurementCbm() {
        return measurementCbm;
    }

    public void setMeasurementCbm(BigDecimal measurementCbm) {
        this.measurementCbm = measurementCbm;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }
}
