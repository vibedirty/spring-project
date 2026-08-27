package com.cat.hard.stock.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class StockAdjustmentRequest {

    @NotNull(message = "库存变动量不能为空")
    private Integer changeQuantity;

    @NotBlank(message = "库存调整原因不能为空")
    @Size(max = 255, message = "库存调整原因长度不能超过255个字符")
    private String reason;

    @AssertTrue(message = "库存变动量不能为0")
    public boolean isChangeQuantityNonZero() {
        return changeQuantity == null || changeQuantity != 0;
    }

    public Integer getChangeQuantity() {
        return changeQuantity;
    }

    public void setChangeQuantity(Integer changeQuantity) {
        this.changeQuantity = changeQuantity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
