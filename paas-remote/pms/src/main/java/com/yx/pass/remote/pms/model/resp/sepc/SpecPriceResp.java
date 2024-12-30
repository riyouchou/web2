package com.yx.pass.remote.pms.model.resp.sepc;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @Description : 账号修改密码数据传输类定义
 * @Author : Lee666
 * @Date : 2023/8/14
 * @Version : 1.0
 */
@Data
public class SpecPriceResp implements Serializable {
    private static final long serialVersionUID = 3531087179527450721L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String regionCode;

    private String spec;

    private String subSpec;

    private String subSpecName;

    private Date createAt;

    private Date updateAt;

    private BigDecimal retailPrice;

    private BigDecimal wholesalePrice;

    private String resourcePool;

    private List<ConfigPriceDiscount> discounts;

    private String specName;

    private String regionName;

    private String gpu;

    private String cpu;

    private String disk;

    private String mem;

    private Long available;

    private List<String> adaptableGames;

}
@Data
class ConfigPriceDiscount implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 所属区域Code
     */
    private String regionCode;

    /**
     * 规格
     */
    private String spec;

    /**
     * 租赁方式名称
     */
    private String name;

    /**
     * 租赁时长（单位：H）
     */
    private Integer hours;

    /**
     * 折扣系数
     */
    private BigDecimal discount;

    private Date createAt;

    private Date updateAt;
}
