package com.yx.pass.remote.pms.model.resp.resource;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

/**
 * <p>
 * server info
 * </p>
 *
 * @author lpj
 * @since 2023-07-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class PmsServersOrderContainerResp implements Serializable {

    private static final long serialVersionUID = -1985453303371887222L;
    private Long id;

    private String regionCode;

    private String parentRegionCode;

    private String regionName;

    //    @ApiModelProperty(value = "服务器状态, wait_confirm: 待确认状态, wait_check: 待检测, online： 上线状态")
    @TableField(exist = false)
    private String status;

    private Date createAt;

    private Date updateAt;

    private Long deleteAt;

    private Boolean online;

    private String spec;

    private String specName;

    private String subSpec;

    private String subSpecName;

    private String orderCode;

    private String resourcePool;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String gpuType;

    private java.sql.Timestamp effectTime;

    private java.sql.Timestamp endTime;

    @JsonInclude()
    private String gpuManufacturer;
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String gpuCount;


}
