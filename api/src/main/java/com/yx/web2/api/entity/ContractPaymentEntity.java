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
@TableName("t_contract_payment")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContractPaymentEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String contractId;
    private Integer instalmentMonthTotal;
    private Integer instalmentMonth;
    private BigDecimal hpPrice;
    private BigDecimal hpPrePaymentPrice;
    private Boolean prePayment;
    private Timestamp createTime;
}
