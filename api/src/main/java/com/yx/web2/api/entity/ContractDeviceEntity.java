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
@TableName("t_contract_device")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContractDeviceEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String contractId;
    private String regionCode;
    private String regionName;
    private String deployRegionCode;
    private String gpuInfo;
    private String cpuInfo;
    private String spec;
    private String mem;
    private String disk;
    private String bandWidth;
    private String unitPrice;
    private String discountPrice;
    private Integer quantity;
    private String deviceInfo;
    private Timestamp createTime;
    private Timestamp lastUpdateTime;
}
