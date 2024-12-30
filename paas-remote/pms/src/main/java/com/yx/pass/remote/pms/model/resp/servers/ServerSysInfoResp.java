package com.yx.pass.remote.pms.model.resp.servers;

import com.yx.pass.remote.pms.model.resp.servers.sshkey.ServerSshKeyResp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServerSysInfoResp implements Serializable {
    /**
     * 容器id
     */
    private Long cid;

    /**
     * 容器指纹id
     */
    private String fingerprint;

    /**
     * 带宽级别
     */
    private String bandwidthLevel;

    /**
     * 上传带宽
     */
    private Double downloadSpeed;

    /**
     * 下载带宽
     */
    private Double uploadSpeed;

    /**
     * cputype
     */
    private String cpuType;

    /**
     * cpu 线程数
     */
    private Integer cpuCount;

    /**
     * GPU数量
     */
    private String gpuType;

    /**
     * GPU型号
     */
    private Integer gpuCount;

    /**
     * 内存大小 单位mb
     */
    private String osMem;

    /**
     * 网卡速率
     */
    private String nic;

    /**
     * disk
     */
    private String disk;

    /**
     * 容器出口ip
     */
    private String publicIp;

    /**
     * ars&arsDog版本
     */
    private String verDetail;

    /**
     * 区域分组
     */
    private String groupId;

    /**
     * 网卡单位
     */
    private String nicUnit;

    /**
     * 带宽单位
     */
    private String bandwidthUnit;

    /**
     * CPU内存单位
     */
    private String storageUnit;

    /**
     * 磁盘信息
     */
    private String storage;

    /**
     * CPU
     */
    private String cpuRam;


    /**
     * CPU内存单位
     */
    private String cpuRamUnit;

    /**
     * CPU主频单位
     */
    private String cpuSpeedUnit;

    /**
     * 主频
     */
    private Double cpuSpeed;

    /**
     * 处理器数量
     */
    private Integer cpuCores;

    /**
     * CPU架构 X86 ARM
     */
    private String cpuArchitecture;

    /**
     * CPU制造商
     */
    private String cpuManufacturer;

    /**
     * CPU物理棵数
     */
    private Integer cpuNum;

    /**
     * GPU显存单位
     */
    private String gpuMemUnit;

    /**
     * GPU显存
     */
    private String gpuMem;

    /**
     * GPU接口类型：SXM / PCIe / NVL
     */
    private String gpuBusType;

    /**
     * GPU制造商
     */
    private String gpuManufacturer;

    private String regionCode;

    private String regionName;

    private Integer sshKeyTotal;

    private String userName;

    private String ip;

    private Integer port;

    private String orderCode;

    private Integer status;

    private java.sql.Timestamp effectTime;

    private java.sql.Timestamp endTime;

    private List<ServerSshKeyResp> serverSshKeyList;

    private String gpuMemOriginal;

    private String storageOriginal;

    private String nicOriginal;

    private static final long serialVersionUID = 1L;
}