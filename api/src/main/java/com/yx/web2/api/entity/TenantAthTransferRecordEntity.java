package com.yx.web2.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@TableName("t_tenant_ath_transfer_record")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TenantAthTransferRecordEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderId;
    private String athOrderId;
    private String paymentOrderId;
    private String athBillId;
    private BigDecimal athAmount;
    private Long transToTenantId;
    private String transToTenantName;
    private Long transFromTenantId;
    private Integer transferStatus;
    private Integer transType;
    private String failureReason;
    private Timestamp createTime;
}
