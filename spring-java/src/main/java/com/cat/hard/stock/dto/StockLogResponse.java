package com.cat.hard.stock.dto;


import com.cat.hard.stock.entity.StockLog;

import java.time.LocalDateTime;

public class StockLogResponse {

    private Long id;
    private Long productId;

    private Integer changeQuantity;

    private Integer beforeStock;

    private Integer afterStock;

    private String reason;

    private String businessNo;

    private LocalDateTime createdAt;
    private String productName;

    public StockLogResponse(Long id, Long productId, Integer changeQuantity, Integer beforeStock, Integer afterStock, String reason, String businessNo, LocalDateTime createdAt, String productName) {
        this.id = id;
        this.productId = productId;
        this.changeQuantity = changeQuantity;
        this.beforeStock = beforeStock;
        this.afterStock = afterStock;
        this.reason = reason;
        this.businessNo = businessNo;
        this.createdAt = createdAt;
        this.productName = productName;
    }

    public static StockLogResponse from(StockLog log) {
        return new StockLogResponse(log.getId(), log.getProductId(), log.getChangeQuantity(), log.getBeforeStock(), log.getAfterStock(), log.getReason(), log.getBusinessNo(), log.getCreatedAt(), log.getProductName());
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getChangeQuantity() {
        return changeQuantity;
    }

    public void setChangeQuantity(Integer changeQuantity) {
        this.changeQuantity = changeQuantity;
    }

    public Integer getBeforeStock() {
        return beforeStock;
    }

    public void setBeforeStock(Integer beforeStock) {
        this.beforeStock = beforeStock;
    }

    public Integer getAfterStock() {
        return afterStock;
    }

    public void setAfterStock(Integer afterStock) {
        this.afterStock = afterStock;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getBusinessNo() {
        return businessNo;
    }

    public void setBusinessNo(String businessNo) {
        this.businessNo = businessNo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
