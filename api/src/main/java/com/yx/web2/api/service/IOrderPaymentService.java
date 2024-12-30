package com.yx.web2.api.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.entity.OrderEntity;
import com.yx.web2.api.entity.OrderPaymentEntity;

import java.util.List;
import java.util.Map;

public interface IOrderPaymentService extends IService<OrderPaymentEntity> {
    /**
     * price already published, do order instalment plan
     *
     * @param tenantId     租户Id
     * @param accountModel 当前登录账户信息
     * @param orderEntity  订单信息
     * @param output       分期计算结果
     * @return List<OrderPaymentEntity> 分期数据
     */
    List<OrderPaymentEntity> prepareOrderInstalmentPlan(Long tenantId, AccountModel accountModel, OrderEntity orderEntity, InstallmentCalculateService.InstallmentOutput output);

    /**
     * 获取首付款订单
     *
     * @param orderId 订单号
     * @return 首付款订单信息
     */
    List<OrderPaymentEntity> getPrePaymentOrder(String orderId);

    /**
     * 获取付款订单
     *
     * @param orderPaymentId 付款订单号
     * @return 订单信息
     */
    OrderPaymentEntity getOrderPayment(String orderPaymentId);

    /**
     * 获取付款订单
     *
     * @param orderPaymentIds 付款订单号集合
     * @return 订单信息
     */
    List<OrderPaymentEntity> getOrderPayments(List<String> orderPaymentIds);

    /**
     * 开始分期计划
     *
     * @param orderEntity 订单信息
     */
    boolean startHirePurchaseSchedule(OrderEntity orderEntity, boolean isOrderVirtualPayment);

    /**
     * 获取分期计划最近一次的未支付订单
     *
     * @param orderId     订单Id
     * @param planPayDate 计划还款时间
     * @return 订单信息
     */
    OrderPaymentEntity getNotPaymentSubscriptionPayments(String orderId, String planPayDate);

    /**
     * 获取分期计划未支付订单
     *
     * @param orderId 订单Id
     * @return 订单信息
     */
    List<OrderPaymentEntity> getNotPaymentSubscriptionPayments(String orderId);

    /**
     * 账单列表
     *
     * @param page         分页信息
     * @param queryWrapper 查询添加
     */
    Page<JSONObject> billList(IPage<JSONObject> page, QueryWrapper<OrderPaymentEntity> queryWrapper);

    /**
     * 查询订单购买数量
     *
     * @param region 计价区域
     * @param spec   规格
     */
    Map<String, Integer> getOrderPaymentBySpecRegion(List<String> region, List<String> spec);

    /**
     * 修改支付信息Period信息
     *
     * @param orderEntity 订单信息
     */
    void updatePaymentPeriod(OrderEntity orderEntity);

    /**
     *
     * @author liyechao
     * @date 2024/12/20 17:58
     * @param orderId
     */
    void updateOrderPaymentValidFlag(String orderId);
}
