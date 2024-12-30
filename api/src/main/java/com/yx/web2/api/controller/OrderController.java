package com.yx.web2.api.controller;


import cn.hutool.core.convert.Convert;
import com.yx.web2.api.common.constant.RequestAttributeConstants;
import com.yx.web2.api.common.enums.AccountType;
import com.yx.web2.api.common.enums.ServiceDurationPeriod;
import com.yx.web2.api.common.enums.SysCode;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.common.req.order.*;
import com.yx.web2.api.common.resp.PageResp;
import com.yx.web2.api.common.resp.order.DueDateOrderListResp;
import com.yx.web2.api.common.resp.order.OrderDetailResp;
import com.yx.web2.api.common.resp.order.OrderListResp;
import com.yx.web2.api.common.resp.order.OrderPayResp;
import com.yx.web2.api.service.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.yx.lib.utils.util.R;
import org.yx.lib.utils.util.StringUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static com.yx.web2.api.common.constant.Web2ApiConstants.SOURCE_TYPE_FROM_WEB;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final IOrderService orderService;

    /**
     * 查询订单列表
     *
     * @param tenantId      租户ID
     * @param accountModel  账户信息
     * @param orderQueryReq 查询条件
     * @return R
     */
    @PostMapping("/list")
    public R<PageResp<OrderListResp>> orderList(
            @RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId,
            @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel,
            @RequestBody OrderQueryReq orderQueryReq) {
        if (StringUtil.isBlank(orderQueryReq.getBizType())) {
            orderQueryReq.setBizType("ARS");
        } else {
            if (!orderQueryReq.getBizType().equalsIgnoreCase("ARS") && !orderQueryReq.getBizType().equalsIgnoreCase("BM")) {
                return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
            }
        }
        return orderService.orderList(tenantId, accountModel, orderQueryReq);
    }

    /**
     * 查询订单详情
     *
     * @param tenantId     租户ID
     * @param accountModel 账户信息
     * @param orderId      订单Id
     * @return R
     */
    @GetMapping("/detail")
    public R<OrderDetailResp> detail(
            @RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId,
            @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel,
            @RequestParam(value = "orderId") String orderId) {
        return orderService.orderDetail(tenantId, accountModel, orderId);
    }

    /**
     * 创建订单
     *
     * @param tenantId       租户ID
     * @param accountModel   账户信息
     * @param createOrderReq 创建订单信息
     * @return R
     */
    @PostMapping("/create")
    public R<String> createOrder(
            @RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId,
            @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel,
            @RequestBody CreateOrderReq createOrderReq) {
        if (createOrderReq.getServiceDuration() == null || createOrderReq.getServiceDuration() <= 0 ||
                createOrderReq.getServiceDurationPeriod() == null ||
                createOrderReq.getServiceDurationPeriod() < ServiceDurationPeriod.Day.getValue() ||
                createOrderReq.getServiceDurationPeriod() > ServiceDurationPeriod.Year.getValue()) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        BigDecimal defaultBigDecimal = new BigDecimal(0);
        if (StringUtil.isBlank(createOrderReq.getInitialPrice()) ||
                Convert.toBigDecimal(createOrderReq.getInitialPrice(), defaultBigDecimal).doubleValue() == defaultBigDecimal.doubleValue()) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        if (createOrderReq.getAutoRenew() == null) {
            createOrderReq.setAutoRenew(false);
        }
        if (createOrderReq.getDevices() == null || createOrderReq.getDevices().isEmpty()) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        if (createOrderReq.getDevices().stream().anyMatch(item ->
                StringUtil.isBlank(item.getRegionCode()) || StringUtil.isBlank(item.getSpec()) ||
                        item.getQuantity() == null || item.getQuantity() <= 0 ||
                        StringUtil.isBlank(item.getUnitPrice()) || StringUtil.isBlank(item.getResourcePool()))) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        if (createOrderReq.getDevices().stream().anyMatch(item ->
                StringUtil.isNotBlank(item.getResourcePool()) && item.getResourcePool().equalsIgnoreCase("BM")
                        && StringUtil.isBlank(item.getSubSpec())
        )) {
            return R.failed(SysCode.x00000400.getValue(), "Order bm container, subSpec can not empty");
        }
        return orderService.createOrder(tenantId, accountModel, createOrderReq);
    }

    @PostMapping("/pay")
    public R<OrderPayResp> payOrder(
            @RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId,
            @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel,
            @RequestBody OrderPayReq orderPayReq) {
        if (StringUtil.isBlank(orderPayReq.getOrderId())) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        return orderService.pay(tenantId, accountModel, orderPayReq);
    }

    @PostMapping("/payVirtualPre")
    public R<?> payVirtualPre(
            @RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId,
            @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel,
            @RequestBody OrderPayVirtualReq orderPayVirtualReq) {
        if (!orderPayVirtualReq.isValidPre()) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        return orderService.payVirtualPre(tenantId, accountModel, orderPayVirtualReq);
    }

    @PostMapping("/payVirtual")
    public R<?> payVirtual(
            @RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId,
            @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel,
            @RequestBody OrderPayVirtualReq orderPayVirtualReq) {
        if (!orderPayVirtualReq.isValid()) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        return orderService.payVirtual(tenantId, accountModel, orderPayVirtualReq);
    }

    /**
     * BD确认订单价格
     *
     * @param tenantId             租户ID
     * @param accountModel         账户信息
     * @param orderConfirmPriceReq 确认订单价格信息
     * @return R
     */
    @PostMapping("/price/confirm")
    public R<?> priceConfirm(
            @RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId,
            @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel,
            @RequestBody OrderConfirmPriceReq orderConfirmPriceReq) {
        if (StringUtil.isBlank(orderConfirmPriceReq.getOrderId()) || StringUtil.isBlank(orderConfirmPriceReq.getConfirmTotalPrice()) ||
                StringUtil.isBlank(orderConfirmPriceReq.getConfirmTotalPrice())) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        BigDecimal defaultBigDecimal = new BigDecimal(-1);
        if (Convert.toBigDecimal(orderConfirmPriceReq.getConfirmPrePaymentPrice(), defaultBigDecimal).doubleValue() == defaultBigDecimal.doubleValue()) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        if (Convert.toBigDecimal(orderConfirmPriceReq.getConfirmPrePaymentPrice(), defaultBigDecimal)
                .compareTo(BigDecimal.valueOf(0.5f).setScale(1, RoundingMode.HALF_UP)) < 0) {
            return R.failed(SysCode.x00000400.getValue(), "PrePayment Price must be at least $0.50 usd");
        }
        if (Convert.toBigDecimal(orderConfirmPriceReq.getConfirmTotalPrice(), defaultBigDecimal).doubleValue() == defaultBigDecimal.doubleValue()) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        // bd or finance or admin can operate
        if (accountModel.getAccountType().intValue() == AccountType.GAMING_TENANT_OWNER.getValue() ||
                accountModel.getAccountType().intValue() == AccountType.GAMING_TENANT_USER.getValue() ||
                accountModel.getAccountType().intValue() == AccountType.AI_TENANT_OWNER.getValue() ||
                accountModel.getAccountType().intValue() == AccountType.AI_TENANT_USER.getValue()) {
            return R.failed(SysCode.x00000403.getValue(), SysCode.x00000403.getMsg());
        }
        return orderService.priceConfirm(tenantId, accountModel, orderConfirmPriceReq);
    }

    /**
     * 财务发布订单价格
     *
     * @param tenantId             租户ID
     * @param accountModel         账户信息
     * @param orderPublishPriceReq 财务发布订单价格信息
     * @return R
     */
    @PostMapping("/price/publish")
    public R<?> pricePublish(
            @RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId,
            @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel,
            @RequestBody OrderPublishPriceReq orderPublishPriceReq) {
        if (StringUtil.isBlank(orderPublishPriceReq.getOrderId()) || orderPublishPriceReq.getPublish() == null) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        if (!orderPublishPriceReq.getPublish()) {
            if (StringUtil.isBlank(orderPublishPriceReq.getRejectMsg()) || orderPublishPriceReq.getRejectMsg().length() > 500) {
                return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
            }
        }
        // finance or admin can operate
        if (accountModel.getAccountType().intValue() != AccountType.FINANCE.getValue() &&
                accountModel.getAccountType().intValue() != AccountType.Admin.getValue()) {
            return R.failed(SysCode.x00000403.getValue(), SysCode.x00000403.getMsg());
        }
        return orderService.pricePublish(tenantId, accountModel, orderPublishPriceReq);
    }

    /**
     * 确认订单支付
     *
     * @param tenantId     租户ID
     * @param accountModel 账户信息
     * @param orderIdReq   确认订单支付信息
     * @return R
     */
    @PostMapping("/confirmPaid")
    public R<?> confirmPaid(
            @RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId,
            @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel,
            @RequestBody OrderIdReq orderIdReq) {
        if (StringUtil.isBlank(orderIdReq.getOrderId())) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        // finance or admin can operate
        if (accountModel.getAccountType().intValue() != AccountType.FINANCE.getValue() &&
                accountModel.getAccountType().intValue() != AccountType.Admin.getValue()) {
            return R.failed(SysCode.x00000403.getValue(), SysCode.x00000403.getMsg());
        }
        return orderService.confirmPaid(tenantId, accountModel, orderIdReq, SOURCE_TYPE_FROM_WEB);
    }

    /**
     * 删除订单
     *
     * @param tenantId     租户ID
     * @param accountModel 账户信息
     * @param orderIdReq   确认订单支付信息
     * @return R
     */
    @PostMapping("/delete")
    public R<?> delete(
            @RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId,
            @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel,
            @RequestBody OrderIdReq orderIdReq) {
        if (StringUtil.isBlank(orderIdReq.getOrderId())) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        return orderService.delete(tenantId, accountModel, orderIdReq);
    }

    /**
     * 终止订单
     *
     * @param tenantId     租户ID
     * @param accountModel 账户信息
     * @param orderIdReq   确认订单支付信息
     * @return R
     */
    @PostMapping("/terminate")
    public R<?> terminate(
            @RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId,
            @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel,
            @RequestBody OrderIdReq orderIdReq) {
        if (StringUtil.isBlank(orderIdReq.getOrderId())) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        // bd or finance or admin can operate
        if (accountModel.getAccountType().intValue() == AccountType.GAMING_TENANT_OWNER.getValue() ||
                accountModel.getAccountType().intValue() == AccountType.GAMING_TENANT_USER.getValue() ||
                accountModel.getAccountType().intValue() == AccountType.AI_TENANT_OWNER.getValue() ||
                accountModel.getAccountType().intValue() == AccountType.AI_TENANT_USER.getValue()) {
            return R.failed(SysCode.x00000403.getValue(), SysCode.x00000403.getMsg());
        }
        return orderService.terminate(tenantId, accountModel, orderIdReq);
    }

    /**
     * 获取逾期未支付的订单列表
     *
     * @param tenantId     租户ID
     * @param accountModel 账户信息
     * @return R
     */
    @PostMapping("/dueList")
    public R<List<DueDateOrderListResp>> dueOrderList(@RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId,
                                                      @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel) {
        return orderService.dueOrderList(tenantId, accountModel);
    }

    /**
     * 逾期订单支付
     *
     * @param tenantId       租户ID
     * @param accountModel   账户信息
     * @param dueOrderPayReq 逾期支付订单号信息
     * @return R
     */
    @PostMapping("/duePay")
    public R<OrderPayResp> duePayOrder(@RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId,
                                       @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel,
                                       @RequestBody List<DueOrderPayReq> dueOrderPayReq) {
        if (dueOrderPayReq == null || dueOrderPayReq.isEmpty()) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        if (dueOrderPayReq.stream().anyMatch(item -> StringUtil.isBlank(item.getOrderId()))) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        if (dueOrderPayReq.stream().anyMatch(item -> item.getPaymentOrderIds() == null || item.getPaymentOrderIds().isEmpty())) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        return orderService.duePayOrder(tenantId, accountModel, dueOrderPayReq);
    }

    /**
     * 获取租户逾期订单个数
     *
     * @param tenantId 租户Id
     * @return R
     */
    @GetMapping("/dueOrderCount")
    public R<Long> dueOrderCount(@RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId) {
        return orderService.dueOrderCount(tenantId);
    }

}
