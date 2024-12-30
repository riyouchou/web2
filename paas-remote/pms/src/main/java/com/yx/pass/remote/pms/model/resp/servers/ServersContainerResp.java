package com.yx.pass.remote.pms.model.resp.servers;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;

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
public class ServersContainerResp extends ServerSysInfoResp implements Serializable {

    private static final long serialVersionUID = -1985453303371887222L;
    private String id;

    private String parentRegionCode;

    private String parentRegionName;

    private String regionCode;

    private String regionName;

    private String fingerprint;

    private Integer status;

    private String spec;

    private String specName;

    private String resourcePool;

    private BigDecimal price;

    private Integer available;

}
