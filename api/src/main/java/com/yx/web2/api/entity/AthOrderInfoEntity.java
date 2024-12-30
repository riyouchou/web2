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
@TableName("ath_order_info")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AthOrderInfoEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderId;
    private String paymentOrderId;
    private String athOrderId;
    private String athTotal;
    private String dailyAth;
    private Long tenantId;
    private Timestamp createTime;
}
