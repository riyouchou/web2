package com.yx.pass.remote.pms.model.resp.sepc;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * 容器规格能力等级定义
 * </p>
 *
 * @author lpj
 * @since 2023-10-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ConfigContainerSpecResp implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;

    private Date createAt;

    private Date updateAt;

    private String spec;
    private String specName;

    /**
     * GPU（冗余的mgr库的规格信息，在更新规格时同步更新）
     */
    private String gpu;

    /**
     * cpu型号描述
     */
    private String cpu;

    /**
     * 内存描述
     */
    private String mem;

    /**
     * 硬盘描述
     */
    private String disk;

    /**
     * NIC描述
     */
    private String nic;

    /**
     * Bandwidth/data 描述
     */
    private String bandwidth;

    private String resourcePool;

    private List<String> adaptableGames;
}
