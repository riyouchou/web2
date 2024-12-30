package com.yx.web2.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.sql.Timestamp;

@Data
@TableName("t_customer_payment_method")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerPaymentMethodEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long accountId;
    private Long tenantId;
    private String customerId;
    private String paymentMethodId;
    private String paymentMethodInfo;
    private Timestamp lastUpdateTime;
}
