package com.yx.pass.remote.pms.model.req;

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
public class ServerSshKeyReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 唯一主键ID
     */
    private Long id;

    private Long sshKeyRelId;

    /**
     * 所属租户的id主键
     */
    private Long tid;

    private Long cid;

    /**
     * ssh name
     */
    private String name;

    /**
     * SSH Key
     */
    private String sshKey;

    private String ids;


}
