package com.yx.pass.remote.pms.model.resp.account;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description : 账号详情展示类定义
 * @Author : Lee666
 * @Date : 2023/7/10
 * @Version : 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TenantDetailResp implements Serializable {
    private static final long serialVersionUID = -7773210243258672681L;
    private Long id;

    private Long tid;

    private String firstName;

    private String roleName;

    private String email;

    private String createAt;


    private String lastName;


    private String createBy;

    private Integer orderNum;
}
