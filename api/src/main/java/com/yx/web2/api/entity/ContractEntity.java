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
@TableName("t_contract")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContractEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String contractId;
    private String envelopeId;
    private String documentId;
    private Long tenantId;
    private String tenantName;
    private String customerLegalEntityName;
    private String customerRegistrationNumber;
    private String customerLegalEntityAddress;
    private String customerCountry;
    private String signerName;
    private String signerEmail;
    private Timestamp signerSignTime;
    private Timestamp startedTime;
    private Integer freeServiceTermDays;
    private BigDecimal amount;
    private BigDecimal avgAmount;
    private BigDecimal prePaymentPrice;
    private Integer serviceDuration;
    private Integer serviceDurationPeriod;
    private Long bdAccountId;
    private String bdAccountName;
    private Long financeAccountId;
    private String financeAccountName;
    private String orderId;
    private Integer contractStatus;
    private String rejectMsg;
    private Timestamp createTime;
    private Timestamp lastUpdateTime;
    private Long lastUpdateAccountId;
    private boolean deleted;
}
