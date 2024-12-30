package com.yx.web2.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.common.req.order.*;
import com.yx.web2.api.common.resp.order.DueDateOrderListResp;
import com.yx.web2.api.common.resp.order.OrderDetailResp;
import com.yx.web2.api.common.resp.order.OrderListResp;
import com.yx.web2.api.common.resp.order.OrderPayResp;
import com.yx.web2.api.common.resp.PageResp;
import com.yx.web2.api.entity.ContractDeviceEntity;
import com.yx.web2.api.entity.ContractEntity;
import com.yx.web2.api.entity.ContractPaymentEntity;
import com.yx.web2.api.entity.OrderEntity;
import org.yx.lib.utils.util.R;

import java.util.List;
import java.util.Map;

public interface IOrderService extends IService<OrderEntity> {

    /**
     * 订单列表
     *
     * @param tenantId      租户Id
     * @param accountModel  当前登录账户
     * @param orderQueryReq 订单列表查询条件
     * @return R<PageResp < OrderListResp>>
     */
    R<PageResp<OrderListResp>> orderList(Long tenantId, AccountModel accountModel, OrderQueryReq orderQueryReq);

    /**
     * 订单详情
     *
     * @param tenantId     租户Id
     * @param accountModel 当前登录账户
     * @param orderId      订单详情
     * @return R<OrderDetailResp>
     */
    R<OrderDetailResp> orderDetail(Long tenantId, AccountModel accountModel, String orderId);

    /**
     * 创建订单
     *
     * @param tenantId       租户Id
     * @param accountModel   当前登录账户
     * @param createOrderReq 创建订单请求
     * @return R<String>
     */
    R<String> createOrder(Long tenantId, AccountModel accountModel, CreateOrderReq createOrderReq);

    /**
     * 订单支付
     *
     * @param tenantId     租户Id
     * @param accountModel 当前登录账户
     * @param orderIdReq   订单Id请求
     * @return R<OrderPayResp>
     */
    R<OrderPayResp> pay(Long tenantId, AccountModel accountModel, OrderPayReq orderIdReq);

    /**
     * 虚拟货币订单支付 请求链之前请求保存数据
     *
     * @param tenantId     租户Id
     * @param accountModel 当前登录账户
     * @param orderPayVirtualReq   订单虚拟货币支付请求
     * @return R<OrderPayResp>
     */
    R<?> payVirtualPre(Long tenantId, AccountModel accountModel, OrderPayVirtualReq orderPayVirtualReq);

    /**
     * 虚拟货币订单支付 请求链之后请求
     *
     * @param tenantId     租户Id
     * @param accountModel 当前登录账户
     * @param orderPayVirtualReq   订单虚拟货币支付请求
     * @return R<OrderPayResp>
     */
    R<?> payVirtual(Long tenantId, AccountModel accountModel, OrderPayVirtualReq orderPayVirtualReq);

    /**
     * 查询订单信息
     *
     * @param orderId 订单编号
     * @return OrderEntity
     */
    OrderEntity getOrder(String orderId);

    /**
     * 查询订单信息
     *
     * @param orderId   订单编号
     * @param accountId 账户Id
     * @return OrderEntity
     */
    OrderEntity getOrder(String orderId, Long accountId);

    /**
     * 查询订单信息
     *
     * @param orderId  订单编号
     * @param tenantId 租户Id
     * @return OrderEntity
     */
    OrderEntity getOrderByTenantId(String orderId, Long tenantId);

    /**
     * 查询订单信息
     *
     * @param orderId     订单编号
     * @param bdAccountId bd账户Id
     * @return OrderEntity
     */
    OrderEntity getOrderByBdAccountId(String orderId, Long bdAccountId);

    /**
     * 查询订单信息
     *
     * @param subscriptionId 订阅Id
     * @return OrderEntity
     */
    OrderEntity getOrderBySubscriptionId(String subscriptionId);

    /**
     * 确认订单价格
     *
     * @param tenantId             租户Id
     * @param accountModel         当前登录账户
     * @param orderConfirmPriceReq 确认订单价格请求
     * @return R
     */
    R<?> priceConfirm(Long tenantId, AccountModel accountModel, OrderConfirmPriceReq orderConfirmPriceReq);

    /**
     * 发布订单价格
     *
     * @param tenantId     租户Id
     * @param accountModel 当前登录账户
     * @param orderPublishPriceReq   订单
     * @return R
     */
    R<?> pricePublish(Long tenantId, AccountModel accountModel, OrderPublishPriceReq orderPublishPriceReq);

    /**
     * 订单确认支付
     *
     * @param tenantId     租户Id
     * @param accountModel 当前登录账户
     * @param orderIdReq   订单Id
     * @param sourceType   订单来源类型 1：web调用 2：定时任务job调用
     * @return R
     */
    R<?> confirmPaid(Long tenantId, AccountModel accountModel, OrderIdReq orderIdReq, Integer sourceType);

    /**
     * 删除订单
     *
     * @param tenantId     租户Id
     * @param accountModel 当前登录账户
     * @param orderIdReq   订单Id
     * @return R
     */
    R<?> delete(Long tenantId, AccountModel accountModel, OrderIdReq orderIdReq);

    /**
     * 终止订单
     *
     * @param tenantId     租户Id
     * @param accountModel 当前登录账户
     * @param orderIdReq   订单Id
     * @return R
     */
    R<?> terminate(Long tenantId, AccountModel accountModel, OrderIdReq orderIdReq);

    /**
     * 查询用户对应的订单数量
     *
     * @param userIds
     * @return
     */
    Map<Long, Integer> getAccountOrderNum(List<Long> userIds);

    /**
     * 查询用户已下单服务中的订单规格容器数量
     *
     * @author yijian
     * @date 2024/10/16 13:15
     */
    R<?> containerOrderDeploySpec(Long tid, String osType, String targetVersion, Long appId);

    /**
     * 从合同创建订单
     *
     * @param contractEntity          合同信息
     * @param contractDeviceEntities  合同资源信息
     * @param contractPaymentEntities 合同分期信息
     * @return 订单号
     */
    String createOrderFromContract(ContractEntity contractEntity, List<ContractDeviceEntity> contractDeviceEntities, List<ContractPaymentEntity> contractPaymentEntities);

    /**
     * 获取逾期未支付的订单列表
     *
     * @param tenantId     租户ID
     * @param accountModel 账户信息
     * @return R
     */
    R<List<DueDateOrderListResp>> dueOrderList(Long tenantId, AccountModel accountModel);

    /**
     * 逾期订单支付
     *
     * @param tenantId       租户ID
     * @param accountModel   账户信息
     * @param dueOrderPayReq 逾期支付订单号信息
     * @return R
     */
    R<OrderPayResp> duePayOrder(Long tenantId, AccountModel accountModel, List<DueOrderPayReq> dueOrderPayReq);

    /**
     * 获取租户逾期订单个数
     *
     * @param tenantId 租户ID
     * @return R
     */
    R<Long> dueOrderCount(Long tenantId);


    void savePaymentRecord(OrderPayVirtualReq orderPayVirtualReq, Boolean success);
}
