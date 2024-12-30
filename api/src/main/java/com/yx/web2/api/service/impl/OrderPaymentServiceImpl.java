package com.yx.web2.api.service.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapBuilder;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.dynamic.datasource.annotation.Master;
import com.baomidou.dynamic.datasource.annotation.Slave;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.stripe.model.SubscriptionSchedule;
import com.yx.web2.api.common.constant.Web2ApiConstants;
import com.yx.web2.api.common.constant.Web2LoggerEvents;
import com.yx.web2.api.common.enums.InstalmentPaymentStatus;
import com.yx.web2.api.common.enums.ValidFlagStatus;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.common.resp.order.OrderDeviceResp;
import com.yx.web2.api.entity.OrderEntity;
import com.yx.web2.api.entity.OrderPaymentEntity;
import com.yx.web2.api.mapper.OrderPaymentMapper;
import com.yx.web2.api.service.*;
import com.yx.web2.api.service.payment.StripePaymentService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.logger.LogFieldConstants;
import org.yx.lib.utils.util.SpringContextHolder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderPaymentServiceImpl extends ServiceImpl<OrderPaymentMapper, OrderPaymentEntity> implements IOrderPaymentService {
    private final StripePaymentService stripePaymentService;
    private final InstallmentCalculateService installmentCalculateService;

    /**
     * 订单分期
     *
     * @param tenantId     租户Id
     * @param accountModel 当前登录账户信息
     * @param orderEntity  订单信息
     * @param output       分期结算结果
     * @return 分期列表
     */
    @Master
    @Override
    public List<OrderPaymentEntity> prepareOrderInstalmentPlan(Long tenantId, AccountModel accountModel, OrderEntity orderEntity,
                                                               InstallmentCalculateService.InstallmentOutput output) {
//        long userServiceDurationOfMonth = 0;
//        Integer serviceDurationPeriod = orderEntity.getServiceDurationPeriod();
//        switch (Objects.requireNonNull(ServiceDurationPeriod.valueOf(serviceDurationPeriod))) {
//            case Day:
//                userServiceDurationOfMonth = BigDecimal.valueOf(orderEntity.getServiceDuration() * 24.0f).divide(HOURS_OF_MONTH, RoundingMode.UP)
//                        .setScale(0, RoundingMode.UP).longValue();
//                break;
//            case Week:
//                userServiceDurationOfMonth = BigDecimal.valueOf(orderEntity.getServiceDuration() * 7L * 24.0f).divide(HOURS_OF_MONTH, RoundingMode.UP)
//                        .setScale(0, RoundingMode.UP).longValue();
//                break;
//            case Month:
//                userServiceDurationOfMonth = orderEntity.getServiceDuration();
//                break;
//            default: // year
//                userServiceDurationOfMonth = orderEntity.getServiceDuration() * 12;
//                break;
//        }
////        long userServiceDurationOfMonth = orderEntity.getServiceDuration();
////        if (orderEntity.getServiceDurationPeriod().intValue() == ServiceDurationPeriod.Week.getValue()) {
////            if (orderEntity.getServiceDuration() < 4) {
////                userServiceDurationOfMonth = 1;
////            } else {
////                userServiceDurationOfMonth = BigDecimal.valueOf(orderEntity.getServiceDuration() / 4.0f).setScale(0, RoundingMode.UP).longValue();
////            }
////        }
//        // 总价
//        BigDecimal totalPrice = orderEntity.getCurrentPrice();
//        // 首付款
//        BigDecimal firstPaymentPrice = orderEntity.getFirstPaymentPrice();
//        // 分期价格, (订单总价-首付款)/(订购月份-1)
//        BigDecimal averageMonthlyRepaymentPrice = totalPrice.subtract(firstPaymentPrice).divide(
//                BigDecimal.valueOf(userServiceDurationOfMonth == 1L ? 1L : userServiceDurationOfMonth - 1L), RoundingMode.UP).setScale(2, RoundingMode.UP);
//        // 需要分期的总价格
//        BigDecimal instalmentPriceTotal = totalPrice.subtract(firstPaymentPrice);
//        // 分期月份
//        BigDecimal instalmentMonthly = BigDecimal.valueOf(0L);
//        if (averageMonthlyRepaymentPrice.doubleValue() > 0) {
//            instalmentMonthly = instalmentPriceTotal.divide(averageMonthlyRepaymentPrice, 0, RoundingMode.UP);
//        }
//
//        List<OrderPaymentEntity> orderPaymentEntityList = Lists.newArrayList();
//        StringBuilder instalmentLogInfo = new StringBuilder();
//
//        // 首付款
//        orderPaymentEntityList.add(OrderPaymentEntity.builder()
//                .orderId(orderEntity.getOrderId())
//                .paymentOrderId(OrderIdGenerate.generateMainOrderId(Web2ApiConstants.ORDER_ID_PAYMENT_SOURCE_TYPE))
//                .instalmentMonth(0)
//                .instalmentMonthTotal(instalmentMonthly.intValue())
//                .hpPrice(firstPaymentPrice)
//                .paymentStatus(InstalmentPaymentStatus.None.getValue())
//                .payLink("")
//                .payId("")
//                .firstPayment(true)
//                .accountId(accountModel.getAccountId())
//                .tenantId(orderEntity.getTenantId())
//                .createTime(new Timestamp(System.currentTimeMillis()))
//                .build());
//        if (instalmentPriceTotal.doubleValue() > 0) {
//            // 如果用户订购选择的服务时长不足一个月，则分期数是1
//            if (userServiceDurationOfMonth == 1) {
//                orderPaymentEntityList.add(OrderPaymentEntity.builder()
//                        .orderId(orderEntity.getOrderId())
//                        .paymentOrderId(OrderIdGenerate.generateMainOrderId(Web2ApiConstants.ORDER_ID_PAYMENT_SOURCE_TYPE))
//                        .instalmentMonth(1)
//                        .instalmentMonthTotal(1)
//                        .hpPrice(instalmentPriceTotal)
//                        .paymentStatus(InstalmentPaymentStatus.None.getValue())
//                        .payLink("")
//                        .payId("")
//                        .firstPayment(false)
//                        .accountId(accountModel.getAccountId())
//                        .tenantId(orderEntity.getTenantId())
//                        .createTime(new Timestamp(System.currentTimeMillis()))
//                        .build());
//                instalmentLogInfo.append("month: ").append(1).append(", repaymentPrice:").append(instalmentPriceTotal);
//            } else {
//                BigDecimal tempRepaymentPriceSum = new BigDecimal("0").setScale(2, RoundingMode.UP);
//                for (int i = 0; i < instalmentMonthly.intValue(); i++) {
//                    BigDecimal repaymentPrice;
//                    // 由于分期还款除了最后一起都进行了进位操作，所以最后一期还款金额不能按照平均还款金额来算，需要用分期总金额减去前几个月还款的累加金额
//                    if (i == instalmentMonthly.intValue() - 1) {
//                        repaymentPrice = instalmentPriceTotal.subtract(tempRepaymentPriceSum);
//                        instalmentLogInfo.append("month: ").append(i + 1).append(", repaymentPrice:").append(repaymentPrice);
//                    } else {
//                        tempRepaymentPriceSum = tempRepaymentPriceSum.add(averageMonthlyRepaymentPrice);
//                        repaymentPrice = averageMonthlyRepaymentPrice;
//                        instalmentLogInfo.append("month: ").append(i + 1).append(", repaymentPrice:").append(repaymentPrice);
//                    }
//                    orderPaymentEntityList.add(OrderPaymentEntity.builder()
//                            .orderId(orderEntity.getOrderId())
//                            .paymentOrderId(OrderIdGenerate.generateMainOrderId(Web2ApiConstants.ORDER_ID_PAYMENT_SOURCE_TYPE))
//                            .instalmentMonth(i + 1)
//                            .instalmentMonthTotal(instalmentMonthly.intValue())
//                            .hpPrice(repaymentPrice)
//                            .paymentStatus(InstalmentPaymentStatus.None.getValue())
//                            .payLink("")
//                            .payId("")
//                            .firstPayment(false)
//                            .accountId(accountModel.getAccountId())
//                            .tenantId(orderEntity.getTenantId())
//                            .createTime(new Timestamp(System.currentTimeMillis()))
//                            .build());
//                }
//            }
//        }
//        return orderPaymentEntityList;

        List<OrderPaymentEntity> orderPaymentEntityList = Lists.newArrayList();
        for (InstallmentCalculateService.InstallmentOutput.InstallmentOutputDetail detail : output.getOutputDetails()) {
            OrderPaymentEntity orderPaymentEntity = OrderPaymentEntity.builder()
                    .orderId(orderEntity.getOrderId())
                    .paymentOrderId(OrderIdGenerate.generateMainOrderId(Web2ApiConstants.ORDER_ID_PAYMENT_SOURCE_TYPE))
                    .instalmentMonth(detail.getMonth())
                    .instalmentMonthTotal(output.getTotalInstalmentCount())
                    .hpPrice(detail.getHpPrice())
                    .hpPrePaymentPrice(detail.getHpPrePaymentPrice())
                    .paymentStatus(InstalmentPaymentStatus.None.getValue())
                    .payLink("")
                    .payId("")
                    .prePayment(detail.isPrePayment())
                    .accountId(accountModel.getAccountId())
                    .tenantId(orderEntity.getTenantId())
                    .createTime(new Timestamp(System.currentTimeMillis()))
                    .build();
            orderPaymentEntityList.add(orderPaymentEntity);
        }
        // update pay_time
        long startDate = System.currentTimeMillis();
        // pre payment
        List<OrderPaymentEntity> prePaymentEntities = orderPaymentEntityList.stream().filter(item -> item.getInstalmentMonth() == 0 ||
                item.getInstalmentMonth() == 1).collect(Collectors.toList());
        for (OrderPaymentEntity orderPaymentEntity : prePaymentEntities) {
            orderPaymentEntity.setPlanPayDate(DateUtil.format(new Date(startDate), DatePattern.SIMPLE_MONTH_PATTERN));
            orderPaymentEntity.setDueDate(new Timestamp(startDate));
            orderPaymentEntity.setLastUpdateTime(new Timestamp(System.currentTimeMillis()));
        }
        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_PREPARE_ORDER_INSTALMENT_PLAN)
                .p(LogFieldConstants.ACCOUNT_ID, accountModel.getAccountId())
                .p(LogFieldConstants.ACCOUNT_NAME, accountModel.getAccountName())
                .p(LogFieldConstants.TENANT_ID, tenantId)
                .p("OrderId", orderEntity.getOrderId())
                .p("InstallmentInfo", JSON.toJSONString(output))
                .i();
        return orderPaymentEntityList;
    }

    @Slave
    @Override
    public List<OrderPaymentEntity> getPrePaymentOrder(String orderId) {
        return list(Wrappers.lambdaQuery(OrderPaymentEntity.class)
                .eq(OrderPaymentEntity::getPrePayment, true)
                .eq(OrderPaymentEntity::getOrderId, orderId));
    }

    @Slave
    @Override
    public OrderPaymentEntity getOrderPayment(String orderPaymentId) {
        return getOne(Wrappers.lambdaQuery(OrderPaymentEntity.class)
                .eq(OrderPaymentEntity::getPaymentOrderId, orderPaymentId)
                .last("LIMIT 1"));
    }

    @Override
    public List<OrderPaymentEntity> getOrderPayments(List<String> orderPaymentIds) {
        return list(Wrappers.lambdaQuery(OrderPaymentEntity.class)
                .in(OrderPaymentEntity::getPaymentOrderId, orderPaymentIds));
    }

    @Master
    @Override
    public boolean startHirePurchaseSchedule(OrderEntity orderEntity, boolean isOrderVirtualPayment) {
        List<OrderPaymentEntity> orderPaymentEntityList = this.list(Wrappers.lambdaQuery(OrderPaymentEntity.class)
                .eq(OrderPaymentEntity::getOrderId, orderEntity.getOrderId())
                .orderByAsc(OrderPaymentEntity::getInstalmentMonth)
        );
        // monthly payment, dueDate, playPayDate
        long startDate = System.currentTimeMillis();
        List<OrderPaymentEntity> monthlyPaymentEntities = orderPaymentEntityList.stream().filter(item -> item.getInstalmentMonth() > 1).collect(Collectors.toList());
        if (monthlyPaymentEntities.isEmpty()) {
            return true;
        } else {
            for (OrderPaymentEntity orderPaymentEntity : monthlyPaymentEntities) {
                startDate = DateUtils.addMonths(new Date(startDate), 1).getTime();
                orderPaymentEntity.setPlanPayDate(DateUtil.format(new Date(startDate), DatePattern.SIMPLE_MONTH_PATTERN));
                orderPaymentEntity.setDueDate(new Timestamp(startDate));
                orderPaymentEntity.setLastUpdateTime(new Timestamp(System.currentTimeMillis()));
            }
            SpringContextHolder.getBean(IOrderPaymentService.class).updateBatchById(monthlyPaymentEntities);
        }
        if (!monthlyPaymentEntities.isEmpty() && !isOrderVirtualPayment) {
            try {
                List<StripePaymentService.CreateSubscriptionScheduleParams.Phase> phaseList = new ArrayList<>();
                for (OrderPaymentEntity monthlyPayment : monthlyPaymentEntities) {
                    phaseList.add(StripePaymentService.CreateSubscriptionScheduleParams.Phase.builder()
                            .amount(monthlyPayment.getHpPrice().multiply(new BigDecimal(100)).setScale(2, RoundingMode.UP).longValue())
                            .iterations(1L).build());
                }
                SubscriptionSchedule subscriptionSchedule = stripePaymentService.createSubscriptionSchedule(
                        StripePaymentService.CreateSubscriptionScheduleParams.builder()
                                .customerId(orderEntity.getCustomerId())
                                .startDate(DateUtils.addMonths(new Date(System.currentTimeMillis()), 1).getTime() / 1000)
                                .phaseList(phaseList)
                                .metaData(MapBuilder.create(new HashMap<String, String>()).put("orderId", orderEntity.getOrderId()).build())
                                .build());
                orderEntity.setSubscriptionId(subscriptionSchedule.getSubscription());
                orderEntity.setScheduleId(subscriptionSchedule.getId());
                orderEntity.setLastUpdateTime(new Timestamp(System.currentTimeMillis()));

                // update order entity fill subscriptionId
                SpringContextHolder.getBean(IOrderService.class).update(Wrappers.lambdaUpdate(OrderEntity.class)
                        .set(OrderEntity::getSubscriptionId, subscriptionSchedule.getSubscription())
                        .set(OrderEntity::getScheduleId, subscriptionSchedule.getId())
                        .eq(OrderEntity::getId, orderEntity.getId()));
                KvLogger.instance(this)
                        .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                        .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_PURCHASE_SCHEDULE_CREATED)
                        .p(LogFieldConstants.ACCOUNT_ID, orderEntity.getAccountId())
                        .p(LogFieldConstants.ACCOUNT_NAME, orderEntity.getAccountName())
                        .p(LogFieldConstants.TENANT_ID, orderEntity.getTenantId())
                        .p("OrderId", orderEntity.getOrderId())
                        .p("ScheduleId", subscriptionSchedule.getId())
                        .p("SubscriptionId", subscriptionSchedule.getSubscription())
                        .p("PurchaseSchedule", JSON.toJSONString(phaseList))
                        .i();
                return true;
            } catch (Exception ex) {
                KvLogger.instance(this)
                        .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                        .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_PURCHASE_SCHEDULE_CREATED)
                        .p(LogFieldConstants.ACCOUNT_ID, orderEntity.getAccountId())
                        .p(LogFieldConstants.ACCOUNT_NAME, orderEntity.getAccountName())
                        .p(LogFieldConstants.TENANT_ID, orderEntity.getTenantId())
                        .p(LogFieldConstants.ERR_MSG, ex.getMessage())
                        .p("OrderId", orderEntity.getOrderId())
                        .e(ex);
                return false;
            }
        }
        return true;
    }

    @Slave
    @Override
    public OrderPaymentEntity getNotPaymentSubscriptionPayments(String orderId, String planPayDate) {
        LambdaQueryWrapper<OrderPaymentEntity> queryWrapper = Wrappers.lambdaQuery(OrderPaymentEntity.class)
                .eq(OrderPaymentEntity::getOrderId, orderId)
                .eq(OrderPaymentEntity::getPrePayment, false)
                .eq(OrderPaymentEntity::getPlanPayDate, planPayDate)
                .eq(OrderPaymentEntity::getPaymentStatus, InstalmentPaymentStatus.None)
                .orderByAsc(OrderPaymentEntity::getInstalmentMonth)
                .last("LIMIT 1");
        return this.getOne(queryWrapper);
    }

    @Override
    public List<OrderPaymentEntity> getNotPaymentSubscriptionPayments(String orderId) {
        LambdaQueryWrapper<OrderPaymentEntity> queryWrapper = Wrappers.lambdaQuery(OrderPaymentEntity.class)
                .eq(OrderPaymentEntity::getOrderId, orderId)
                .eq(OrderPaymentEntity::getPrePayment, false)
                .eq(OrderPaymentEntity::getPaymentStatus, InstalmentPaymentStatus.None)
                .orderByAsc(OrderPaymentEntity::getInstalmentMonth);
        return this.list(queryWrapper);
    }

    @Override
    public Page<JSONObject> billList(IPage<JSONObject> page, QueryWrapper<OrderPaymentEntity> queryWrapper) {
        return baseMapper.billList(page, queryWrapper);
    }

    @Override
    public Map<String, Integer> getOrderPaymentBySpecRegion(List<String> region, List<String> spec) {
        List<OrderDeviceResp> orderPaymentBySpecRegion = baseMapper.getOrderPaymentBySpecRegion(region, spec);
        return orderPaymentBySpecRegion.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getSpec() + "_" + order.getRegionCode(),
                        Collectors.summingInt(OrderDeviceResp::getQuantity)
                ));
    }

    @Override
    @Master
    @Transactional
    public void updateOrderPaymentValidFlag(String orderId) {
        Timestamp nowTimestamp = new Timestamp(System.currentTimeMillis());
        List<OrderPaymentEntity> orderPayments = list(
                Wrappers.lambdaQuery(OrderPaymentEntity.class)
                        .eq(OrderPaymentEntity::getOrderId, orderId)
                        .eq(OrderPaymentEntity::getValidFlag, ValidFlagStatus.Effective.getValue())
                        .eq(OrderPaymentEntity::getPaymentStatus, InstalmentPaymentStatus.None.getValue())
                        .orderByAsc(OrderPaymentEntity::getDueDate)
        );
        if (orderPayments.isEmpty()) {
            return;
        }

        List<Long> updateIds = getUpdateIds(orderPayments, nowTimestamp);
        if (!updateIds.isEmpty()) {
            update(
                    Wrappers.lambdaUpdate(OrderPaymentEntity.class)
                            .set(OrderPaymentEntity::getValidFlag, ValidFlagStatus.InVain.getValue())
                            .in(OrderPaymentEntity::getId, updateIds)
            );
        }
    }

    private List<Long> getUpdateIds(List<OrderPaymentEntity> orderPayments, Timestamp nowTimestamp) {
        // in case of prepayment update all records
        if (orderPayments.get(0).getPrePayment()) {
            return orderPayments.stream()
                    .map(OrderPaymentEntity::getId)
                    .collect(Collectors.toList());
        }

        List<OrderPaymentEntity> notDuePayments = orderPayments.stream()
                .filter(payment -> payment.getDueDate() != null && payment.getDueDate().after(nowTimestamp))
                .collect(Collectors.toList());

        if (!notDuePayments.isEmpty()) {
            return notDuePayments.stream()
                    .skip(1)
                    .map(OrderPaymentEntity::getId)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    @Override
    @Master
    @Transactional
    public void updatePaymentPeriod(OrderEntity orderEntity) {
        int totalHours = ServiceDurationCalculate.calculate(orderEntity.getServiceDuration(), orderEntity.getServiceDurationPeriod());

        List<OrderPaymentEntity> orderPaymentEntityList = list(Wrappers.lambdaQuery(OrderPaymentEntity.class)
                .eq(OrderPaymentEntity::getOrderId, orderEntity.getOrderId())
        );
        long startDate = System.currentTimeMillis();
        long lastPeriodEndDate;
        int remainHours = 0;
        if (totalHours <= 730) {
            lastPeriodEndDate = DateUtils.addHours(new Date(startDate), totalHours).getTime();
        } else {
            lastPeriodEndDate = DateUtils.addHours(new Date(startDate), 730).getTime();
            remainHours = totalHours - 730;
        }
        // pre payment,
        List<OrderPaymentEntity> prePaymentEntities = orderPaymentEntityList.stream().filter(item -> item.getInstalmentMonth() == 0 ||
                item.getInstalmentMonth() == 1).collect(Collectors.toList());
        for (OrderPaymentEntity orderPaymentEntity : prePaymentEntities) {
            orderPaymentEntity.setPeriodStart(new Timestamp(startDate));
            orderPaymentEntity.setPeriodEnd(new Timestamp(lastPeriodEndDate));
        }
        saveOrUpdateBatch(prePaymentEntities);

        // monthly payment
        List<OrderPaymentEntity> monthlyPaymentEntities = orderPaymentEntityList.stream().filter(item -> item.getInstalmentMonth() > 1).collect(Collectors.toList());
        for (OrderPaymentEntity orderPaymentEntity : monthlyPaymentEntities) {
            orderPaymentEntity.setPeriodStart(new Timestamp(lastPeriodEndDate));
            if (remainHours <= 730) {
                lastPeriodEndDate = DateUtils.addHours(orderPaymentEntity.getPeriodStart(), Math.max(remainHours, 0)).getTime();
            } else {
                lastPeriodEndDate = DateUtils.addHours(orderPaymentEntity.getPeriodStart(), 730).getTime();
                remainHours = remainHours - 730;
            }
            orderPaymentEntity.setPeriodEnd(new Timestamp(lastPeriodEndDate));
        }
        saveOrUpdateBatch(monthlyPaymentEntities);
    }

//    public static void main(String[] args) {
//        BigDecimal totalMonthHours = BigDecimal.valueOf(730);
//        long userServiceDurationOfDays = 1;
//        System.out.println("选择天数:" + userServiceDurationOfDays);
//        System.out.println("换算小时: " + userServiceDurationOfDays + " * 24 :" + userServiceDurationOfDays * 24);
//        System.out.println("除以 730(不进位，单位月): " + BigDecimal.valueOf(userServiceDurationOfDays * 24).divide(totalMonthHours, 4, RoundingMode.HALF_UP).setScale(3, RoundingMode.UP));
//        System.out.println("除以 730(进位，单位月): " + BigDecimal.valueOf(userServiceDurationOfDays * 24).divide(totalMonthHours, 0, RoundingMode.UP).setScale(0, RoundingMode.UP));
//
////        long userServiceDurationOfWeeks = 9;
////        long userServiceDurationOfDays = userServiceDurationOfWeeks * 7;
////        System.out.println("选择周数:" + userServiceDurationOfWeeks);
////        System.out.println("换算小时: " + userServiceDurationOfDays + " * 24 :" + userServiceDurationOfDays * 24);
////        System.out.println("除以 730(不进位，单位月): " + BigDecimal.valueOf(userServiceDurationOfDays * 24).divide(totalMonthHours, 4, RoundingMode.HALF_UP).setScale(3, RoundingMode.UP));
////        System.out.println("除以 730(进位，单位月): " + BigDecimal.valueOf(userServiceDurationOfDays * 24).divide(totalMonthHours, 0, RoundingMode.UP).setScale(0, RoundingMode.UP));
//
////        long userServiceDurationOfYears = 1;
////        long userServiceDurationOfMonth = userServiceDurationOfYears * 12;
////        long userServiceDurationOfDays = BigDecimal.valueOf(userServiceDurationOfMonth).multiply(totalMonthHours)
////                .divide(BigDecimal.valueOf(24), RoundingMode.UP).longValue();
////        System.out.println("选择年数:" + userServiceDurationOfYears);
////        System.out.println("换算小时: " + userServiceDurationOfMonth + " 730 :" + userServiceDurationOfMonth * 730);
////        System.out.println("除以 730(不进位，单位月): " + BigDecimal.valueOf(userServiceDurationOfDays * 24).divide(totalMonthHours, 4, RoundingMode.HALF_UP).setScale(3, RoundingMode.UP));
////        System.out.println("除以 730(进位，单位月): " + BigDecimal.valueOf(userServiceDurationOfDays * 24).divide(totalMonthHours, 0, RoundingMode.UP).setScale(0, RoundingMode.UP));
////        long userServiceDurationOfMonth = 12;
//
////        Day(1),
////        Week(2),
////        Month(3),
////        Year(4);
//        int serviceDurationPeriod = 1;
//        int serviceDuration = 1;
//
//        long userServiceDurationOfMonth = 0;
//        switch (Objects.requireNonNull(ServiceDurationPeriod.valueOf(serviceDurationPeriod))) {
//            case Day:
//                userServiceDurationOfMonth = BigDecimal.valueOf(serviceDuration * 24.0f).divide(HOURS_OF_MONTH, RoundingMode.UP)
//                        .setScale(0, RoundingMode.UP).longValue();
//                break;
//            case Week:
//                userServiceDurationOfMonth = BigDecimal.valueOf(serviceDuration * 7L * 24.0f).divide(HOURS_OF_MONTH, RoundingMode.UP)
//                        .setScale(0, RoundingMode.UP).longValue();
//                break;
//            case Month:
//                userServiceDurationOfMonth = serviceDuration;
//                break;
//            default:
//                userServiceDurationOfMonth = serviceDuration * 12;
//                break;
//        }
////        // 总价
//        BigDecimal totalPrice = new BigDecimal("1.24");
//        // 首付款
//        BigDecimal firstPaymentPrice = new BigDecimal("0.8").setScale(2, RoundingMode.UP);
////        // 总价
////        BigDecimal totalPrice = new BigDecimal("80");
////        // 首付款
////        BigDecimal firstPaymentPrice = new BigDecimal("20").setScale(2, RoundingMode.UP);
//        // 分期价格, (订单总价-首付款)/(订购月份-1)
//        BigDecimal averageMonthlyRepaymentPrice = totalPrice.subtract(firstPaymentPrice).divide(
//                BigDecimal.valueOf(userServiceDurationOfMonth == 1L ? 1L : userServiceDurationOfMonth - 1L), RoundingMode.UP).setScale(2, RoundingMode.UP);
//        // 需要分期的总价格
//        BigDecimal instalmentPriceTotal = totalPrice.subtract(firstPaymentPrice);
//        // 分期月份
//        BigDecimal instalmentMonthly = instalmentPriceTotal.divide(averageMonthlyRepaymentPrice, 0, RoundingMode.UP);
//
//        System.out.println("总价: " + totalPrice);
//        System.out.println("首付款: " + firstPaymentPrice);
//        System.out.println("分期总价格: " + instalmentPriceTotal);
//        System.out.println("分期月份: " + instalmentMonthly);
////        System.out.println("平均每月分期价格: " + averageMonthlyRepaymentPrice);
//
//        BigDecimal tempRepaymentPrice = new BigDecimal("0").setScale(2, RoundingMode.UP);
//        for (int i = 0; i < instalmentMonthly.intValue(); i++) {
//            if (i == instalmentMonthly.intValue() - 1) {
//                System.out.println("分期计划-" + (i + 1) + ": " + instalmentPriceTotal.subtract(tempRepaymentPrice));
//            } else {
//                tempRepaymentPrice = tempRepaymentPrice.add(averageMonthlyRepaymentPrice);
//                System.out.println("分期计划-" + (i + 1) + ": " + averageMonthlyRepaymentPrice);
//            }
//        }
//    }


}
