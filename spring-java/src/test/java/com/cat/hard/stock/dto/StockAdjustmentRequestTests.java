package com.cat.hard.stock.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class StockAdjustmentRequestTests {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void shouldAcceptPositiveAndNegativeAdjustments() {
        assertThat(validate(10, "采购入库")).isEmpty();
        assertThat(validate(-3, "盘点扣减")).isEmpty();
    }

    @Test
    void shouldRejectMissingQuantityAndBlankReason() {
        Set<ConstraintViolation<StockAdjustmentRequest>> violations = validate(null, " ");

        assertThat(messages(violations))
                .contains("库存变动量不能为空", "库存调整原因不能为空");
    }

    @Test
    void shouldRejectZeroAdjustment() {
        assertThat(messages(validate(0, "库存盘点")))
                .contains("库存变动量不能为0");
    }

    @Test
    void shouldRejectReasonLongerThanDatabaseColumn() {
        assertThat(messages(validate(1, "a".repeat(256))))
                .contains("库存调整原因长度不能超过255个字符");
    }

    private Set<ConstraintViolation<StockAdjustmentRequest>> validate(Integer changeQuantity, String reason) {
        StockAdjustmentRequest request = new StockAdjustmentRequest();
        request.setChangeQuantity(changeQuantity);
        request.setReason(reason);
        return validator.validate(request);
    }

    private Set<String> messages(Set<ConstraintViolation<StockAdjustmentRequest>> violations) {
        return violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());
    }
}
