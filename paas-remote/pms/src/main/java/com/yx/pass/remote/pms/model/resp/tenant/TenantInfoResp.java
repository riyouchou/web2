package com.yx.pass.remote.pms.model.resp.tenant;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Data
public class TenantInfoResp implements Serializable {
    private Long id;
    private String code;
    private String name;
    // "租户状态，normal 和 pending"
    private String status;
    // "租户类型, IDC: IDC类租户, GP: GP类租户, GPD: GPD类租户"
    private String tenantType;
    private String email;
    private String address;
    // "授信租户标识: 0 -- 非授信租户, 1 -- 授信租户"
    private Integer credit;
    private Integer adminSessionStatus;
    private Long createBy;
    private Long createByTid;
    private List<TenantAksk> tenantAkskList;

    private Aksk _aksk;

    public Aksk getAkSk() {
        if (_aksk != null) {
            return _aksk;
        }
        if (tenantAkskList != null && !tenantAkskList.isEmpty()) {
            Optional<TenantAksk> tenantAkskOptional = tenantAkskList.stream().filter(item ->
                    item.getValid() != null &&
                            item.getValid() == 1 &&
                            item.getExpireTs() != null &&
                            item.getExpireTs().after(new Date())).findFirst();
            if (tenantAkskOptional.isPresent()) {
                Aksk aksk = new Aksk();
                aksk.setAk(tenantAkskOptional.get().getAk());
                aksk.setSk(tenantAkskOptional.get().getSk());
                this._aksk = aksk;
                return _aksk;
            }
        }
        return null;
    }

    @Data
    public static class TenantAksk {
        private String ak;
        private String sk;
        private Integer valid;
        private Date expireTs;
    }

    @Data
    public static class Aksk {
        private String ak;
        private String sk;
    }
}
