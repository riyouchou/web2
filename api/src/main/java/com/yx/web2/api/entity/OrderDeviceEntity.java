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
@TableName("t_order_device")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDeviceEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderId;
    private String regionCode;
    private String regionName;
    private String spec;
    private String subSpec;
    private String resourcePool;
    private String specName;
    private String gpuInfo;
    private String cpuInfo;
    private String disk;
    private String mem;
    private String unitPrice;
    private String discountPrice;
    private Integer quantity;
    private String deployRegionCode;
    private String deviceInfo;
    private Timestamp createTime;
}
