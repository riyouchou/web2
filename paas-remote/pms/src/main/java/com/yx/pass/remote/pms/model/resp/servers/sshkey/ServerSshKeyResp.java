package com.yx.pass.remote.pms.model.resp.servers.sshkey;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
/**
 * <p>
 * SSH Keys
 * </p>
 *
 * @author lpj
 * @since 2024-08-29
 */
@Data
public class ServerSshKeyResp implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 唯一主键ID
     */
    private Long id;

    /**
     * 所属租户的id主键
     */
    private Long tid;

    /**
     * ssh name
     */
    private String name;

    /**
     * SSH Key
     */
    private String sshKey;

    /**
     * 创建者ID（用户ID）
     */
    private Long createBy;

    /**
     * 创建时间
     */
    @JsonFormat(timezone = "UTC")
    private Date createAt;

    /**
     * 更新者ID（用户ID）
     */
    private Long updateBy;

    /**
     * 更新时间
     */
    @JsonFormat(timezone = "UTC")
    private Date updateAt;

    private Long sshKeyRelId;

}
