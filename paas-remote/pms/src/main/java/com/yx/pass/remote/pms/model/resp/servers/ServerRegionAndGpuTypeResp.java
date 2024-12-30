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
public class ServerRegionAndGpuTypeResp implements Serializable {

    /**
     * GPU数量
     */
    private String gpuType;

    /**
     * GPU制造商
     */
    private String gpuManufacturer;

    private String regionCode;

    private String regionName;

    private static final long serialVersionUID = 1L;
}