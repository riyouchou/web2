package com.yx.web2.api.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.dynamic.datasource.annotation.Master;
import com.baomidou.dynamic.datasource.annotation.Slave;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.yx.pass.remote.pms.PmsRemoteContainerOrderService;
import com.yx.pass.remote.pms.PmsRemoteTenantService;
import com.yx.pass.remote.pms.model.resp.tenant.TenantInfoResp;
import com.yx.web2.api.common.constant.CacheConstants;
import com.yx.web2.api.common.constant.Web2ApiConstants;
import com.yx.web2.api.common.constant.Web2LoggerEvents;
import com.yx.web2.api.common.enums.*;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.common.model.StripePaymentMetaData;
import com.yx.web2.api.common.req.order.*;
import com.yx.web2.api.common.resp.PageResp;
import com.yx.web2.api.common.resp.order.DueDateOrderListResp;
import com.yx.web2.api.common.resp.order.OrderDetailResp;
import com.yx.web2.api.common.resp.order.OrderListResp;
import com.yx.web2.api.common.resp.order.OrderPayResp;
import com.yx.web2.api.common.web3api.Web3BasicDataService;
import com.yx.web2.api.config.Web2ApiConfig;
import com.yx.web2.api.entity.*;
import com.yx.web2.api.mapper.OrderMapper;
import com.yx.web2.api.service.*;
import com.yx.web2.api.service.payment.AthOrderPaymentService;
import com.yx.web2.api.service.payment.StripePaymentService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.logger.LogFieldConstants;
import org.yx.lib.utils.util.R;
import org.yx.lib.utils.util.SpringContextHolder;
import org.yx.lib.utils.util.StringPool;
import org.yx.lib.utils.util.StringUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.yx.web2.api.common.constant.Web2ApiConstants.SOURCE_TYPE_FROM_WEB;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderEntity> implements IOrderService {

    private final Web2ApiConfig web2ApiConfig;
    private final RedisTemplate<String, String> redisTemplate;
    private final Web3BasicDataService web3BasicDataService;

    private final IOrderDeviceService orderDeviceService;
    private final IOrderPaymentService orderPaymentService;
    private final StripePaymentService stripePaymentService;
    private final AthOrderPaymentService athOrderPaymentService;
    private final ICustomerPaymentMethodService customerPaymentMethodService;
    private final IAthOrderInfoService athOrderInfoService;
    private final ISubscribedServiceService subscribedServiceService;
    private final PmsRemoteContainerOrderService pmsRemoteContainerOrderService;
    private final PmsRemoteTenantService pmsRemoteTenantService;
    private final InstallmentCalculateService installmentCalculateService;
    private final IOrderVirtualPaymentService orderVirtualPaymentService;

    private static final DateTimeFormatter usDateTimeFormat = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US);

    @Slave
    @Override
    public R<PageResp<OrderListResp>> orderList(Long tenantId, AccountModel accountModel, OrderQueryReq orderQueryReq) {
        LambdaQueryWrapper<OrderEntity> lambdaQueryWrapper = Wrappers.lambdaQuery();
        lambdaQueryWrapper.eq(OrderEntity::getDeleted, false);
        if (orderQueryReq.getOrderStatus() != null) {
            lambdaQueryWrapper.eq(OrderEntity::getOrderStatus, orderQueryReq.getOrderStatus());
        }
        if (orderQueryReq.getPaymentStatus() != null) {
            lambdaQueryWrapper.eq(OrderEntity::getPaymentStatus, orderQueryReq.getPaymentStatus());
        }
        if (StringUtil.isNoneBlank(orderQueryReq.getAccountName())) {
            lambdaQueryWrapper.likeRight(OrderEntity::getTenantName, orderQueryReq.getAccountName());
        }
        if (orderQueryReq.getBdAccountId() != null) {
            lambdaQueryWrapper.eq(OrderEntity::getBdAccountId, orderQueryReq.getBdAccountId());
        }
        if (orderQueryReq.getBizType().equalsIgnoreCase("ARS")) {
            lambdaQueryWrapper.eq(OrderEntity::getOrderResourcePool, OrderResourcePool.ARS.getValue());
        } else {
            lambdaQueryWrapper.eq(OrderEntity::getOrderResourcePool, OrderResourcePool.BM.getValue());
        }

        if (accountModel.getAccountType().intValue() == AccountType.GAMING_TENANT_OWNER.getValue() ||
                accountModel.getAccountType().intValue() == AccountType.GAMING_TENANT_USER.getValue() ||
                accountModel.getAccountType().intValue() == AccountType.AI_TENANT_OWNER.getValue() ||
                accountModel.getAccountType().intValue() == AccountType.AI_TENANT_USER.getValue()) {
            lambdaQueryWrapper.eq(OrderEntity::getTenantId, tenantId);
        }
        if (accountModel.getAccountType().intValue() == AccountType.GAMING_BD.getValue() ||
                accountModel.getAccountType().intValue() == AccountType.AI_BD.getValue()) {
            lambdaQueryWrapper.eq(OrderEntity::getBdAccountId, accountModel.getAccountId());
        }

        IPage<OrderEntity> page = new Page<>(orderQueryReq.getCurrent(), orderQueryReq.getSize());
        page.orders().add(OrderItem.desc("id"));
        this.page(page, lambdaQueryWrapper);

        List<OrderListResp> resultList = new ArrayList<>();
        page.getRecords().forEach(orderEntity -> {
            String serviceTerm = "";
            if (orderEntity.getOrderStatus().intValue() == OrderStatus.InService.getValue() || orderEntity.getOrderStatus().intValue() == OrderStatus.End.getValue()) {
                Timestamp initialServiceEndTime = getServiceEndTime(orderEntity.getLastUpdateTime(), orderEntity.getServiceDuration(), orderEntity.getServiceDurationPeriod());
                serviceTerm = String.format("%s ~ %s", DateUtil.formatDateTime(orderEntity.getLastUpdateTime()), DateUtil.formatDateTime(initialServiceEndTime));
            }
            boolean isPaid = orderEntity.getOrderStatus().intValue() == OrderStatus.NotStarted.getValue() ||
                    orderEntity.getOrderStatus().intValue() == OrderStatus.InService.getValue() ||
                    orderEntity.getOrderStatus().intValue() == OrderStatus.End.getValue() ||
                    orderEntity.getOrderStatus().intValue() == OrderStatus.Failed.getValue();
            Set<Integer> terminatedStatuses = new HashSet<>(Arrays.asList(
                    OrderStatus.AbnormalCancel.getValue(),
                    OrderStatus.InsufficientCredit.getValue()
                    , OrderStatus.Terminated.getValue()
            ));
            resultList.add(OrderListResp.builder()
                    .orderId(orderEntity.getOrderId())
                    .orderStatus(terminatedStatuses.contains(orderEntity.getOrderStatus())
                            ? OrderStatus.Terminated.getValue()
                            : orderEntity.getOrderStatus())
                    .paymentStatus(orderEntity.getPaymentStatus())
                    .accountName(orderEntity.getTenantName())
                    .accountId(orderEntity.getTenantId())
                    .bdName(orderEntity.getBdAccountName())
                    .createTime(DateUtil.formatDateTime(orderEntity.getCreateTime()))
                    .initialPrice(orderEntity.getInitialPrice() != null ? orderEntity.getInitialPrice().setScale(2, RoundingMode.UP).toString() : "")
                    .currentPrice(orderEntity.getCurrentPrice() != null ? orderEntity.getCurrentPrice().setScale(2, RoundingMode.UP).toString() : "")
                    .prePaymentPrice(orderEntity.getPrePaymentPrice() != null ? orderEntity.getPrePaymentPrice().setScale(2, RoundingMode.UP).toString() : "")
                    .published(orderEntity.getPublishPrice() != null)
                    .paid(isPaid)
                    .reason(orderEntity.getReason())
                    .serviceTerm(serviceTerm)
                    .orderResourcePool(orderEntity.getOrderResourcePool() == null ? 1 : orderEntity.getOrderResourcePool())
                    .build());
        });
        PageResp<OrderListResp> resultData = new PageResp<>();
        resultData.setCurrent(orderQueryReq.getCurrent());
        resultData.setSize(orderQueryReq.getSize());
        resultData.setRecords(resultList);
        resultData.setTotal(page.getTotal());
        resultData.setPages(page.getPages());
        return R.ok(resultData);
    }

    @Slave
    @Override
    public R<OrderDetailResp> orderDetail(Long tenantId, AccountModel accountModel, String orderId) {
        OrderEntity orderEntity = null;
        if (accountModel.getAccountType().intValue() == AccountType.GAMING_TENANT_OWNER.getValue()
                || accountModel.getAccountType().intValue() == AccountType.GAMING_TENANT_USER.getValue()
                || accountModel.getAccountType().intValue() == AccountType.AI_TENANT_OWNER.getValue()
                || accountModel.getAccountType().intValue() == AccountType.AI_TENANT_USER.getValue()) {
            orderEntity = this.getOrderByTenantId(orderId, tenantId);
        }
        if (accountModel.getAccountType().intValue() == AccountType.GAMING_BD.getValue()
                || accountModel.getAccountType().intValue() == AccountType.AI_BD.getValue()) {
            orderEntity = this.getOrderByBdAccountId(orderId, accountModel.getAccountId());
        }
        if (accountModel.getAccountType().intValue() == AccountType.FINANCE.getValue() || accountModel.getAccountType().intValue() == AccountType.Admin.getValue()) {
            orderEntity = this.getOrder(orderId);
        }
        if (orderEntity == null) {
            return R.ok();
        }
        List<OrderDeviceEntity> orderDeviceEntityList = orderDeviceService.getDeviceList(orderId);
        String serviceTerm = "";
        if (orderEntity.getOrderStatus().intValue() == OrderStatus.InService.getValue() || orderEntity.getOrderStatus().intValue() == OrderStatus.End.getValue()) {
            Timestamp initialServiceEndTime = getServiceEndTime(orderEntity.getLastUpdateTime(), orderEntity.getServiceDuration(), orderEntity.getServiceDurationPeriod());
            serviceTerm = String.format("%s ~ %s", DateUtil.formatDateTime(orderEntity.getLastUpdateTime()), DateUtil.formatDateTime(initialServiceEndTime));
        }
        String discountPrice = "";
        if (orderEntity.getCurrentPrice() != null) {
            discountPrice = orderEntity.getInitialPrice().subtract(orderEntity.getCurrentPrice()).setScale(2, RoundingMode.UP).toString();
        }
        Set<Integer> terminatedStatuses = new HashSet<>(Arrays.asList(
                OrderStatus.AbnormalCancel.getValue(),
                OrderStatus.InsufficientCredit.getValue()
                , OrderStatus.Terminated.getValue()
        ));
        OrderDetailResp orderDetailResp = OrderDetailResp.builder()
                .orderId(orderEntity.getOrderId())
                .orderStatus(terminatedStatuses.contains(orderEntity.getOrderStatus())
                        ? OrderStatus.Terminated.getValue()
                        : orderEntity.getOrderStatus())
                .serviceDurationPeriod(orderEntity.getServiceDurationPeriod())
                .serviceDuration(orderEntity.getServiceDuration())
                .paymentStatus(orderEntity.getPaymentStatus())
                .accountName(orderEntity.getAccountName())
                .accountId(orderEntity.getAccountId())
                .bdName(orderEntity.getBdAccountName())
                .createTime(DateUtil.formatDateTime(orderEntity.getCreateTime()))
                .initialPrice(orderEntity.getInitialPrice() != null ? orderEntity.getInitialPrice().setScale(2, RoundingMode.UP).toString() : "")
                .currentPrice(orderEntity.getCurrentPrice() != null ? orderEntity.getCurrentPrice().setScale(2, RoundingMode.UP).toString() : "")
                .discountPrice(discountPrice)
                .prePaymentPrice(orderEntity.getPrePaymentPrice() != null ? orderEntity.getPrePaymentPrice().setScale(2, RoundingMode.UP).toString() : "")
                .published(orderEntity.getPublishPrice() != null)
                .paid(orderEntity.getPaymentStatus().intValue() == OrderPaymentStatus.NormalPayment.getValue().intValue())
                .serviceTerm(serviceTerm)
                .reason(orderEntity.getReason())
                .monthlyPayment(orderEntity.getInstalmentMonthPaymentAvg() != null ? orderEntity.getInstalmentMonthPaymentAvg().setScale(2, RoundingMode.UP).toString() : "")
                .monthlyPaymentCycle(orderEntity.getInstalmentMonthTotal())
                .build();
        List<OrderDetailResp.OrderDevice> orderDeviceList = Lists.newArrayList();
        orderDeviceEntityList.forEach(orderDeviceEntity -> {
            orderDeviceList.add(OrderDetailResp.OrderDevice.builder()
                    .regionCode(orderDeviceEntity.getRegionCode())
                    .regionName(orderDeviceEntity.getRegionName())
                    .spec(orderDeviceEntity.getSpec())
                    .specName(orderDeviceEntity.getSpecName())
                    .quantity(orderDeviceEntity.getQuantity())
                    .gpuInfo(orderDeviceEntity.getGpuInfo())
                    .cpuInfo(orderDeviceEntity.getCpuInfo())
                    .mem(orderDeviceEntity.getMem())
                    .disk(orderDeviceEntity.getDisk())
                    .unitPrice(orderDeviceEntity.getUnitPrice())
                    .discountPrice(orderDeviceEntity.getDiscountPrice())
                    .deviceInfo(orderDeviceEntity.getDeviceInfo())
                    .build());
        });
        orderDetailResp.setDevices(orderDeviceList);
        return R.ok(orderDetailResp);
    }

    @Master
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> createOrder(Long tenantId, AccountModel accountModel, CreateOrderReq createOrderReq) {
        if (createOrderReq.getOrderResourcePool() == null) {
            createOrderReq.setOrderResourcePool(OrderResourcePool.ARS.getValue());
        }
        String createOrderMd5 = createOrderReq.toMd5();
        Boolean isNotIdempotent = redisTemplate.opsForValue().setIfAbsent(
                String.format(CacheConstants.ORDER_IDEMPOTENT, createOrderMd5), createOrderMd5, Duration.ofSeconds(web2ApiConfig.getOrder().getIdempotentInterval()));
        if (isNotIdempotent != null && isNotIdempotent) {
            // get account has bound payment method
            CustomerPaymentMethodEntity customerPaymentMethod = customerPaymentMethodService.getSimple(accountModel.getTenantId());
            // create order
            OrderEntity orderEntity = OrderEntity.builder()
                    .orderId(OrderIdGenerate.generateMainOrderId(Web2ApiConstants.ORDER_ID_MAIN_SOURCE_TYPE))
                    .initialPrice(new BigDecimal(createOrderReq.getInitialPrice()).setScale(2, RoundingMode.UP))
                    .serviceDuration(createOrderReq.getServiceDuration())
                    .serviceDurationPeriod(createOrderReq.getServiceDurationPeriod())
                    .autoRenew(createOrderReq.getAutoRenew())
                    .instalmentMonthTotal(createOrderReq.getAutoRenew() ? null : 0)
                    .orderStatus(OrderStatus.PendingReview.getValue())
                    .paymentStatus(OrderPaymentStatus.None.getValue())
                    .customerId(customerPaymentMethod != null ? customerPaymentMethod.getCustomerId() : "")
                    .redirectUrl(createOrderReq.getRedirectUrl())
                    .accountId(accountModel.getAccountId())
                    .accountName(accountModel.getAccountName())
                    .bdAccountId(accountModel.getBdAccountId())
                    .bdAccountName(accountModel.getBdAccountName())
                    .tenantId(tenantId)
                    .tenantName(accountModel.getTenantName())
                    .createTime(new Timestamp(System.currentTimeMillis()))
                    .orderResourcePool(createOrderReq.getOrderResourcePool())
                    .deleted(false)
                    .build();
            List<OrderDeviceEntity> orderDeviceEntityList = Lists.newArrayList();
            for (CreateOrderReq.OrderDevice orderDeviceReq : createOrderReq.getDevices()) {
                String regionName = web3BasicDataService.getRegion(orderDeviceReq.getRegionCode());
                if (StringUtil.isBlank(regionName)) {
                    return R.failed(SysCode.x00000404.getValue(), "Not found region '" + orderDeviceReq.getRegionCode() + "'");
                }
                Web3BasicDataService.SpecCacheInfo specCacheInfo = web3BasicDataService.getSpec(orderDeviceReq.getSpec());
                if (specCacheInfo == null) {
                    return R.failed(SysCode.x00000404.getValue(), "Not found spec '" + orderDeviceReq.getSpec() + "'");
                }
                OrderDeviceEntity orderDeviceEntity = OrderDeviceEntity.builder()
                        .orderId(orderEntity.getOrderId())
                        .regionCode(orderDeviceReq.getRegionCode())
                        .regionName(regionName)
                        .spec(orderDeviceReq.getSpec())
                        .subSpec(orderDeviceReq.getSubSpec())
                        .specName(specCacheInfo.getSpecName())
                        .resourcePool(orderDeviceReq.getResourcePool())
                        .gpuInfo(specCacheInfo.getGpu())
                        .cpuInfo(specCacheInfo.getCpu())
                        .mem(specCacheInfo.getMem())
                        .disk(specCacheInfo.getDisk())
                        .unitPrice(orderDeviceReq.getUnitPrice())
                        .quantity(orderDeviceReq.getQuantity())
                        .deployRegionCode(orderDeviceReq.getDeployRegionCode())
                        .deviceInfo(orderDeviceReq.getDeviceInfo())
                        .createTime(new Timestamp(System.currentTimeMillis()))
                        .build();
                orderDeviceEntityList.add(orderDeviceEntity);
            }
            baseMapper.insert(orderEntity);
            orderDeviceService.saveBatch(orderDeviceEntityList);
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_CREATE_ORDER)
                    .p(LogFieldConstants.ACCOUNT_ID, accountModel.getAccountId())
                    .p(LogFieldConstants.ACCOUNT_NAME, accountModel.getAccountName())
                    .p(LogFieldConstants.TENANT_ID, tenantId)
                    .flat("OrderInfo", createOrderReq)
                    .p("OrderResourcePool", createOrderReq.getOrderResourcePool().intValue() == OrderResourcePool.ARS.getValue() ? "ARS" : "BM")
                    .p("OrderStatus", OrderStatus.PendingReview.getName())
                    .p("OrderId", orderEntity.getOrderId())
                    .i();
            return R.ok(orderEntity.getOrderId());
        } else {
            return R.failed(SysCode.x00000010.getValue(), SysCode.x00000010.getMsg());
        }
    }

    @Master
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<?> payVirtualPre(Long tenantId, AccountModel accountModel, OrderPayVirtualReq orderPayVirtualReq) {
        try {
            List<OrderEntity> orderEntities = list(Wrappers.lambdaQuery(OrderEntity.class)
                    .in(OrderEntity::getOrderId, orderPayVirtualReq.getOrderPayList().stream().map(OrderPayVirtualReq.OrderPay::getOrderId).collect(Collectors.toList())));
            if (orderEntities == null || orderEntities.isEmpty() || orderEntities.size() != orderPayVirtualReq.getOrderPayList().size()) {
                return R.failed(SysCode.x00000455.getValue(), SysCode.x00000455.getMsg());
            }
            List<String> duePaymentOrderIds = orderPayVirtualReq.getOrderPayList().stream().map(OrderPayVirtualReq.OrderPay::getOrderPaymentIdList).flatMap(Collection::stream).collect(Collectors.toList());
            List<OrderPaymentEntity> orderPaymentEntities = orderPaymentService.list(Wrappers.lambdaQuery(OrderPaymentEntity.class)
                    .ne(OrderPaymentEntity::getPaymentStatus, InstalmentPaymentStatus.PAID.getValue())
                    .in(OrderPaymentEntity::getPaymentOrderId, duePaymentOrderIds));


            Map<String, List<OrderPaymentEntity>> orderIdGroups = orderPaymentEntities.stream().collect(Collectors.groupingBy(OrderPaymentEntity::getOrderId));
            // check
            for (OrderPayVirtualReq.OrderPay orderPay : orderPayVirtualReq.getOrderPayList()) {
                List<OrderPaymentEntity> paymentEntities = orderIdGroups.get(orderPay.getOrderId());
                if (paymentEntities == null || paymentEntities.isEmpty() || paymentEntities.size() != orderPay.getOrderPaymentIdList().size()) {
                    return R.failed(SysCode.x00000456.getValue(), SysCode.x00000456.getMsg());
                }
            }

            // sum total price
            BigDecimal hpTotalPrice = orderPaymentEntities.stream()
                    .map(OrderPaymentEntity::getHpPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal defaultBigDecimal = Convert.toBigDecimal(orderPayVirtualReq.getAmount(), BigDecimal.ZERO);
            if (defaultBigDecimal.compareTo(hpTotalPrice) != 0) {
                return R.failed(SysCode.x00000457.getValue(), SysCode.x00000457.getMsg());
            }
            Map<String, OrderVirtualPaymentEntity> existingPayments = orderVirtualPaymentService.list(Wrappers.lambdaQuery(OrderVirtualPaymentEntity.class)
                            .in(OrderVirtualPaymentEntity::getPaymentOrderId, duePaymentOrderIds))
                    .stream()
                    .collect(Collectors.toMap(OrderVirtualPaymentEntity::getPaymentOrderId, entity -> entity,
                            (existing, replacement) -> existing));

            List<OrderVirtualPaymentEntity> orderVirtualPaymentEntityList = new ArrayList<>();
            for (OrderPaymentEntity orderPaymentEntity : orderPaymentEntities) {
                String paymentOrderId = orderPaymentEntity.getPaymentOrderId();
                OrderVirtualPaymentEntity existingPayment = existingPayments.get(paymentOrderId);

                OrderVirtualPaymentEntity paymentEntity = existingPayment != null ? existingPayment : new OrderVirtualPaymentEntity();
                paymentEntity.setTenantId(tenantId);
                paymentEntity.setFromAddress(orderPayVirtualReq.getFrom());
                paymentEntity.setToAddress(orderPayVirtualReq.getTo());
                paymentEntity.setAmount(hpTotalPrice.toPlainString());
                paymentEntity.setType(orderPayVirtualReq.getType());
                paymentEntity.setStatus(OrderPaymentStatus.None.getValue());
                paymentEntity.setLastUpdateTime(new Timestamp(System.currentTimeMillis()));
                if (existingPayment == null) {
                    paymentEntity.setOrderId(orderPaymentEntity.getOrderId());
                    paymentEntity.setPaymentOrderId(paymentOrderId);
                    paymentEntity.setCreateTime(new Timestamp(System.currentTimeMillis()));
                }
                orderVirtualPaymentEntityList.add(paymentEntity);
            }

            // 批量保存或更新虚拟支付记录
            if (!orderVirtualPaymentService.saveOrUpdateBatch(orderVirtualPaymentEntityList)) {
                throw new RuntimeException("orderVirtualPaymentService save or update batch failed");
            }

            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_VIRTUAL_PAY)
                    .p(LogFieldConstants.ACCOUNT_ID, accountModel.getAccountId())
                    .p(LogFieldConstants.ACCOUNT_NAME, accountModel.getAccountName())
                    .p(LogFieldConstants.TENANT_ID, tenantId)
                    .p("Amount", hpTotalPrice)
                    .p("DueOrderInfo", JSON.toJSONString(orderPayVirtualReq))
                    .i();
            return R.ok();
        } catch (Exception ex) {
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_DUE_PAY)
                    .p(LogFieldConstants.ACCOUNT_ID, accountModel.getAccountId())
                    .p(LogFieldConstants.ACCOUNT_NAME, accountModel.getAccountName())
                    .p(LogFieldConstants.TENANT_ID, tenantId)
                    .p("DueOrderInfo", JSON.toJSONString(orderPayVirtualReq))
                    .e(ex);
            return R.failed(SysCode.x00000807.getValue(), SysCode.x00000807.getMsg());
        }
    }

    @Master
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<?> payVirtual(Long tenantId, AccountModel accountModel, OrderPayVirtualReq orderPayVirtualReq) {
        try {
            List<OrderEntity> orderEntities = list(Wrappers.lambdaQuery(OrderEntity.class)
                    .in(OrderEntity::getOrderId, orderPayVirtualReq.getOrderPayList().stream().map(OrderPayVirtualReq.OrderPay::getOrderId).collect(Collectors.toList())));
            if (orderEntities == null || orderEntities.isEmpty() || orderEntities.size() != orderPayVirtualReq.getOrderPayList().size()) {
                return R.failed(SysCode.x00000458.getValue(), SysCode.x00000458.getMsg());
            }
            List<String> duePaymentOrderIds = orderPayVirtualReq.getOrderPayList().stream().map(OrderPayVirtualReq.OrderPay::getOrderPaymentIdList).flatMap(Collection::stream).collect(Collectors.toList());
            List<OrderPaymentEntity> orderPaymentEntities = orderPaymentService.list(Wrappers.lambdaQuery(OrderPaymentEntity.class)
                    .ne(OrderPaymentEntity::getPaymentStatus, InstalmentPaymentStatus.PAID.getValue())
                    .in(OrderPaymentEntity::getPaymentOrderId, duePaymentOrderIds));


            Map<String, List<OrderPaymentEntity>> orderIdGroups = orderPaymentEntities.stream().collect(Collectors.groupingBy(OrderPaymentEntity::getOrderId));
            // check
            for (OrderPayVirtualReq.OrderPay orderPay : orderPayVirtualReq.getOrderPayList()) {
                List<OrderPaymentEntity> paymentEntities = orderIdGroups.get(orderPay.getOrderId());
                if (paymentEntities == null || paymentEntities.isEmpty() || paymentEntities.size() != orderPay.getOrderPaymentIdList().size()) {
                    return R.failed(SysCode.x00000459.getValue(), SysCode.x00000459.getMsg());
                }
            }

            // sum total price
            BigDecimal hpTotalPrice = orderPaymentEntities.stream()
                    .map(OrderPaymentEntity::getHpPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal defaultBigDecimal = Convert.toBigDecimal(orderPayVirtualReq.getAmount(), BigDecimal.ZERO);
            if (defaultBigDecimal.compareTo(hpTotalPrice) != 0) {
                return R.failed(SysCode.x00000460.getValue(), SysCode.x00000460.getMsg());
            }

            Map<String, OrderVirtualPaymentEntity> existingPayments = orderVirtualPaymentService.list(Wrappers.lambdaQuery(OrderVirtualPaymentEntity.class)
                            .in(OrderVirtualPaymentEntity::getPaymentOrderId, duePaymentOrderIds))
                    .stream()
                    .collect(Collectors.toMap(OrderVirtualPaymentEntity::getPaymentOrderId, entity -> entity,
                            (existing, replacement) -> existing));

            List<OrderVirtualPaymentEntity> orderVirtualPaymentEntityList = new ArrayList<>();
            for (OrderPaymentEntity orderPaymentEntity : orderPaymentEntities) {
                String paymentOrderId = orderPaymentEntity.getPaymentOrderId();
                OrderVirtualPaymentEntity existingVirtualPayment = existingPayments.get(paymentOrderId);
                if (existingVirtualPayment == null ){
                    return  R.failed(SysCode.x00000461.getValue(), SysCode.x00000461.getMsg());
                }
                existingVirtualPayment.setTenantId(tenantId);
                existingVirtualPayment.setFromAddress(orderPayVirtualReq.getFrom());
                existingVirtualPayment.setToAddress(orderPayVirtualReq.getTo());
                existingVirtualPayment.setAmount(hpTotalPrice.toPlainString());
                existingVirtualPayment.setType(orderPayVirtualReq.getType());
                existingVirtualPayment.setStatus(OrderPaymentStatus.None.getValue());
                existingVirtualPayment.setHashCode(orderPayVirtualReq.getHashCode());
                existingVirtualPayment.setLastUpdateTime(new Timestamp(System.currentTimeMillis()));
                orderVirtualPaymentEntityList.add(existingVirtualPayment);
            }
            // 批量保存或更新虚拟支付记录
            if (!orderVirtualPaymentService.updateBatchById(orderVirtualPaymentEntityList)) {
                throw new RuntimeException("orderVirtualPaymentService save or update batch failed");
            }

            // order success
            IOrderService orderService = SpringContextHolder.getBean(IOrderService.class);
            orderService.savePaymentRecord(orderPayVirtualReq, true);

            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_VIRTUAL_PAY)
                    .p(LogFieldConstants.ACCOUNT_ID, accountModel.getAccountId())
                    .p(LogFieldConstants.ACCOUNT_NAME, accountModel.getAccountName())
                    .p(LogFieldConstants.TENANT_ID, tenantId)
                    .p("Amount", hpTotalPrice)
                    .p("orderPayVirtualReq", JSON.toJSONString(orderPayVirtualReq))
                    .i();
            return R.ok();
        } catch (Exception ex) {
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_DUE_PAY)
                    .p(LogFieldConstants.ACCOUNT_ID, accountModel.getAccountId())
                    .p(LogFieldConstants.ACCOUNT_NAME, accountModel.getAccountName())
                    .p(LogFieldConstants.TENANT_ID, tenantId)
                    .p("orderPayVirtualReq", JSON.toJSONString(orderPayVirtualReq))
                    .e(ex);
            return R.failed(SysCode.x00000808.getValue(), SysCode.x00000808.getMsg());
        }
    }

    @Master
    @Override
    public R<OrderPayResp> pay(Long tenantId, AccountModel accountModel, OrderPayReq orderPayReq) {
//        OrderEntity orderEntity = this.getOrder(orderPayReq.getOrderId(), accountModel.getAccountId());
        OrderEntity orderEntity = this.getOrderByTenantId(orderPayReq.getOrderId(), tenantId);
        if (orderEntity == null) {
            return R.failed(SysCode.x00000404.getValue(), SysCode.x00000404.getMsg());
        }
        if (orderEntity.getPublishPrice() == null) {
            return R.failed(SysCode.x00000405.getValue(), "Order price not published");
        }
        if (orderEntity.getOrderStatus().intValue() == OrderStatus.InService.getValue() ||
                orderEntity.getOrderStatus().intValue() == OrderStatus.End.getValue() ||
                orderEntity.getOrderStatus().intValue() == OrderStatus.Failed.getValue() ||
                orderEntity.getOrderStatus().intValue() == OrderStatus.Terminated.getValue() ||
                orderEntity.getOrderStatus().intValue() == OrderStatus.Cancel.getValue()) {
            return R.failed(SysCode.x00000449.getValue(), SysCode.x00000449.getMsg());
        }
        List<OrderPaymentEntity> firstOrderPaymentEntities = orderPaymentService.getPrePaymentOrder(orderPayReq.getOrderId());
        if (firstOrderPaymentEntities == null || firstOrderPaymentEntities.isEmpty()) {
            return R.failed(SysCode.x00000404.getValue(), SysCode.x00000404.getMsg());
        }
        if (firstOrderPaymentEntities.stream().anyMatch(item -> item.getPaymentStatus() != null && item.getPaymentStatus() ==
                InstalmentPaymentStatus.PAID.getValue().intValue())) {
            return R.failed(SysCode.x00000410.getValue(), SysCode.x00000410.getMsg());
        }
//        if (orderPayment.getPaymentStatus() != null && orderPayment.getPaymentStatus() == InstalmentPaymentStatus.PAID.getValue().intValue()) {
//            return R.failed(SysCode.x00000410.getValue(), SysCode.x00000410.getMsg());
//        }
        CustomerPaymentMethodEntity customerPaymentMethod = customerPaymentMethodService.getSimple(tenantId);
        if (customerPaymentMethod == null) {
            return R.failed(SysCode.x00000407.getValue(), SysCode.x00000407.getMsg());
        }
        // 支付id是否有效
        if (firstOrderPaymentEntities.stream().anyMatch(item -> StringUtil.isNotBlank(item.getPayId()) && item.getPaymentStatus() ==
                InstalmentPaymentStatus.None.getValue().intValue())) {
            return R.ok(OrderPayResp.builder().payId(firstOrderPaymentEntities.get(0).getPayId()).build());
        }
//        if (StringUtil.isNotBlank(orderPayment.getPayId()) && orderPayment.getPaymentStatus().intValue() == InstalmentPaymentStatus.None.getValue()) {
//            return R.ok(OrderPayResp.builder().payId(orderPayment.getPayId()).build());
//        }
        // new user first payment update order customerId
        if (StringUtil.isBlank(orderEntity.getCustomerId())) {
            orderEntity.setCustomerId(customerPaymentMethod.getCustomerId());
            update(Wrappers.lambdaUpdate(OrderEntity.class)
                    .set(OrderEntity::getCustomerId, customerPaymentMethod.getCustomerId())
                    .eq(OrderEntity::getId, orderEntity.getId()));
        }
        // call stripe create paymentIntent
        BigDecimal hpTotalPrice = new BigDecimal("0.00");
        for (OrderPaymentEntity orderPaymentEntity : firstOrderPaymentEntities) {
            hpTotalPrice = hpTotalPrice.add(orderPaymentEntity.getHpPrice());
        }
        try {
            List<Long> pIds = firstOrderPaymentEntities.stream().map(OrderPaymentEntity::getId).collect(Collectors.toList());
            // build meta data
            StripePaymentMetaData.OrderIdInfo orderIdInfo = new StripePaymentMetaData.OrderIdInfo();
            orderIdInfo.setOid(orderEntity.getId());
            orderIdInfo.setPIds(pIds);

            List<StripePaymentMetaData.OrderIdInfo> orderIdInfoList = Lists.newArrayList();
            orderIdInfoList.add(orderIdInfo);

            StripePaymentMetaData metaData = new StripePaymentMetaData();
            metaData.setMetaData(orderIdInfoList);

            // create order PaymentIntent
            PaymentIntent paymentIntent = stripePaymentService.createPaymentIntent(
                    StripePaymentService.CreatePaymentIntentParams.builder()
                            .metaData(metaData)
                            .paymentMethodId(customerPaymentMethod.getPaymentMethodId())
                            .customerId(orderEntity.getCustomerId())
                            .hpPrice(hpTotalPrice)
                            .build());
            String payId = paymentIntent.getClientSecret();

            OrderPaymentEntity toUpdateEntity = OrderPaymentEntity.builder()
                    .payId(payId)
                    .payLinkExpireAt(new Timestamp(DateUtils.addHours(new Date(), 12).getTime()))
                    .lastUpdateTime(new Timestamp(System.currentTimeMillis()))
                    .build();
            List<String> paymentOrderIds = firstOrderPaymentEntities.stream().map(OrderPaymentEntity::getPaymentOrderId).collect(Collectors.toList());
            boolean isSuccess = orderPaymentService.update(toUpdateEntity,
                    Wrappers.lambdaUpdate(OrderPaymentEntity.class)
                            .in(OrderPaymentEntity::getPaymentOrderId, paymentOrderIds));

            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_CREATE_PRE_PAYMENT)
                    .p(LogFieldConstants.ACCOUNT_ID, accountModel.getAccountId())
                    .p(LogFieldConstants.ACCOUNT_NAME, accountModel.getAccountName())
                    .p(LogFieldConstants.TENANT_ID, tenantId)
                    .p("OrderId", orderEntity.getOrderId())
                    .p("Amount", hpTotalPrice)
                    .p("PaymentOrderId", JSON.toJSONString(paymentOrderIds))
                    .p("PayId", payId)
                    .p(LogFieldConstants.Success, isSuccess)
                    .i();
            if (isSuccess) {
                return R.ok(OrderPayResp.builder().payId(payId).build());
            } else {
                return R.failed(SysCode.x00000404.getValue(), "The payment order information has changed, please refresh");
            }
        } catch (Exception ex) {
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_PAY)
                    .p(LogFieldConstants.ACCOUNT_ID, accountModel.getAccountId())
                    .p(LogFieldConstants.ACCOUNT_NAME, accountModel.getAccountName())
                    .p(LogFieldConstants.TENANT_ID, tenantId)
                    .p("OrderId", orderEntity.getOrderId())
                    .p("Amount", hpTotalPrice)
                    .e(ex);
            return R.failed(SysCode.x00000805.getValue(), SysCode.x00000805.getMsg());
        }
    }

    @Slave
    @Override
    public OrderEntity getOrder(String orderId) {
        return this.getOne(Wrappers.lambdaQuery(OrderEntity.class).eq(OrderEntity::getOrderId, orderId).eq(OrderEntity::getDeleted, false));
    }

    @Slave
    @Override
    public OrderEntity getOrder(String orderId, Long accountId) {
        return this.getOne(Wrappers.lambdaQuery(OrderEntity.class).eq(OrderEntity::getOrderId, orderId).eq(OrderEntity::getAccountId, accountId).eq(OrderEntity::getDeleted, false));
    }

    @Slave
    @Override
    public OrderEntity getOrderByTenantId(String orderId, Long tenantId) {
        return this.getOne(Wrappers.lambdaQuery(OrderEntity.class).eq(OrderEntity::getOrderId, orderId).eq(OrderEntity::getTenantId, tenantId).eq(OrderEntity::getDeleted, false));
    }

    @Slave
    @Override
    public OrderEntity getOrderByBdAccountId(String orderId, Long bdAccountId) {
        return this.getOne(Wrappers.lambdaQuery(OrderEntity.class).eq(OrderEntity::getOrderId, orderId).eq(OrderEntity::getBdAccountId, bdAccountId).eq(OrderEntity::getDeleted, false));
    }

    @Slave
    @Override
    public OrderEntity getOrderBySubscriptionId(String subscriptionId) {
        return this.getOne(Wrappers.lambdaQuery(OrderEntity.class).eq(OrderEntity::getSubscriptionId, subscriptionId).eq(OrderEntity::getDeleted, false));
    }

    @Master
    @Override
    public R<?> priceConfirm(Long tenantId, AccountModel accountModel, OrderConfirmPriceReq orderConfirmPriceReq) {
        OrderEntity orderEntity = this.getOrder(orderConfirmPriceReq.getOrderId());
        if (orderEntity == null) {
            return R.failed(SysCode.x00000404.getValue(), SysCode.x00000404.getMsg());
        }
        if (orderEntity.getPublishPrice() != null) {
            return R.failed(SysCode.x00000405.getValue(), "Order already published price");
        }
        List<Integer> invalidStatuses = Arrays.asList(
                OrderStatus.End.getValue(),
                OrderStatus.Failed.getValue(),
                OrderStatus.Terminated.getValue(),
                OrderStatus.Cancel.getValue()
        );
        if (invalidStatuses.contains(orderEntity.getOrderStatus())) {
            return R.failed(SysCode.x00000405.getValue(), "Order status is invalid for price Confirm");
        }
        BigDecimal totalPrice = new BigDecimal(orderConfirmPriceReq.getConfirmTotalPrice()).setScale(2, RoundingMode.UP);
        BigDecimal prePaymentPrice = new BigDecimal(orderConfirmPriceReq.getConfirmPrePaymentPrice()).setScale(2, RoundingMode.UP);

        if (prePaymentPrice.compareTo(totalPrice) > 0) {
            return R.failed(SysCode.x00000405.getValue(), "The installment price cannot exceed the total price");
        }
        LambdaUpdateWrapper<OrderEntity> updateWrapper = Wrappers.lambdaUpdate(OrderEntity.class);
        updateWrapper.eq(OrderEntity::getOrderId, orderConfirmPriceReq.getOrderId()).eq(OrderEntity::getDeleted, 0);
        if (orderEntity.getCurrentPrice() != null) {
            updateWrapper.eq(OrderEntity::getCurrentPrice, orderEntity.getCurrentPrice());
        }
        if (orderEntity.getPrePaymentPrice() != null) {
            updateWrapper.eq(OrderEntity::getPrePaymentPrice, orderEntity.getPrePaymentPrice());
        }

        OrderEntity toUpdateEntity = OrderEntity.builder()
                .currentPrice(totalPrice)
                .prePaymentPrice(prePaymentPrice)
                .lastUpdateAccountId(accountModel.getAccountId())
                .lastUpdateTime(new Timestamp(System.currentTimeMillis()))
                .build();
        boolean isSuccess = this.update(toUpdateEntity, updateWrapper);

        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_BD_CONFIRM_PRICE)
                .p(LogFieldConstants.ACCOUNT_ID, accountModel.getAccountId())
                .p(LogFieldConstants.ACCOUNT_NAME, accountModel.getAccountName())
                .p(LogFieldConstants.TENANT_ID, tenantId).p("initialPrice", "")
                .p("ConfirmPrice", totalPrice).p("confirmPrePaymentPrice", prePaymentPrice)
                .p("OrderId", orderConfirmPriceReq.getOrderId()).p("success", isSuccess)
                .i();
        if (isSuccess) {
            return R.ok();
        } else {
            return R.failed(SysCode.x00000404.getValue(), "The order information has changed, please refresh");
        }
    }

    @Master
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<?> pricePublish(Long tenantId, AccountModel accountModel, OrderPublishPriceReq orderPublishPriceReq) {
        OrderEntity orderEntity = this.getOrder(orderPublishPriceReq.getOrderId());
        if (orderEntity == null) {
            return R.failed(SysCode.x00000404.getValue(), SysCode.x00000404.getMsg());
        }
        if (orderEntity.getCurrentPrice() == null) {
            return R.failed(SysCode.x00000405.getValue(), "Order price not confirmed");
        }
        if (orderEntity.getPublishPrice() != null) {
            return R.failed(SysCode.x00000405.getValue(), "The order price has been published");
        }
        List<Integer> invalidStatuses = Arrays.asList(
                OrderStatus.End.getValue(),
                OrderStatus.Failed.getValue(),
                OrderStatus.Terminated.getValue(),
                OrderStatus.Cancel.getValue()
        );
        if (invalidStatuses.contains(orderEntity.getOrderStatus())) {
            return R.failed(SysCode.x00000405.getValue(), "Order status is invalid for price publishing");
        }

        LambdaUpdateWrapper<OrderEntity> updateWrapper = Wrappers.lambdaUpdate(OrderEntity.class);
        updateWrapper.eq(OrderEntity::getOrderId, orderPublishPriceReq.getOrderId())
                .eq(OrderEntity::getDeleted, false)
                .isNull(OrderEntity::getPublishPrice);
        if (!orderPublishPriceReq.getPublish()) {
            OrderEntity toUpdateEntity = OrderEntity.builder()
                    .financeAccountId(accountModel.getAccountId())
                    .financeAccountName(accountModel.getAccountName())
                    .lastUpdateAccountId(accountModel.getAccountId())
                    .reason(orderPublishPriceReq.getRejectMsg())
                    .lastUpdateTime(new Timestamp(System.currentTimeMillis()))
                    .build();
            if (this.update(toUpdateEntity, updateWrapper)) {
                return R.ok();
            } else {
                return R.failed(SysCode.x00000404.getValue(), "The order information has changed, please refresh");
            }
        }
        OrderEntity toUpdateEntity = OrderEntity.builder()
                .publishPrice(orderEntity.getCurrentPrice())
                .orderStatus(OrderStatus.WaitingPayment.getValue())
                .financeAccountId(accountModel.getAccountId())
                .financeAccountName(accountModel.getAccountName())
                .lastUpdateAccountId(accountModel.getAccountId())
                .lastUpdateTime(new Timestamp(System.currentTimeMillis()))
                .build();
        boolean isSuccess = this.update(toUpdateEntity, updateWrapper);

        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_FINANCE_PUBLISH_PRICE)
                .p(LogFieldConstants.ACCOUNT_ID, accountModel.getAccountId())
                .p(LogFieldConstants.ACCOUNT_NAME, accountModel.getAccountName())
                .p(LogFieldConstants.TENANT_ID, tenantId)
                .p("OrderStatus", OrderStatus.WaitingPayment.getName())
                .p("PublishPrice", orderEntity.getCurrentPrice())
                .p("OrderId", orderEntity.getOrderId())
                .p(LogFieldConstants.Success, isSuccess)
                .i();
        if (isSuccess) {
            orderEntity.setPublishPrice(orderEntity.getCurrentPrice());
            // price publish, do order instalment plan
            InstallmentCalculateService.InstallmentInput input = new InstallmentCalculateService.InstallmentInput();
            input.setServiceDuration(orderEntity.getServiceDuration());
            input.setServiceDurationPeriod(orderEntity.getServiceDurationPeriod());
            input.setFreeServiceTermHours(0);
            input.setPrePaymentPrice(orderEntity.getPrePaymentPrice());
            input.setTotalAmount(orderEntity.getPublishPrice());
            InstallmentCalculateService.InstallmentOutput output = installmentCalculateService.calculate(input);

            List<OrderPaymentEntity> orderPaymentEntityList = orderPaymentService.prepareOrderInstalmentPlan(tenantId, accountModel, orderEntity, output);
            orderPaymentService.saveBatch(orderPaymentEntityList);

            // update order instalmentMonthTotal
            toUpdateEntity = OrderEntity.builder()
                    .instalmentMonthTotal(output.getTotalInstalmentCount())
                    .instalmentMonthPaymentAvg(output.getAvgMonthlyPaymentAmount())
                    .build();
            updateWrapper = Wrappers.lambdaUpdate(OrderEntity.class)
                    .eq(OrderEntity::getOrderId, orderPublishPriceReq.getOrderId())
                    .eq(OrderEntity::getDeleted, false);
            this.update(toUpdateEntity, updateWrapper);

            //update orderDevice discountPrice
            //orderEntity.getPublishPrice() / orderEntity.getInitialPrice() * unitPrice
            List<OrderDeviceEntity> orderDevices = orderDeviceService.getDeviceList(orderEntity.getOrderId());
            List<OrderDeviceEntity> updatedDevices = orderDevices.stream().map(device -> {
                BigDecimal publishPrice = orderEntity.getPublishPrice();
                BigDecimal initialPrice = orderEntity.getInitialPrice();
                if (device.getUnitPrice() == null) {
                    return device;
                }
                BigDecimal unitPrice = new BigDecimal(device.getUnitPrice());
                // publishPrice / initialPrice * unitPrice
                BigDecimal newDiscountPrice = publishPrice.divide(initialPrice, 10, RoundingMode.HALF_UP)
                        .multiply(unitPrice)
                        .setScale(2, RoundingMode.HALF_UP);

                device.setDiscountPrice(newDiscountPrice.toPlainString());
                return device;
            }).collect(Collectors.toList());
            orderDeviceService.updateBatchById(updatedDevices);
            return R.ok();
        } else {
            return R.failed(SysCode.x00000404.getValue(), "The order information has changed, please refresh");
        }
    }

    @Master
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<?> confirmPaid(Long tenantId, AccountModel accountModel, OrderIdReq orderIdReq, Integer sourceType) {
        OrderEntity orderEntity = getOrder(orderIdReq.getOrderId());
        if (orderEntity == null) {
            return R.failed(SysCode.x00000404.getValue(), "Not found order");
        }

        if (Objects.equals(sourceType, SOURCE_TYPE_FROM_WEB) &&
                orderEntity.getOrderStatus().intValue() != OrderStatus.WaitingPayment.getValue() &&
                orderEntity.getOrderStatus().intValue() != OrderStatus.AwaitingPaymentReceipt.getValue()) {
            return R.failed(SysCode.x00000405.getValue(), "The order status is not `waiting pre payment` or `awaiting payment receipt`");
        }

        // Contract ordering: If the contract start time has not yet arrived, it is not allowed to generate an ath order
        if (orderEntity.getOrderResourcePool().intValue() == OrderResourcePool.BM.getValue()) {
            IContractService contractService = SpringContextHolder.getBean(IContractService.class);
            ContractEntity contractEntity = contractService.getContractByOrderId(orderIdReq.getOrderId());
            if (contractEntity == null) {
                return R.failed(SysCode.x00000404.getValue(), "Not found contract by orderId " + orderIdReq.getOrderId());
            }
            if (contractEntity.getStartedTime().getTime() > System.currentTimeMillis()) {
                update(Wrappers.lambdaUpdate(OrderEntity.class)
                        .set(OrderEntity::getOrderStatus, OrderStatus.NotStarted.getValue())
                        .set(OrderEntity::getLastUpdateTime, new Timestamp(System.currentTimeMillis()))
                        .set(OrderEntity::getLastUpdateAccountId, accountModel.getAccountId())
                        .eq(OrderEntity::getId, orderEntity.getId())
                );
                KvLogger.instance(this)
                        .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                        .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_FINANCE_CONFIRM_PAID_CONTRACT_NOT_START)
                        .p(LogFieldConstants.ACCOUNT_ID, accountModel.getAccountId())
                        .p(LogFieldConstants.ACCOUNT_NAME, accountModel.getAccountName())
                        .p(LogFieldConstants.TENANT_ID, tenantId)
                        .p("OrderTid", orderEntity.getTenantId())
                        .p("ContractId", contractEntity.getContractId())
                        .p("OrderId", orderIdReq.getOrderId())
                        .p("StartTime", contractEntity.getStartedTime())
                        .i();
                return R.ok();
            }
        }

        List<OrderPaymentEntity> prePaymentEntities = orderPaymentService.getPrePaymentOrder(orderIdReq.getOrderId());
        if (prePaymentEntities == null || prePaymentEntities.isEmpty()) {
            return R.failed(SysCode.x00000404.getValue(), "Not found payment");
        }
        List<OrderDeviceEntity> orderDeviceEntityList = orderDeviceService.getDeviceList(orderIdReq.getOrderId());
        if (orderDeviceEntityList.isEmpty()) {
            return R.failed(SysCode.x00000404.getValue(), "Not found order devices");
        }
        List<String> prePaymentOrderIds = prePaymentEntities.stream().map(OrderPaymentEntity::getPaymentOrderId).collect(Collectors.toList());

        if (prePaymentEntities.stream().anyMatch(item -> item.getPaymentStatus() != null && item.getPaymentStatus() !=
                InstalmentPaymentStatus.PAID.getValue().intValue())) {
            // force update order payment to paid
            orderPaymentService.update(Wrappers.lambdaUpdate(OrderPaymentEntity.class)
                    .set(OrderPaymentEntity::getPaymentStatus, InstalmentPaymentStatus.PAID.getValue())
                    .in(OrderPaymentEntity::getPaymentOrderId, prePaymentOrderIds));
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_FINANCE_FORCE_CONFIRM_PAID)
                    .p(LogFieldConstants.ACCOUNT_ID, accountModel.getAccountId())
                    .p(LogFieldConstants.ACCOUNT_NAME, accountModel.getAccountName())
                    .p(LogFieldConstants.TENANT_ID, tenantId)
                    .p("OrderTid", orderEntity.getTenantId())
                    .p("OrderId", orderIdReq.getOrderId())
                    .p("PaymentOrderIds", String.join(StringPool.COMMA, prePaymentOrderIds))
                    .i();
        } else {
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_FINANCE_CONFIRM_PAID)
                    .p(LogFieldConstants.ACCOUNT_ID, accountModel.getAccountId())
                    .p(LogFieldConstants.ACCOUNT_NAME, accountModel.getAccountName())
                    .p(LogFieldConstants.TENANT_ID, tenantId)
                    .p("OrderTid", orderEntity.getTenantId())
                    .p("OrderId", orderIdReq.getOrderId())
                    .p("PaymentOrderIds", String.join(StringPool.COMMA, prePaymentOrderIds))
                    .i();
        }
