package com.yx.pass.remote.feecenter.model.resp;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsdRateResp {
    /**
     * 数据标识
     */
    private Integer id;
    /**
     * 汇率统计周期内的平均价格
     */
    private String price;
    /**
     * 汇率统计开始时间
     */
    private String createAt;
    /**
     * 汇率统计截至时间
     */
    private String updateAt;
}
