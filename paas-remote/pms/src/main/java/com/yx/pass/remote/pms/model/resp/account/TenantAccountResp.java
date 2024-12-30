package com.yx.pass.remote.pms.model.resp.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
/**
 * <p>
 * 
 * </p>
 *
 * @author lpj
 * @since 2023-06-21
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TenantAccountResp implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String acct;

    private String name;

    private Long tid;

    private Integer roleId;

    private String email;

    private String mobile;

    private Date createAt;

    private Date updateAt;

    private String privyId;

    private Boolean isDelete;
}
