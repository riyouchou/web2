package com.yx.pass.remote.pms.model.resp.region;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Data
public class RegionInfoResp implements Serializable {

    private String resourcePool;

    private Integer id;

    private String regionCode;

    private String regionName;

    private String note;

    private String gatewayUrl;

    private String matchRegular;

    private String parentRegionCode;
    private String ossCode;

    private Date createAt;

    private String stunCredential;

    private String stunUserName;

    private String stunUrl;

    private String pingIpv4;

    private String pingIpv6;

    private String httpPingUrl;

    private String testSpeedUrl;

    private String nginxSignalUrl;

    private Integer isPriceRegion;

    private String logSvcUrl;

    private String idcGps;

    private String logStashUrl;

    private String idxUrl;

    private Boolean isTest;

    private Boolean isIdc;

    private Integer hasServer;

    /**
     * 时区
     */
    private String timeZone;

    /**
     * 时区偏移
     */
    private String timezoneOffset;

    /**
     * 语言
     */
    private String language;

    /**
     * arsDog重启间隔
     */
    private Integer arsdogRestartInterval;

    /**
     * arsDog-checker发送心跳间隔
     */
    private Integer arsdogCheckerHeartbeatInterval;

    /**
     * arsDog发送心跳间隔
     */
    private Integer arsdogHeartbeatInterval;

    /**
     *
     * PMS可用容器数量【Health = 1、已质押、未租用】
     * @date 2024/10/21 16:24
     */
    private Integer availableContainers;
    /**
     *
     * PMS可用容器数量【已质押、未租用】
     * @date 2024/10/21 16:24
     */
    private Integer availableContainer;

    private Set<String> specs;
}
