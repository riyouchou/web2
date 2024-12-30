package com.yx.web2.api.common.req.container.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class FeeCenterPickOrderContainerDTO implements Serializable {
    private static final long serialVersionUID = -7493437676526765441L;

    private Long wholesaleTid;
    private String thirdPartyOrderCode;
    private String orderCode;
    // 1.bind  2 unbind
    private Integer type;
    private Integer status;
    private List<Long> cids;

    @Override
    public String toString() {
        return "FeeCenterPrickOrderContainerDTO{" +
                "wholesaleTid='" + wholesaleTid + '\'' +
                ", orderCode='" + orderCode + '\'' +
                ", type=" + type +
                ", cids=" + cids +
                '}';
    }
}
