package com.yx.web2.api.mapper;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yx.web2.api.entity.OrderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {

    @Select("SELECT " +
            "GROUP_CONCAT(tOrder.order_id) AS orderId," +
            "GROUP_CONCAT(tOrder.ath_order_id) AS athOrderId," +
            "tOrder.tenant_id," +
            "tOrder.order_status as orderStatus," +
            "tOrder.order_status as orderStatus," +
            "device.spec_name as specName, " +
            "device.region_name as regionName, " +
            "device.spec as spec, " +
            "GROUP_CONCAT(device.region_code) AS regionCode," +
            "SUM(device.quantity) as quantity " +
            "FROM t_order AS tOrder " +
            "LEFT JOIN t_order_device AS device ON tOrder.order_id = device.order_id " +
            "LEFT JOIN ath_order_info AS ath ON tOrder.order_id = ath.order_id " +
            "WHERE tOrder.order_status = 4 " +
            "AND tOrder.tenant_id = #{tid} " +
            "AND device.spec = #{spec} " +
            "GROUP BY device.spec, device.region_code,device.spec_name,device.region_name")
    List<JSONObject> containerOrderDeploySpec(@Param("tid") Long tid,@Param("spec") String spec);

}
