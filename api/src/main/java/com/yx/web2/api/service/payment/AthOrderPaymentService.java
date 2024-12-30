package com.yx.web2.api.service.payment;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.dynamic.datasource.annotation.Master;
import com.google.common.collect.Lists;
import com.yx.pass.remote.feecenter.FeeCenterRemoteOrderService;
import com.yx.pass.remote.feecenter.FeeCenterRemotePriceService;
import com.yx.pass.remote.feecenter.FeeCenterRemoteWalletService;
import com.yx.pass.remote.feecenter.model.req.*;
import com.yx.pass.remote.feecenter.model.resp.OrderCreateResp;
import com.yx.pass.remote.feecenter.model.resp.UsdRateResp;
import com.yx.pass.remote.feecenter.model.resp.WalletTransferResp;
import com.yx.pass.remote.pms.PmsRemoteOrderService;
import com.yx.pass.remote.pms.PmsRemoteSpecService;
import com.yx.pass.remote.pms.model.req.PayOrderReq;
import com.yx.web2.api.common.constant.Web2LoggerEvents;
import com.yx.web2.api.common.enums.*;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.common.req.order.CidsByDeviceInfoReq;
import com.yx.web2.api.config.Web2ApiConfig;
import com.yx.web2.api.entity.*;
import com.yx.web2.api.service.IAthOrderInfoService;
import com.yx.web2.api.service.IOrderContainerService;
import com.yx.web2.api.service.ITenantAthTransferRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.logger.LogFieldConstants;
import org.yx.lib.utils.util.R;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AthOrderPaymentService {

    private final Web2ApiConfig web2ApiConfig;

    private final FeeCenterRemoteOrderService feeCenterRemoteOrderService;
    private final PmsRemoteOrderService pmsCenterOrderService;
    private final FeeCenterRemotePriceService remoteFeeCenterPriceService;
    private final FeeCenterRemoteWalletService remoteFeeCenterWalletService;

    private final ITenantAthTransferRecordService tenantAthTransferRecordService;
    private final IAthOrderInfoService athOrderInfoService;
    private final PmsRemoteSpecService pmsRemoteSpecService;
    private final IOrderContainerService orderContainerService;

    /**
     * ath订阅
     *
     * @param accountModel          账户信息
     * @param orderTenantName       订单租户名称
     * @param orderEntity           Usd订单
     * @param orderPaymentEntity    Usd支付订单
     * @param orderDeviceEntityList Usd订单设备信息列表
     */
    @Master
    @Transactional(rollbackFor = Exception.class)
    public R<?> athSubscribe(AccountModel accountModel,
                             String orderTenantName,
                             OrderEntity orderEntity,
                             OrderPaymentEntity orderPaymentEntity,
                             List<OrderDeviceEntity> orderDeviceEntityList) {
//        // ath usd rate
//        String queryUsdBeginDateStr = DateUtil.formatDate(orderPaymentEntity.getPayFinishTime()) + " 00:00:00";
//        String queryUsdEndDateStr = DateUtil.formatDate(orderPaymentEntity.getPayFinishTime()) + " 23:59:59";
//        String matchedUsdRateDateStr = DateUtil.formatDateTime(orderPaymentEntity.getPayFinishTime());
//        BigDecimal usdRate = getUsdRate(DateUtil.parseDateTime(queryUsdBeginDateStr),
//                DateUtil.parseDateTime(queryUsdEndDateStr), DateUtil.parseDateTime(matchedUsdRateDateStr));
//        if (usdRate == null) {
//            kvBaseLogger.p(LogFieldConstants.ERR_MSG, "get usd rate failed")
//                    .e();
//            return R.failed(SysCode.x00000430.getValue(), SysCode.x00000430.getMsg());
//        }

        // ath create order
        R<OrderCreateResp> athOrderCreateRespR = createOrder(accountModel, orderEntity, orderPaymentEntity, orderDeviceEntityList);
        if (athOrderCreateRespR.getCode() != R.ok().getCode()) {
            return athOrderCreateRespR;
        }
        // ath transfer
//        long needTransferAthValue = calculateNeedTransferAthValue(orderEntity.getCurrentPrice(), usdRate);
        String transferAthAmount = new BigDecimal(athOrderCreateRespR.getData().getDailyAth()).multiply(BigDecimal.valueOf(2)).setScale(8, RoundingMode.UP).toString();
        R<WalletTransferResp> walletTransferRespR = athTransfer(accountModel, orderTenantName, orderEntity, orderPaymentEntity,
                web2ApiConfig.getAthAdminTid(), orderEntity.getTenantId(), athOrderCreateRespR.getData().getOrderCode(),
                transferAthAmount, AthTransferType.In.getValue(), -1, false);
        if (walletTransferRespR.getCode() != R.ok().getCode()) {
            return walletTransferRespR;
        }
        // BM order need bind container
        if (orderEntity.getOrderResourcePool().intValue() == OrderResourcePool.BM.getValue()) {
            // bind container
            List<CidsByDeviceInfoReq.SpecRegionSysInfo> cidsByDeviceInfoReqList = new ArrayList<>();
            Map<Long, OrderDeviceEntity> orderDeviceMap = new HashMap<>();
            Integer totalQuantity = 0;
            for (OrderDeviceEntity entity : orderDeviceEntityList) {
                totalQuantity += entity.getQuantity();
                orderDeviceMap.put(entity.getId(), entity);
                JSONObject deviceInfoObj = JSONObject.parseObject(entity.getDeviceInfo());
                cidsByDeviceInfoReqList.add(getCidsByDeviceInfoReq(deviceInfoObj, entity));
            }
            CidsByDeviceInfoReq cidsByDeviceInfoReq = CidsByDeviceInfoReq.builder().specRegionSysInfos(cidsByDeviceInfoReqList).build();
            JSONObject cidsByDeviceInfoReqJson = JSONObject.parseObject(JSONObject.toJSONString(cidsByDeviceInfoReq));

            R<?> cidsR = pmsRemoteSpecService.getCidsBySpecAndRegion(cidsByDeviceInfoReqJson);
            if (cidsR != null && cidsR.getCode() == R.ok().getCode()) {
                // Extract the "data" array from the response
                JSONArray dataArray = (JSONArray) cidsR.getData();
                Set<Long> uniqueCids = new HashSet<>();
                List<OrderContainerEntity> containerList = new ArrayList<>();
                Integer returnTotalCidNum = 0;
                for (int i = 0; i < dataArray.size(); i++) {
                    JSONObject item = dataArray.getJSONObject(i);
                    String[] cids = item.getString("cids").split(",");
                    // Extract the "orderDeviceId" value
                    Long orderDeviceId = item.getLong("orderDeviceId");
                    OrderDeviceEntity orderDeviceEntity = null;
                    if (orderDeviceId == null || (orderDeviceEntity = orderDeviceMap.get(orderDeviceId)) == null) {
                        // ath transfer rollback
                        R<?> rollBackR = athOrderRollback(accountModel, orderEntity, orderPaymentEntity, athOrderCreateRespR, transferAthAmount,
                                walletTransferRespR.getData().getBillId());
                        if (rollBackR.getCode() != R.ok().getCode()) {
                            return rollBackR;
                        }
                        return R.failed(SysCode.x00000454.getValue(), SysCode.x00000454.getMsg());
                    }
                    // check cids is not empty or null and add to containerList
                    for (String cid : cids) {
                        if (cid == null ||cid.isEmpty()) {
                            continue;
                        }
                        returnTotalCidNum++;
                        Long cidLong = Long.parseLong(cid);
                        containerList.add(OrderContainerEntity.builder()
                                .cid(cidLong)
                                .orderDeviceId(orderDeviceId)
                                .orderId(orderEntity.getOrderId())
                                .orderResourcePool(OrderResourcePool.BM.getValue())
                                .wholesaleTid(orderEntity.getTenantId())
                                .spec(orderDeviceEntity.getSpec())
                                .regionCode(orderDeviceEntity.getRegionCode())
                                .wholesalePrice(orderDeviceEntity.getDiscountPrice())
                                .serviceStartTime(new Timestamp(System.currentTimeMillis()))
                                .status(OrderContainerStatus.INITIALIZE.getValue())
                                .refundStatus(RefundStatus.NOT_REFUNDED.getValue())
                                .build()
                        );
                        uniqueCids.add(cidLong);
                    }
                }
                if (!returnTotalCidNum.equals(totalQuantity)) {
                    // ath transfer rollback
                    R<?> rollBackR = athOrderRollback(accountModel, orderEntity, orderPaymentEntity, athOrderCreateRespR, transferAthAmount,
                            walletTransferRespR.getData().getBillId());
                    if (rollBackR.getCode() != R.ok().getCode()) {
                        return rollBackR;
                    }
                    return R.failed(SysCode.x00000452.getValue(), SysCode.x00000452.getMsg());
                }
                // save batch containerList
                orderContainerService.saveBatch(containerList);
                // bind container
                List<Long> cIds = new ArrayList<>(uniqueCids);
                R<?> bindR = bindContainer(orderEntity.getOrderId(), athOrderCreateRespR.getData().getOrderCode(), cIds);

                // bind failed
                if (bindR.getCode() != R.ok().getCode()) {
                    // ath transfer rollback
                    R<?> rollBackR = athOrderRollback(accountModel, orderEntity, orderPaymentEntity, athOrderCreateRespR, transferAthAmount,
                            walletTransferRespR.getData().getBillId());
                    if (rollBackR.getCode() != R.ok().getCode()) {
                        return rollBackR;
                    }
                    return bindR;
                } else {
                    KvLogger.instance(this)
                            .p(LogFieldConstants.EVENT, Web2LoggerEvents.ATH_ORDER_EVENT)
                            .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ATH_ORDER_EVENT_BIND_CONTAINER)
                            .p(LogFieldConstants.Success, true)
                            .p("UsdOrderId", orderEntity.getOrderId())
                            .p("AthOrderId", athOrderCreateRespR.getData().getOrderCode())
                            .p(LogFieldConstants.TID, orderEntity.getTenantId())
                            .p("CIds", JSONObject.toJSONString(cIds))
                            .i();
                }
            } else {
                R<?> rollBackR = athOrderRollback(accountModel, orderEntity, orderPaymentEntity, athOrderCreateRespR, transferAthAmount,
                        walletTransferRespR.getData().getBillId());
                if (rollBackR.getCode() != R.ok().getCode()) {
                    return rollBackR;
                }
                return cidsR;
            }
        }
        // ath order pay
        R<?> payOrderR = payOrder(accountModel, orderEntity, athOrderCreateRespR.getData().getOrderCode());
        if (payOrderR.getCode() != R.ok().getCode()) {
            // ath transfer rollback
            R<?> rollBackR = athOrderRollback(accountModel, orderEntity, orderPaymentEntity, athOrderCreateRespR, transferAthAmount,
                    walletTransferRespR.getData().getBillId());
            if (rollBackR.getCode() != R.ok().getCode()) {
                return rollBackR;
            }
            return payOrderR;
        } else {
            // add ath order info
            athOrderInfoService.saveAthOrderInfo(AthOrderInfoEntity.builder()
                    .orderId(orderEntity.getOrderId())
                    .paymentOrderId(orderPaymentEntity.getPaymentOrderId())
                    .athOrderId(athOrderCreateRespR.getData().getOrderCode())
                    .athTotal(athOrderCreateRespR.getData().getAthTotal())
                    .dailyAth(athOrderCreateRespR.getData().getDailyAth())
                    .tenantId(orderEntity.getTenantId())
                    .createTime(new Timestamp(System.currentTimeMillis()))
                    .build());
        }
        return R.ok(athOrderCreateRespR.getData().getOrderCode());
    }

    private CidsByDeviceInfoReq.SpecRegionSysInfo getCidsByDeviceInfoReq(JSONObject deviceInfoObj, OrderDeviceEntity entity) {
        return CidsByDeviceInfoReq.SpecRegionSysInfo.builder()
                .gpuCount(getStringOrDefault(deviceInfoObj, "gpuCount"))
                .gpuManufacturer(getStringOrDefault(deviceInfoObj, "gpuManufacturer"))
                .gpuType(getStringOrDefault(deviceInfoObj, "gpuType"))
                .gpuBusType(getStringOrDefault(deviceInfoObj, "gpuBusType"))
                .gpuMem(getStringOrDefault(deviceInfoObj, "gpuMemOriginal"))
                .gpuMemUnit(getStringOrDefault(deviceInfoObj, "gpuMemUnit"))
                .cpuNum(getStringOrDefault(deviceInfoObj, "cpuNum"))
                .cpuManufacturer(getStringOrDefault(deviceInfoObj, "cpuManufacturer"))
                .cpuType(getStringOrDefault(deviceInfoObj, "cpuType"))
                .cpuCores(getStringOrDefault(deviceInfoObj, "cpuCores"))
                .cpuCount(getStringOrDefault(deviceInfoObj, "cpuCount"))
                .cpuSpeed(getStringOrDefault(deviceInfoObj, "cpuSpeed"))
                .cpuSpeedUnit(getStringOrDefault(deviceInfoObj, "cpuSpeedUnit"))
                .osMem(getStringOrDefault(deviceInfoObj, "osMem"))
                .storage(getStringOrDefault(deviceInfoObj, "storageOriginal"))
                .storageUnit(getStringOrDefault(deviceInfoObj, "storageUnit"))
                .nic(getStringOrDefault(deviceInfoObj, "nicOriginal"))
                .nicUnit(getStringOrDefault(deviceInfoObj, "nicUnit"))
                .bandwidthLevel(getStringOrDefault(deviceInfoObj, "bandwidthLevel"))
                .bandwidthUnit(getStringOrDefault(deviceInfoObj, "bandwidthUnit"))
                .spec(getStringOrDefault(deviceInfoObj, "spec"))
                .regionCode(entity.getDeployRegionCode() == null ? "":entity.getDeployRegionCode())
                .quantity(entity.getQuantity() == null ? "":entity.getQuantity().toString())
                .orderDeviceId(entity.getId())
                .resourcePool("BM")
                .build();
    }


    String getStringOrDefault(Map<String, Object> map, String key) {
        if (map == null) {
            return "";
        }
        Object value = map.get(key);
        if (value instanceof String) {
            return (String) value;
        } else if (value != null) {
            return value.toString();
        } else {
            return "";
        }
    }


    /**
     * 查询Usd汇率
     *
     * @param queryBeginDate     查询汇率的开始时间
     * @param queryEndDate       查询汇率的结束时间
     * @param matchedUsdRateDate 匹配汇率的时间
     * @return 汇率
     */
    public BigDecimal getUsdRate(AccountModel accountModel, Date queryBeginDate, Date queryEndDate, Date matchedUsdRateDate) {
        List<UsdRateResp> usdRateRespList = remoteFeeCenterPriceService.queryUsdRate(QueryUsdRateReq.builder()
                .beginDateTime(queryBeginDate)
                .endDateTime(queryEndDate)
                .tid(accountModel.getTenantId())
                .tenantType(accountModel.getTenantType())
                .tenantType(accountModel.getTenantType())
                .build());
        if (usdRateRespList == null || usdRateRespList.isEmpty()) {
            return null;
        }
        UsdRateResp matchedUsdRate = null;
        for (UsdRateResp usdRateItem : usdRateRespList) {
            long usdRateCreateTime = DateUtil.parseUTC(usdRateItem.getCreateAt()).getTime();
            long matchedUsdRateTime = matchedUsdRateDate.getTime();
            if (matchedUsdRateTime >= usdRateCreateTime) {
                matchedUsdRate = usdRateItem;
            }
        }
        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.ATH_ORDER_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ATH_ORDER_EVENT_ACTION_GET_USD_RATE)
                .p("QueryBeginDate", queryBeginDate)
                .p("QueryEndDate", queryEndDate)
                .p("MatchedUsdRateDate", matchedUsdRateDate)
                .p("MatchedUsdRate", matchedUsdRate == null ? null : JSON.toJSONString(matchedUsdRate))
                .i();
        return matchedUsdRate != null ?
                new BigDecimal(matchedUsdRate.getPrice()).setScale(2, RoundingMode.UP) : null;
    }

    private R<OrderCreateResp> createOrder(AccountModel accountModel,
                                           OrderEntity orderEntity,
                                           OrderPaymentEntity orderPaymentEntity,
                                           List<OrderDeviceEntity> orderDeviceEntityList) {
        KvLogger kvBaseLogger = KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.ATH_ORDER_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ATH_ORDER_EVENT_ACTION_CREATE_ORDER)
                .p(LogFieldConstants.ACCOUNT_ID, orderEntity.getAccountId())
                .p(LogFieldConstants.ACCOUNT_NAME, orderEntity.getAccountName())
                .p(LogFieldConstants.TENANT_ID, orderEntity.getTenantId())
                .p("UsdOrderId", orderEntity.getOrderId())
                .p("UsdPaymentOrderCode", orderPaymentEntity.getPaymentOrderId());

        R<OrderCreateResp> athOrderCreateRespR = feeCenterRemoteOrderService.createOrder(
                createAthCreateOrderReq(accountModel, orderEntity, orderPaymentEntity.getPaymentOrderId(), orderDeviceEntityList));
        if (athOrderCreateRespR == null) {
            kvBaseLogger.p(LogFieldConstants.ERR_CODE, -1)
                    .p(LogFieldConstants.ERR_MSG, "call fee center create order, result is null").e();
            return R.failed(SysCode.x00000601.getValue(), SysCode.x00000601.getMsg());
        }
        if (athOrderCreateRespR.getCode() != R.ok().getCode()) {
            kvBaseLogger.p(LogFieldConstants.ERR_CODE, athOrderCreateRespR.getCode())
                    .p(LogFieldConstants.ERR_MSG, athOrderCreateRespR.getMsg())
                    .i();
            return R.failed(athOrderCreateRespR.getCode(), JSON.toJSONString(athOrderCreateRespR));
        }
        OrderCreateResp athOrderCreateResp = athOrderCreateRespR.getData();
        kvBaseLogger.p("athOrderId", athOrderCreateResp.getOrderCode())
                .p("athOrderStatus", athOrderCreateResp.getOrderStatus()).i();
        return athOrderCreateRespR;
    }

    private R<?> closeOrder(String athOrderId, Long orderTid) {
        return feeCenterRemoteOrderService.orderClose(CloseOrderReq.builder()
                .orderCode(athOrderId)
                .tid(orderTid)
                .build());
    }

    private OrderCreateReq createAthCreateOrderReq(AccountModel accountModel, OrderEntity orderEntity,
                                                   String thirdPartyOrderCode, List<OrderDeviceEntity> orderDeviceEntityList) {
        int feeCenterOrderPeriod = 0;
        int feeCenterOrderDuration = 0;
        switch (ServiceDurationPeriod.valueOf(orderEntity.getServiceDurationPeriod())) {
            case Day:
                feeCenterOrderPeriod = 3;
                feeCenterOrderDuration = orderEntity.getServiceDuration();
                break;
            case Week:
                feeCenterOrderPeriod = 1;
                feeCenterOrderDuration = orderEntity.getServiceDuration();
                break;
            case Month:
                feeCenterOrderPeriod = 2;
                feeCenterOrderDuration = orderEntity.getServiceDuration();
                break;
            default:
                feeCenterOrderPeriod = 4;
                feeCenterOrderDuration = orderEntity.getServiceDuration();
                break;
        }

        List<OrderCreateReq.Resource> resourceList = Lists.newArrayList();
        orderDeviceEntityList.forEach(orderDeviceEntity -> resourceList.add(
                OrderCreateReq.Resource.builder()
                        .count(orderDeviceEntity.getQuantity())
                        .spec(orderDeviceEntity.getSpec())
                        .subSpec(orderDeviceEntity.getSubSpec())
                        .region(orderDeviceEntity.getRegionCode())
                        .resourcePool(orderDeviceEntity.getResourcePool())
                        .build()
        ));
        return OrderCreateReq.builder()
                .thirdPartyOrderCode(thirdPartyOrderCode)
                .orderType(1)
                .autoRenew(orderEntity.getAutoRenew())
                .businessChannel(2)
                .tid(orderEntity.getTenantId())
                .orderPeriod(feeCenterOrderPeriod)
                .orderDuration(feeCenterOrderDuration)
                .resources(resourceList)
                .build();
    }

    private R<WalletTransferResp> athTransfer(AccountModel accountModel, String toTransferTenantName, OrderEntity orderEntity,
                                              OrderPaymentEntity orderPaymentEntity, Long fromTid, Long toTid, String athOrderId,
                                              String transferAthAmount, Integer transferType, Integer billId, boolean isBackToAdmin) {
        KvLogger kvTransferLogger = KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.ATH_ORDER_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ATH_ORDER_EVENT_ACTION_TRANSFER_ATH)
                .p(LogFieldConstants.ACCOUNT_ID, orderEntity.getAccountId())
                .p(LogFieldConstants.ACCOUNT_NAME, orderEntity.getAccountName())
                .p("NeedTransferAthValue", transferAthAmount)
                .p("AthOrderId", athOrderId)
                .p("FromTid", fromTid)
                .p("ToTid", toTid)
                .p("OrderId", orderEntity.getOrderId())
                .p("OrderPaymentId", orderPaymentEntity.getPaymentOrderId())
                .p("isBackToAdmin", isBackToAdmin);

        R<WalletTransferResp> walletTransferRespR;
        if (isBackToAdmin) {
            walletTransferRespR = remoteFeeCenterWalletService.transferToAdmin(web2ApiConfig.getAthAdminTid(),
                    WalletTransferToAdminReq.builder()
                            .fromTid(fromTid.intValue())
                            .tid(accountModel.getTenantId())
                            .tenantType(accountModel.getTenantType())
                            .accountId(accountModel.getAccountId())
                            .billId(billId)
                            .build());
        } else {
            walletTransferRespR = remoteFeeCenterWalletService.transferToTenant(web2ApiConfig.getAthAdminTid(),
                    WalletTransferToTenantReq.builder()
                            .fromTid(fromTid.intValue())
                            .toTid(toTid.intValue())
                            .tid(accountModel.getTenantId())
                            .tenantType(accountModel.getTenantType())
                            .accountId(accountModel.getAccountId())
                            .amount(transferAthAmount)
                            .build());
        }
        if (walletTransferRespR == null) {
            kvTransferLogger.p(LogFieldConstants.ERR_CODE, -1)
                    .p(LogFieldConstants.ERR_MSG, "call fee center transfer ath, result is null")
                    .e();
            return R.failed(SysCode.x00000602.getValue(), SysCode.x00000602.getMsg());
        }
        if (walletTransferRespR.getCode() == R.ok().getCode()) {
            kvTransferLogger.p("billId", walletTransferRespR.getData().getBillId()).i();
        } else {
            kvTransferLogger.p(LogFieldConstants.ERR_CODE, walletTransferRespR.getCode())
                    .p(LogFieldConstants.ERR_MSG, walletTransferRespR.getMsg()).i();
            return R.failed(walletTransferRespR.getCode(), JSON.toJSONString(walletTransferRespR));
        }
        // add transfer ath bill
        tenantAthTransferRecordService.save(TenantAthTransferRecordEntity.builder()
                .orderId(orderEntity.getOrderId())
                .paymentOrderId(orderPaymentEntity.getPaymentOrderId())
                .athOrderId(athOrderId)
                .athBillId(walletTransferRespR.getData().getBillId() != null ? walletTransferRespR.getData().getBillId().toString() : "")
                .athAmount(new BigDecimal(transferAthAmount))
                .transFromTenantId(fromTid)
                .transToTenantId(toTid)
                .transToTenantName(toTransferTenantName)
                .transType(transferType)
                .transferStatus(walletTransferRespR.getCode())
                .failureReason(walletTransferRespR.getMsg())
                .createTime(new Timestamp(System.currentTimeMillis()))
                .build());
        return walletTransferRespR;
    }

    private R<?> payOrder(AccountModel accountModel, OrderEntity orderEntity, String athOrderCode) {
        KvLogger kvPayOrderLogger = KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.ATH_ORDER_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ATH_ORDER_EVENT_ACTION_PAY_ORDER)
                .p(LogFieldConstants.ACCOUNT_ID, orderEntity.getAccountId())
                .p(LogFieldConstants.ACCOUNT_NAME, orderEntity.getAccountName())
                .p(LogFieldConstants.TENANT_ID, orderEntity.getTenantId())
                .p("UsdOrderId", orderEntity.getOrderId())
                .p("AthOrderId", athOrderCode);

        R<?> athPayOrderR = pmsCenterOrderService.payOrder(PayOrderReq.builder()
                .orderCode(athOrderCode)
                .tid(orderEntity.getTenantId())
                .build());
        if (athPayOrderR == null) {
            kvPayOrderLogger.p(LogFieldConstants.ERR_CODE, -1)
                    .p(LogFieldConstants.ERR_MSG, "call pms pay order, result is null")
                    .e();
            return R.failed(SysCode.x00000603.getValue(), SysCode.x00000603.getMsg());
        }
        if (athPayOrderR.getCode() == R.ok().getCode()) {
            kvPayOrderLogger.p(LogFieldConstants.Success, true).i();
        } else {
            kvPayOrderLogger.p(LogFieldConstants.ERR_CODE, athPayOrderR.getCode())
                    .p(LogFieldConstants.ERR_MSG, athPayOrderR.getMsg()).i();
            return R.failed(athPayOrderR.getCode(), JSON.toJSONString(athPayOrderR));
        }
        return athPayOrderR;
    }

    private R<?> bindContainer(String usdOrderId, String athOrderId, List<Long> cIds) {
        KvLogger kvLogger = KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.ATH_ORDER_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ATH_ORDER_EVENT_ACTION_BIND_CONTAINER)
                .p("UsdOrderId", usdOrderId)
                .p("AthOrderId", athOrderId);
        R<?> bindR = feeCenterRemoteOrderService.bindContainer(BindContainerReq.builder()
                .orderCode(athOrderId)
                .cids(cIds)
                .build());
        if (bindR == null) {
            kvLogger.p(LogFieldConstants.ERR_CODE, -1)
                    .p(LogFieldConstants.ERR_MSG, "call feeCenter  bind container, result is null")
                    .e();
            return R.failed(SysCode.x00000604.getValue(), SysCode.x00000604.getMsg());
        }
        if (bindR.getCode() == R.ok().getCode()) {
            kvLogger.p("bindResult", true).i();
        } else {
            kvLogger.p(LogFieldConstants.ERR_CODE, bindR.getCode())
                    .p(LogFieldConstants.ERR_MSG, bindR.getMsg())
                    .i();
            return R.failed(bindR.getCode(), JSON.toJSONString(bindR));
        }
        return bindR;
    }

    private R<?> athOrderRollback(AccountModel accountModel, OrderEntity orderEntity, OrderPaymentEntity orderPaymentEntity,
                                  R<OrderCreateResp> athOrderCreateRespR, String transferAthAmount, Integer athTransferBillId) {
        // ath transfer rollback
        R<WalletTransferResp> walletTransferRollbackR = athTransfer(accountModel, web2ApiConfig.getAthAdminName(), orderEntity,
                orderPaymentEntity, orderEntity.getTenantId(), web2ApiConfig.getAthAdminTid(), athOrderCreateRespR.getData().getOrderCode(),
                transferAthAmount, AthTransferType.Rollback.getValue(), athTransferBillId, true);
        if (walletTransferRollbackR.getCode() != R.ok().getCode()) {
            return walletTransferRollbackR;
        }
        // close order
        R<?> closeOrderR = closeOrder(athOrderCreateRespR.getData().getOrderCode(), orderEntity.getTenantId());
        if (closeOrderR.getCode() != R.ok().getCode()) {
            return closeOrderR;
        }
        return R.ok();
    }

    private long calculateNeedTransferAthValue(BigDecimal usdValue, BigDecimal usdRate) {
        long athValue = usdValue.divide(usdRate, 2, RoundingMode.UP).longValue();
        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.ATH_ORDER_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ATH_ORDER_EVENT_ACTION_CALCULATE_ATH_VALUE)
                .p("UsdValue", usdValue)
                .p("UsdRate", usdRate)
                .p("AthValue", athValue)
                .i();
        return athValue;
    }
}