//        if (firstPaymentEntity.getPaymentStatus().intValue() != InstalmentPaymentStatus.PAID.getValue()) {
//            return R.failed(SysCode.x00000411.getValue(), SysCode.x00000411.getMsg());
//        }

        boolean isOrderVirtualPayment = orderVirtualPaymentService.lambdaQuery()
                .eq(OrderVirtualPaymentEntity::getOrderId, orderEntity.getOrderId())
                .eq(OrderVirtualPaymentEntity::getStatus, InstalmentPaymentStatus.PAID.getValue())
                .exists();

        // create hire purchase schedule
        if (StringUtil.isBlank(orderEntity.getCustomerId()) && !isOrderVirtualPayment) {
            Customer customer;
            R<TenantInfoResp> tenantInfoRespR = pmsRemoteTenantService.getTenantInfo(orderEntity.getTenantId());
            if (tenantInfoRespR == null || tenantInfoRespR.getCode() != R.ok().getCode()) {
                KvLogger.instance(this)
                        .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                        .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_FINANCE_CONFIRM_PAID)
                        .p(LogFieldConstants.TENANT_ID, orderEntity.getTenantId())
                        .p(LogFieldConstants.ERR_MSG, "Get TenantInfo Failed")
                        .p("OrderId", orderEntity.getOrderId())
                        .e();
                return R.failed(SysCode.x00000453.getValue(), SysCode.x00000453.getMsg());
            }
            try {
                customer = stripePaymentService.getCustomer(tenantInfoRespR.getData().getName(), tenantInfoRespR.getData().getEmail());
                if (customer == null) {
                    customer = stripePaymentService.createCustomer(accountModel.getOwnerAccountName(), accountModel.getOwnerAccountEmail());
                }
            } catch (Exception ex) {
                KvLogger.instance(this)
                        .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                        .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_FINANCE_CONFIRM_PAID)
                        .p(LogFieldConstants.TENANT_ID, orderEntity.getTenantId())
                        .p("TenantName", tenantInfoRespR.getData().getName())
                        .p("TenantEmail", tenantInfoRespR.getData().getEmail())
                        .p(LogFieldConstants.ERR_MSG, "Create Stripe Customer Failed")
                        .p("OrderId", orderEntity.getOrderId())
                        .e(ex);
                return R.failed(SysCode.x00000453.getValue(), SysCode.x00000453.getMsg());
            }
            if (customer == null) {
                KvLogger.instance(this)
                        .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                        .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_FINANCE_CONFIRM_PAID)
                        .p(LogFieldConstants.TENANT_ID, orderEntity.getTenantId())
                        .p(LogFieldConstants.ERR_MSG, "Get TenantInfo Failed")
                        .p("OrderId", orderEntity.getOrderId())
                        .e();
                return R.failed(SysCode.x00000453.getValue(), SysCode.x00000453.getMsg());
            }
            orderEntity.setCustomerId(customer.getId());
            update(Wrappers.lambdaUpdate(OrderEntity.class)
                    .set(OrderEntity::getCustomerId, customer.getId())
                    .eq(OrderEntity::getId, orderEntity.getId())
            );
        }
        if (StringUtil.isBlank(orderEntity.getSubscriptionId()) &&
                orderEntity.getInstalmentMonthTotal() != null && orderEntity.getInstalmentMonthTotal() > 0) {
            if (!orderPaymentService.startHirePurchaseSchedule(orderEntity, isOrderVirtualPayment)) {
                return R.failed(SysCode.x00000433.getValue(), SysCode.x00000433.getMsg());
            }
        }
        // start create ath order
        R<?> athOrderR = athOrderPaymentService.athSubscribe(accountModel, orderEntity.getTenantName(), orderEntity, prePaymentEntities.get(0), orderDeviceEntityList);
        // update order status to in service
        if (athOrderR.getCode() == R.ok().getCode()) {
            Timestamp serviceBeginTime = new Timestamp(System.currentTimeMillis());
            Timestamp serviceEndTime = getServiceEndTime(serviceBeginTime, orderEntity.getServiceDuration(), orderEntity.getServiceDurationPeriod());

            this.update(Wrappers.lambdaUpdate(OrderEntity.class)
                    .set(OrderEntity::getOrderStatus, OrderStatus.InService.getValue())
                    .set(OrderEntity::getLastUpdateTime, serviceBeginTime)
                    .set(OrderEntity::getLastUpdateAccountId, accountModel.getAccountId())
                    .eq(OrderEntity::getId, orderEntity.getId())
            );
            // add service record
            subscribedServiceService.saveSubscribedRecord(SubscribedServiceEntity.builder()
                    .orderId(orderEntity.getOrderId())
                    .paymentOrderId(prePaymentEntities.get(0).getPaymentOrderId())
                    .athOrderId(athOrderR.getData().toString())
                    .tenantId(orderEntity.getTenantId())
                    .accountId(orderEntity.getAccountId())
                    .serviceBeginTime(serviceBeginTime)
                    .serviceEndTime(serviceEndTime).build()
            );
            // update payment period
            orderPaymentService.updatePaymentPeriod(orderEntity);

            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_FINANCE_CONFIRM_PAID)
                    .p(LogFieldConstants.ACCOUNT_ID, accountModel.getAccountId())
                    .p(LogFieldConstants.ACCOUNT_NAME, accountModel.getAccountName())
                    .p(LogFieldConstants.TENANT_ID, tenantId)
                    .p("OrderTid", orderEntity.getTenantId())
                    .p("OrderId", orderIdReq.getOrderId())
                    .p("PaymentOrderIds", String.join(StringPool.COMMA, prePaymentOrderIds))
                    .p("OrderStatus", OrderStatus.InService.getName()).i();
        } else {
            this.update(null, Wrappers.lambdaUpdate(OrderEntity.class)
                    .set(OrderEntity::getOrderStatus, OrderStatus.Failed.getValue())
                    .set(OrderEntity::getFailureReason, JSON.toJSONString(athOrderR))
                    .set(OrderEntity::getLastUpdateTime, new Timestamp(System.currentTimeMillis()))
                    .set(OrderEntity::getLastUpdateAccountId, accountModel.getAccountId())
                    .eq(OrderEntity::getId, orderEntity.getId())
            );
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_FINANCE_CONFIRM_PAID)
                    .p(LogFieldConstants.ACCOUNT_ID, accountModel.getAccountId())
                    .p(LogFieldConstants.ACCOUNT_NAME, accountModel.getAccountName())
                    .p(LogFieldConstants.TENANT_ID, tenantId)
                    .p("OrderTid", orderEntity.getTenantId())
                    .p("OrderId", orderIdReq.getOrderId())
                    .p("OrderStatus", OrderStatus.Failed.getName())
                    .p(LogFieldConstants.ERR_CODE, athOrderR.getCode())
                    .p(LogFieldConstants.ERR_MSG, athOrderR.getMsg())
                    .i();
            // 取消订阅计划
            if (!isOrderVirtualPayment && StringUtil.isNotBlank(orderEntity.getScheduleId())){
                KvLogger kvLogger = KvLogger.instance(this)
                        .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                        .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_PURCHASE_SCHEDULE_CANCEL)
                        .p(LogFieldConstants.ACCOUNT_ID, accountModel.getAccountId())
                        .p(LogFieldConstants.ACCOUNT_NAME, accountModel.getAccountName())
                        .p(LogFieldConstants.TENANT_ID, tenantId)
                        .p("OrderTid", orderEntity.getTenantId())
                        .p("OrderId", orderIdReq.getOrderId())
                        .p("ScheduleId", orderEntity.getScheduleId())
                        .p("OrderStatus", OrderStatus.Failed.getName());
                try {
                    stripePaymentService.cancelSubscriptionSchedule(orderEntity.getScheduleId());
                    kvLogger.i();
                } catch (StripeException e) {
                    kvLogger.e(e);
                }
            }
            return R.failed(athOrderR.getCode(), "Order failed");
        }
        return athOrderR;
    }

    @Master
    @Override
    public R<?> delete(Long tenantId, AccountModel accountModel, OrderIdReq orderIdReq) {
        OrderEntity orderEntity = getOrder(orderIdReq.getOrderId());
        if (orderEntity == null) {
            return R.failed(SysCode.x00000404.getValue(), "Not found order");
        }
        if (orderEntity.getOrderStatus().intValue() != OrderStatus.PendingReview.getValue()) {
            return R.failed(SysCode.x00000405.getValue(), "The order status is not pending review");
        }
        this.update(Wrappers.lambdaUpdate(OrderEntity.class)
                .set(OrderEntity::getOrderStatus, OrderStatus.Cancel.getValue())
                .set(OrderEntity::getLastUpdateTime, new Timestamp(System.currentTimeMillis()))
                .set(OrderEntity::getLastUpdateAccountId, accountModel.getAccountId())
                .set(OrderEntity::getFailureReason, "byHand")
                .set(OrderEntity::getDeleted, true)
                .eq(OrderEntity::getOrderId, orderIdReq.getOrderId())
        );
        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_DELETE)
                .p(LogFieldConstants.ACCOUNT_ID, accountModel.getAccountId())
                .p(LogFieldConstants.ACCOUNT_NAME, accountModel.getAccountName())
                .p(LogFieldConstants.TENANT_ID, tenantId)
                .p("OrderId", orderIdReq.getOrderId())
                .p("OrderStatus", OrderStatus.Cancel.getValue())
                .i();

        return R.ok();
    }

    @Master
    @Override
    @Transactional
    public R<?> terminate(Long tenantId, AccountModel accountModel, OrderIdReq orderIdReq) {
        OrderEntity orderEntity = getOrder(orderIdReq.getOrderId());
        if (orderEntity == null) {
            return R.failed(SysCode.x00000404.getValue(), "Not found order");
        }
        if (orderEntity.getOrderStatus().intValue() == OrderStatus.Terminated.getValue()) {
            return R.failed(SysCode.x00000405.getValue(), "The order status is already terminated");
        }
        if (orderEntity.getOrderStatus().intValue() == OrderStatus.PendingReview.getValue() ||
                orderEntity.getOrderStatus().intValue() == OrderStatus.WaitingPayment.getValue() ||
                orderEntity.getOrderStatus().intValue() == OrderStatus.AwaitingPaymentReceipt.getValue() ||
                orderEntity.getOrderStatus().intValue() == OrderStatus.NotStarted.getValue() ||
                orderEntity.getOrderStatus().intValue() == OrderStatus.InService.getValue()) {
            this.update(Wrappers.lambdaUpdate(OrderEntity.class)
                    .set(OrderEntity::getOrderStatus, OrderStatus.Terminated.getValue())
                    .set(OrderEntity::getLastUpdateTime, new Timestamp(System.currentTimeMillis()))
                    .set(OrderEntity::getLastUpdateAccountId, accountModel.getAccountId())
                    .eq(OrderEntity::getOrderId, orderIdReq.getOrderId())
            );
            if (orderEntity.getOrderStatus().intValue() == OrderStatus.InService.getValue()) {
                // 关闭订单
                R<?> closeOrderR = athOrderInfoService.closeOrder(accountModel, orderEntity);
                if (closeOrderR.getCode() != R.ok().getCode()) {
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    return R.failed(SysCode.x00000432.getValue(), closeOrderR.getMsg());
                }
            }
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_TERMINATE)
                    .p(LogFieldConstants.ACCOUNT_ID, accountModel.getAccountId())
                    .p(LogFieldConstants.ACCOUNT_NAME, accountModel.getAccountName())
                    .p(LogFieldConstants.TENANT_ID, tenantId)
                    .p("OrderId", orderIdReq.getOrderId())
                    .p("OrderStatus", OrderStatus.Terminated.getName())
                    .i();
            // update payment valid flag
            orderPaymentService.updateOrderPaymentValidFlag(orderEntity.getOrderId());

            // virtual Pay
            boolean isOrderVirtualPayment = orderVirtualPaymentService.lambdaQuery()
                    .eq(OrderVirtualPaymentEntity::getOrderId, orderEntity.getOrderId())
                    .eq(OrderVirtualPaymentEntity::getStatus, InstalmentPaymentStatus.PAID.getValue())
                    .exists();
            // stripe close 订阅计划或者订阅
            if (!isOrderVirtualPayment){
                try {
                    KvLogger.instance(this)
                            .p(LogFieldConstants.EVENT, Web2LoggerEvents.ATH_ORDER_EVENT)
                            .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ATH_ORDER_EVENT_ACTION_CANCEL_SUBSCRIPTION_SCHEDULE)
                            .p(LogFieldConstants.ACCOUNT_ID, orderEntity.getAccountId())
                            .p(LogFieldConstants.ACCOUNT_NAME, orderEntity.getAccountName())
                            .p(LogFieldConstants.TENANT_ID, orderEntity.getTenantId())
                            .p("ScheduleId", orderEntity.getScheduleId())
                            .i();
                    // stripe cancelSubscriptionSchedule
                    if (StringUtil.isNotBlank(orderEntity.getScheduleId())) {
                        stripePaymentService.cancelSubscriptionSchedule(orderEntity.getScheduleId());
                    }
                } catch (Exception ex) {
                    KvLogger.instance(this)
                            .p(LogFieldConstants.EVENT, Web2LoggerEvents.ATH_ORDER_EVENT)
                            .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ATH_ORDER_EVENT_ACTION_CANCEL_SUBSCRIPTION_SCHEDULE)
                            .p(LogFieldConstants.ACCOUNT_ID, orderEntity.getAccountId())
                            .p(LogFieldConstants.ACCOUNT_NAME, orderEntity.getAccountName())
                            .p(LogFieldConstants.TENANT_ID, orderEntity.getTenantId())
                            .p("ScheduleId", orderEntity.getScheduleId())
                            .e(ex);
                }
                try {
                    KvLogger.instance(this)
                            .p(LogFieldConstants.EVENT, Web2LoggerEvents.ATH_ORDER_EVENT)
                            .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ATH_ORDER_EVENT_ACTION_CANCEL_SUBSCRIPTION)
                            .p(LogFieldConstants.ACCOUNT_ID, orderEntity.getAccountId())
                            .p(LogFieldConstants.ACCOUNT_NAME, orderEntity.getAccountName())
                            .p(LogFieldConstants.TENANT_ID, orderEntity.getTenantId())
                            .p("SubscriptionId", orderEntity.getSubscriptionId())
                            .i();
                    // stripe cancelSubscription
                    if (StringUtil.isNotBlank(orderEntity.getSubscriptionId())) {
                        stripePaymentService.cancelSubscription(orderEntity.getScheduleId());
                    }
                } catch (Exception ex) {
                    KvLogger.instance(this)
                            .p(LogFieldConstants.EVENT, Web2LoggerEvents.ATH_ORDER_EVENT)
                            .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ATH_ORDER_EVENT_ACTION_CANCEL_SUBSCRIPTION)
                            .p(LogFieldConstants.ACCOUNT_ID, orderEntity.getAccountId())
                            .p(LogFieldConstants.ACCOUNT_NAME, orderEntity.getAccountName())
                            .p(LogFieldConstants.TENANT_ID, orderEntity.getTenantId())
                            .p("SubscriptionId", orderEntity.getSubscriptionId())
                            .e(ex);
                }
            }
            return R.ok();
        } else {
            return R.failed(SysCode.x00000405.getValue(), "The order status is end or failed or cancel");
        }
    }

    /**
     * 获取服务结束时间
     *
     * @param serviceBeginTime      服务开始时间
     * @param serviceDuration       服务时长
     * @param serviceDurationPeriod 服务时长单位
     * @return newServiceEndTime
     */
    private Timestamp getServiceEndTime(Timestamp serviceBeginTime, Integer serviceDuration, Integer serviceDurationPeriod) {
        int totalHours = ServiceDurationCalculate.calculate(serviceDuration, serviceDurationPeriod);
        return new Timestamp(DateUtils.addHours(new Date(serviceBeginTime.getTime()), totalHours).getTime());
    }

    @Override
    public Map<Long, Integer> getAccountOrderNum(List<Long> tids) {
        QueryWrapper<OrderEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("tenant_id as tenantId ,count(*) as count");
        queryWrapper.lambda().in(OrderEntity::getTenantId, tids);
        queryWrapper.lambda().groupBy(OrderEntity::getTenantId);
        queryWrapper.lambda().eq(OrderEntity::getDeleted, false);
        List<Map<String, Object>> result = this.baseMapper.selectMaps(queryWrapper);
        Map<Long, Integer> resultMap = result.stream().collect(Collectors.toMap(
                m -> Long.parseLong(m.get("tenantId").toString()), m -> Integer.parseInt(m.get("count").toString())));
        return resultMap;
    }

    @Override
    public R<?> containerOrderDeploySpec(Long tid, String qSpec, String targetVersion, Long appId) {
        return R.ok(pmsRemoteContainerOrderService.getContainerOrderDeployStrategy(tid, targetVersion, qSpec, appId).getData());
    }

    @Override
    public String createOrderFromContract(ContractEntity contractEntity, List<ContractDeviceEntity> contractDeviceEntities,
                                          List<ContractPaymentEntity> contractPaymentEntities) {
        contractPaymentEntities = contractPaymentEntities.stream().sorted(Comparator.comparing(ContractPaymentEntity::getInstalmentMonth)).collect(Collectors.toList());
        ContractPaymentEntity prePayment = contractPaymentEntities.stream().filter(ContractPaymentEntity::getPrePayment).findFirst().get();
        // get account has bound payment method
        CustomerPaymentMethodEntity customerPaymentMethod = customerPaymentMethodService.getSimple(contractEntity.getTenantId());
        // create order
        OrderEntity orderEntity = OrderEntity.builder()
                .orderId(OrderIdGenerate.generateMainOrderId(Web2ApiConstants.ORDER_ID_MAIN_SOURCE_TYPE))
                .initialPrice(contractEntity.getAmount())
                .serviceDuration(contractEntity.getServiceDuration())
                .serviceDurationPeriod(contractEntity.getServiceDurationPeriod())
                .autoRenew(false)
                .instalmentMonthTotal(contractPaymentEntities.get(0).getInstalmentMonthTotal())
                .orderStatus(OrderStatus.WaitingPayment.getValue())
                .paymentStatus(OrderPaymentStatus.None.getValue())
                .customerId(customerPaymentMethod != null ? customerPaymentMethod.getCustomerId() : "")
                .redirectUrl("")
                .accountId(contractEntity.getTenantId())
                .accountName(contractEntity.getTenantName())
                .bdAccountId(contractEntity.getBdAccountId())
                .bdAccountName(contractEntity.getBdAccountName())
                .tenantId(contractEntity.getTenantId())
                .tenantName(contractEntity.getTenantName())
                .createTime(new Timestamp(System.currentTimeMillis()))
                .orderResourcePool(OrderResourcePool.BM.getValue())
                .currentPrice(contractEntity.getAmount())
                .publishPrice(contractEntity.getAmount())
                .prePaymentPrice(prePayment.getHpPrice())
                .instalmentMonthTotal(prePayment.getInstalmentMonthTotal())
                .instalmentMonthPaymentAvg(contractEntity.getAvgAmount())
                .deleted(false)
                .build();
        // create order device
        List<OrderDeviceEntity> orderDeviceEntityList = Lists.newArrayList();
        for (ContractDeviceEntity contractDevice : contractDeviceEntities) {
            if (StringUtil.isBlank(contractDevice.getSpec())) {
                KvLogger.instance(this)
                        .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                        .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_GET_SPECNAME)
                        .p(LogFieldConstants.ACCOUNT_ID, orderEntity.getAccountId())
                        .p(LogFieldConstants.ACCOUNT_NAME, orderEntity.getAccountName())
                        .p(LogFieldConstants.TENANT_ID, orderEntity.getTenantId())
                        .p(LogFieldConstants.ERR_MSG, "Contract Spec is null")
                        .p("ContractId", contractEntity.getContractId())
                        .i();
                return null;
            }
            Web3BasicDataService.SpecCacheInfo specCacheInfo = web3BasicDataService.getSpec(contractDevice.getSpec());
            if (specCacheInfo == null) {
                KvLogger.instance(this)
                        .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                        .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_GET_SPECNAME)
                        .p(LogFieldConstants.ACCOUNT_ID, orderEntity.getAccountId())
                        .p(LogFieldConstants.ACCOUNT_NAME, orderEntity.getAccountName())
                        .p(LogFieldConstants.TENANT_ID, orderEntity.getTenantId())
                        .p("Spec", contractDevice.getSpec())
                        .i();
                return null;
            }
            OrderDeviceEntity orderDeviceEntity = OrderDeviceEntity.builder()
                    .orderId(orderEntity.getOrderId())
                    .regionCode(contractDevice.getRegionCode())
                    .regionName(contractDevice.getRegionName())
                    .spec(contractDevice.getSpec())
                    .specName(specCacheInfo.getSpecName())
                    .resourcePool("BM")
                    .gpuInfo(contractDevice.getGpuInfo())
                    .cpuInfo(contractDevice.getCpuInfo())
                    .mem(contractDevice.getMem())
                    .disk(contractDevice.getDisk())
                    .unitPrice(contractDevice.getDiscountPrice())
                    .discountPrice(contractDevice.getDiscountPrice())
                    .quantity(contractDevice.getQuantity())
                    .deployRegionCode(contractDevice.getDeployRegionCode())
                    .deviceInfo(contractDevice.getDeviceInfo())
                    .createTime(new Timestamp(System.currentTimeMillis()))
                    .build();
            orderDeviceEntityList.add(orderDeviceEntity);
        }
        baseMapper.insert(orderEntity);
        orderDeviceService.saveBatch(orderDeviceEntityList);
        // create order payment
        List<OrderPaymentEntity> orderPaymentEntityList = Lists.newArrayList();
        for (ContractPaymentEntity contractPayment : contractPaymentEntities) {
            orderPaymentEntityList.add(OrderPaymentEntity.builder()
                    .orderId(orderEntity.getOrderId())
                    .paymentOrderId(OrderIdGenerate.generateMainOrderId(Web2ApiConstants.ORDER_ID_PAYMENT_SOURCE_TYPE))
                    .instalmentMonth(contractPayment.getInstalmentMonth())
                    .instalmentMonthTotal(contractPayment.getInstalmentMonthTotal())
                    .hpPrice(contractPayment.getHpPrice())
                    .hpPrePaymentPrice(contractPayment.getHpPrePaymentPrice())
                    .paymentStatus(InstalmentPaymentStatus.None.getValue())
                    .payLink("")
                    .payId("")
                    .prePayment(contractPayment.getPrePayment())
                    .accountId(contractEntity.getTenantId())
                    .tenantId(contractEntity.getTenantId())
                    .createTime(new Timestamp(System.currentTimeMillis()))
                    .build());
        }
        long startDate = System.currentTimeMillis();
        // pre payment
        List<OrderPaymentEntity> prePaymentEntities = orderPaymentEntityList.stream().filter(item -> item.getInstalmentMonth() == 0 ||
                item.getInstalmentMonth() == 1).collect(Collectors.toList());
        for (OrderPaymentEntity orderPaymentEntity : prePaymentEntities) {
            orderPaymentEntity.setPlanPayDate(DateUtil.format(new Date(startDate), DatePattern.SIMPLE_MONTH_PATTERN));
            orderPaymentEntity.setDueDate(new Timestamp(startDate));
            orderPaymentEntity.setLastUpdateTime(new Timestamp(System.currentTimeMillis()));
        }
        orderPaymentService.saveBatch(orderPaymentEntityList);
        return orderEntity.getOrderId();
    }

    @Override
    @Slave
    public R<List<DueDateOrderListResp>> dueOrderList(Long tenantId, AccountModel accountModel) {
        List<OrderPaymentEntity> dueDatePaymentOrderList = orderPaymentService.list(Wrappers.lambdaQuery(OrderPaymentEntity.class)
                .eq(OrderPaymentEntity::getTenantId, tenantId)
                .and(query -> query.eq(OrderPaymentEntity::getPaymentStatus, InstalmentPaymentStatus.None.getValue())
                        .or().eq(OrderPaymentEntity::getPaymentStatus, InstalmentPaymentStatus.FAILED.getValue()))
                .le(OrderPaymentEntity::getDueDate, new Timestamp(System.currentTimeMillis()))
                .eq(OrderPaymentEntity::getValidFlag, ValidFlagStatus.Effective.getValue())
                .orderByAsc(OrderPaymentEntity::getId)
        );

        if (dueDatePaymentOrderList == null || dueDatePaymentOrderList.isEmpty()) {
            return R.ok();
        }
        Map<String, List<OrderPaymentEntity>> dueDateOrderGroups = dueDatePaymentOrderList.stream().collect(Collectors.groupingBy(OrderPaymentEntity::getOrderId));

        List<DueDateOrderListResp> respList = Lists.newArrayList();
        for (String orderId : dueDateOrderGroups.keySet()) {
            DueDateOrderListResp resp = new DueDateOrderListResp();
            resp.setOrderId(orderId);

            List<DueDateOrderListResp.DueDateOrder> dueDateOrders = Lists.newArrayList();
            for (OrderPaymentEntity dueOrderPayment : dueDateOrderGroups.get(orderId)) {
                DueDateOrderListResp.DueDateOrder dueDateOrder = new DueDateOrderListResp.DueDateOrder();
                dueDateOrder.setOrderPaymentId(dueOrderPayment.getPaymentOrderId());
                dueDateOrder.setAmount(dueOrderPayment.getHpPrice().toString());
                String billingType;
                if (dueOrderPayment.getInstalmentMonth() == 0) {
                    billingType = "PREPAYMENT";
                } else {
                    billingType = String.format("MONTHLY(%s/%s)", dueOrderPayment.getInstalmentMonth(), dueOrderPayment.getInstalmentMonthTotal());
                }
                dueDateOrder.setBillingType(billingType);
                dueDateOrder.setDueDate(DateUtil.format(dueOrderPayment.getDueDate(), DatePattern.NORM_DATETIME_PATTERN));

                String periodStart = "";
                if (dueOrderPayment.getPeriodStart() != null) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(dueOrderPayment.getPeriodStart());
                    periodStart = LocalDate.of(
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH) + 1,
                            calendar.get(Calendar.DAY_OF_MONTH)).format(usDateTimeFormat);
                }
                String periodEnd = "";
                if (dueOrderPayment.getPeriodEnd() != null) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(dueOrderPayment.getPeriodEnd());
                    periodEnd = LocalDate.of(
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH) + 1,
                            calendar.get(Calendar.DAY_OF_MONTH)).format(usDateTimeFormat);
                }
                dueDateOrder.setPeriod(periodStart + " - " + periodEnd);
                dueDateOrders.add(dueDateOrder);
            }
            resp.setDueDateList(dueDateOrders);
            respList.add(resp);
        }
        return R.ok(respList);
    }

    @Override
    @Master
    public R<OrderPayResp> duePayOrder(Long tenantId, AccountModel accountModel, List<DueOrderPayReq> dueOrderPayReq) {
        BigDecimal hpTotalPrice = new BigDecimal("0.00");
        try {
            CustomerPaymentMethodEntity customerPaymentMethod = customerPaymentMethodService.getSimple(tenantId);
            if (customerPaymentMethod == null) {
                return R.failed(SysCode.x00000407.getValue(), SysCode.x00000407.getMsg());
            }
            List<OrderEntity> orderEntities = list(Wrappers.lambdaQuery(OrderEntity.class)
                    .notIn(OrderEntity::getOrderStatus, Lists.newArrayList(
                            OrderStatus.InService.getValue(),
                            OrderStatus.End.getValue(),
                            OrderStatus.Failed.getValue(),
                            OrderStatus.Terminated.getValue(),
                            OrderStatus.Cancel.getValue()
                    )).in(OrderEntity::getOrderId, dueOrderPayReq.stream().map(DueOrderPayReq::getOrderId).collect(Collectors.toList())));
            if (orderEntities == null || orderEntities.isEmpty() || orderEntities.size() != dueOrderPayReq.size()) {
                return R.failed(SysCode.x00000450.getValue(), SysCode.x00000450.getMsg());
            }
            List<String> duePaymentOrderIds = dueOrderPayReq.stream().map(DueOrderPayReq::getPaymentOrderIds).flatMap(Collection::stream).collect(Collectors.toList());
            List<OrderPaymentEntity> orderPaymentEntities = orderPaymentService.list(Wrappers.lambdaQuery(OrderPaymentEntity.class)
                    .ne(OrderPaymentEntity::getPaymentStatus, InstalmentPaymentStatus.PAID.getValue())
                    .in(OrderPaymentEntity::getPaymentOrderId, duePaymentOrderIds));

            // 支付id是否有效
            List<String> payIds = orderPaymentEntities.stream().filter(item -> StringUtil.isNotBlank(item.getPayId()) && item.getPaymentStatus() ==
                    InstalmentPaymentStatus.None.getValue().intValue()).map(OrderPaymentEntity::getPayId).distinct().collect(Collectors.toList());
            if (!payIds.isEmpty()) {
                if (payIds.size() > 1) {
                    return R.failed(SysCode.x00000451.getValue(), SysCode.x00000451.getMsg());
                }
                return R.ok(OrderPayResp.builder().payId(payIds.get(0)).build());
            }
            Map<String, List<OrderPaymentEntity>> orderIdGroups = orderPaymentEntities.stream().collect(Collectors.groupingBy(OrderPaymentEntity::getOrderId));
            // check
            for (DueOrderPayReq orderPayReq : dueOrderPayReq) {
                List<OrderPaymentEntity> paymentEntities = orderIdGroups.get(orderPayReq.getOrderId());
                if (paymentEntities == null || paymentEntities.isEmpty() || paymentEntities.size() != orderPayReq.getPaymentOrderIds().size()) {
                    return R.failed(SysCode.x00000450.getValue(), SysCode.x00000450.getMsg());
                }
            }
            // new user first payment update order customerId
            if (orderEntities.stream().anyMatch(item -> StringUtil.isBlank(item.getCustomerId()))) {
                update(Wrappers.lambdaUpdate(OrderEntity.class)
                        .set(OrderEntity::getCustomerId, customerPaymentMethod.getCustomerId())
                        .in(OrderEntity::getId, orderEntities.stream().map(OrderEntity::getId).collect(Collectors.toList())));
            }
            // sum total price
            for (OrderPaymentEntity orderPaymentEntity : orderPaymentEntities) {
                hpTotalPrice = hpTotalPrice.add(orderPaymentEntity.getHpPrice());
            }
            // build meta data
            List<StripePaymentMetaData.OrderIdInfo> orderIdInfoList = Lists.newArrayList();
            for (OrderEntity orderEntity : orderEntities) {
                StripePaymentMetaData.OrderIdInfo orderIdInfo = new StripePaymentMetaData.OrderIdInfo();
                orderIdInfo.setOid(orderEntity.getId());
                orderIdInfo.setPIds(orderPaymentEntities.stream().filter(item -> item.getOrderId().equalsIgnoreCase(orderEntity.getOrderId()))
                        .map(OrderPaymentEntity::getId).collect(Collectors.toList()));
                orderIdInfoList.add(orderIdInfo);
            }

            StripePaymentMetaData metaData = new StripePaymentMetaData();
            metaData.setMetaData(orderIdInfoList);

            // create order PaymentIntent
            PaymentIntent paymentIntent = stripePaymentService.createPaymentIntent(
                    StripePaymentService.CreatePaymentIntentParams.builder()
                            .metaData(metaData)
                            .paymentMethodId(customerPaymentMethod.getPaymentMethodId())
                            .customerId(customerPaymentMethod.getCustomerId())
                            .hpPrice(hpTotalPrice)
                            .build());
            String payId = paymentIntent.getClientSecret();

            OrderPaymentEntity toUpdateEntity = OrderPaymentEntity.builder()
                    .payId(payId)
                    .payLinkExpireAt(new Timestamp(DateUtils.addHours(new Date(), 12).getTime()))
                    .lastUpdateTime(new Timestamp(System.currentTimeMillis()))
                    .build();
            boolean isSuccess = orderPaymentService.update(toUpdateEntity,
                    Wrappers.lambdaUpdate(OrderPaymentEntity.class)
                            .in(OrderPaymentEntity::getPaymentOrderId, duePaymentOrderIds));

            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_DUE_PAY)
                    .p(LogFieldConstants.ACCOUNT_ID, accountModel.getAccountId())
                    .p(LogFieldConstants.ACCOUNT_NAME, accountModel.getAccountName())
                    .p(LogFieldConstants.TENANT_ID, tenantId)
                    .p("Amount", hpTotalPrice)
                    .p("DueOrderInfo", JSON.toJSONString(dueOrderPayReq))
                    .p("PayId", payId)
                    .p(LogFieldConstants.Success, isSuccess)
                    .i();
            if (isSuccess) {
                return R.ok(OrderPayResp.builder().payId(payId).build());
            } else {
                return R.failed(SysCode.x00000404.getValue(), "The payment order information has changed, please refresh");
            }
        } catch (Exception ex) {
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_DUE_PAY)
                    .p(LogFieldConstants.ACCOUNT_ID, accountModel.getAccountId())
                    .p(LogFieldConstants.ACCOUNT_NAME, accountModel.getAccountName())
                    .p(LogFieldConstants.TENANT_ID, tenantId)
                    .p("DueOrderInfo", JSON.toJSONString(dueOrderPayReq))
                    .p("Amount", hpTotalPrice)
                    .e(ex);
            return R.failed(SysCode.x00000806.getValue(), SysCode.x00000806.getMsg());
        }
    }

    @Override
    public R<Long> dueOrderCount(Long tenantId) {
        List<String> waitPaymentOrderIds = this.list(Wrappers.lambdaQuery(OrderEntity.class)
                .eq(OrderEntity::getTenantId, tenantId)
                .eq(OrderEntity::getOrderStatus, OrderStatus.WaitingPayment.getValue())
                .select(OrderEntity::getOrderId)).stream().map(OrderEntity::getOrderId).collect(Collectors.toList());

        Long dueOrderCount = orderPaymentService.count(Wrappers.lambdaQuery(OrderPaymentEntity.class)
                .eq(OrderPaymentEntity::getTenantId, tenantId)
                .and(query -> query.eq(OrderPaymentEntity::getPaymentStatus, InstalmentPaymentStatus.None.getValue())
                        .or().eq(OrderPaymentEntity::getPaymentStatus, InstalmentPaymentStatus.FAILED.getValue()))
                .le(OrderPaymentEntity::getDueDate, new Timestamp(System.currentTimeMillis()))
                .and(query -> query.in(OrderPaymentEntity::getOrderId, waitPaymentOrderIds.isEmpty() ? Lists.newArrayList("1") : waitPaymentOrderIds))
        );
        return R.ok(dueOrderCount);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePaymentRecord(OrderPayVirtualReq orderPayVirtualReq, Boolean success) {
        List<OrderPayVirtualReq.OrderPay> orderPayList = orderPayVirtualReq.getOrderPayList();
        if (success) {
            for (OrderPayVirtualReq.OrderPay orderPay : orderPayList) {
                try {
                    orderVirtualPaymentService.savePaymentRecordSuccess(orderPay);
                } catch (Exception ex) {
                    KvLogger.instance(this)
                            .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                            .p(LogFieldConstants.ACTION, "savePaymentRecordSuccess")
                            .p(LogFieldConstants.ERR_MSG, ex.getMessage())
                            .p("OrderPay", JSON.toJSONString(orderPay))
                            .e(ex);
                }
            }
        } else {
            for (OrderPayVirtualReq.OrderPay orderPay : orderPayList) {
                try {
                    orderVirtualPaymentService.savePaymentRecordFail(orderPay);
                } catch (Exception ex) {
                    KvLogger.instance(this)
                            .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                            .p(LogFieldConstants.ACTION, "savePaymentRecordFail")
                            .p(LogFieldConstants.ERR_MSG, ex.getMessage())
                            .p("OrderPay", JSON.toJSONString(orderPay))
                            .e(ex);
                }
            }
        }

    }

}
