package com.yx.web2.api.mapper;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.web2.api.common.resp.order.OrderDeviceResp;
import com.yx.web2.api.entity.OrderPaymentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderPaymentMapper extends BaseMapper<OrderPaymentEntity> {
    @Select("SELECT b.id, b.order_id, b.payment_order_id, b.instalment_month, b.instalment_month_total, " +
            "b.hp_price, b.payment_status, b.pay_link, b.pay_id, b.pay_link_expire_at, b.plan_pay_date, b.pay_finish_time,a.order_resource_pool, " +
            "b.pre_payment, b.failure_reason, b.account_id, b.tenant_id, b.create_time, b.last_update_time " +
            "FROM t_order a INNER JOIN t_order_payment b ON a.order_id = b.order_id ${ew.customSqlSegment}")
    Page<JSONObject> billList(IPage<JSONObject> page, @Param(Constants.WRAPPER) Wrapper queryWrapper);

    @Select("<script>" +
            "SELECT " +
            "tOrder.order_status AS orderStatus, " +
            "device.order_id AS orderId, " +
            "device.spec, " +
            "device.region_code AS regionCode, " +
            "device.quantity " +
            "FROM t_order AS tOrder " +
            "LEFT JOIN t_order_device AS device ON tOrder.order_id = device.order_id " +
            "WHERE tOrder.order_status &lt; 5 AND tOrder.deleted = 0 and device.resource_pool = 'ARS' " +
            "<if test='specs != null and specs.size > 0'>" +
            "AND device.spec IN " +
            "<foreach item='spec' collection='specs' open='(' separator=',' close=')'>" +
            "#{spec}" +
            "</foreach> " +
            "</if>" +
            "<if test='regions != null and !regions.isEmpty()'>" +
            "AND device.region_code IN " +
            "<foreach item='region' collection='regions' open='(' separator=',' close=')'>" +
            "#{region}" +
            "</foreach>" +
            "</if>" +
            "</script>")
    List<OrderDeviceResp> getOrderPaymentBySpecRegion(@Param("regions") List<String> regions, @Param("specs") List<String> specs);


}
