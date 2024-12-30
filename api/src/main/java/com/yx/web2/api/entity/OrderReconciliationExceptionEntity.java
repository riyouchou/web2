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
@TableName("t_order_reconciliation_exception")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderReconciliationExceptionEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String paymentOrderId;
    private String amount;
    private String paymentRequired;
    private String toAddress;
    private Timestamp createTime;
    private Timestamp lastUpdateTime;

}