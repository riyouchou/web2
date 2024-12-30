package com.yx.web2.api.common.req.container.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * packageName com.yx.web2.api.common.req.container.dto
 *
 * @author YI-JIAN-ZHANG
 * @version JDK 8
 * @className FeeOrderResourceDTO
 * @date 2024/9/12
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FeeOrderResourceDTO implements Serializable {
    private String orderCode;

    private String cid;

    private String resourceType;

    private String region;

    private String wholesale;

    private String effectTime;

    private String endTime;

}
