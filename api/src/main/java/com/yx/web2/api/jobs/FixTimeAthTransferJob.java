package com.yx.web2.api.jobs;

import com.alibaba.fastjson.JSON;
import com.baomidou.dynamic.datasource.annotation.Master;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.yx.pass.remote.feecenter.FeeCenterRemoteWalletService;
import com.yx.pass.remote.feecenter.model.req.WalletTransferToTenantReq;
import com.yx.pass.remote.feecenter.model.resp.WalletTransferResp;
import com.yx.web2.api.common.constant.Web2LoggerEvents;
import com.yx.web2.api.config.Web2ApiConfig;
import com.yx.web2.api.entity.AthOrderInfoEntity;
import com.yx.web2.api.entity.SubscribedServiceEntity;
import com.yx.web2.api.entity.TenantAthTransferRecordEntity;
import com.yx.web2.api.service.IAthOrderInfoService;
import com.yx.web2.api.service.ISubscribedServiceService;
import com.yx.web2.api.service.ITenantAthTransferRecordService;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.yx.lib.job.core.YxJobRegister;
import org.yx.lib.utils.constant.CommonConstants;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.logger.LogFieldConstants;
import org.yx.lib.utils.util.R;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RefreshScope
@RequiredArgsConstructor
public class FixTimeAthTransferJob implements YxJobRegister {

    private final Web2ApiConfig web2ApiConfig;
    private final ISubscribedServiceService subscribedServiceService;
    private final IAthOrderInfoService athOrderInfoService;
    private final FeeCenterRemoteWalletService remoteFeeCenterWalletService;
    private final ITenantAthTransferRecordService tenantAthTransferRecordService;

    @Master
    @XxlJob("fixTimeAthTransferJobHandler")
    public void doHandler() {
        long jobId = XxlJobHelper.getJobId();
        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.TRANSFER_ATH_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.TRANSFER_ATH_EVENT_ACTION_START)
                .p("JobId", jobId)
                .i();
        try {
            // select inService list
            List<SubscribedServiceEntity> inServiceSubscribedList = subscribedServiceService.listInServiceSubscribedList();
            if (!inServiceSubscribedList.isEmpty()) {
                KvLogger.instance(this)
                        .p(LogFieldConstants.EVENT, Web2LoggerEvents.TRANSFER_ATH_EVENT)
                        .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.TRANSFER_ATH_EVENT_ACTION_START_ACTION_SELECT_IN_SERVICE_ORDERS)
                        .p("Count", inServiceSubscribedList.size())
                        .i();
                List<String> orderIds = inServiceSubscribedList.stream().map(SubscribedServiceEntity::getOrderId).collect(Collectors.toList());
                // select athOrders by orderIds
                List<AthOrderInfoEntity> athOrderInfoEntityList = athOrderInfoService.list(Wrappers.lambdaQuery(AthOrderInfoEntity.class).in(AthOrderInfoEntity::getOrderId, orderIds));
                KvLogger.instance(this)
                        .p(LogFieldConstants.EVENT, Web2LoggerEvents.TRANSFER_ATH_EVENT)
                        .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.TRANSFER_ATH_EVENT_ACTION_START_ACTION_SELECT_ATH_ORDERS)
                        .p("Count", athOrderInfoEntityList.size())
                        .i();
                for (AthOrderInfoEntity athOrderInfo : athOrderInfoEntityList) {
                    MDC.put(CommonConstants.TRACE_ID, athOrderInfo.getAthOrderId());
                    // transfer ath
                    R<WalletTransferResp> walletTransferRespR = remoteFeeCenterWalletService.transferToTenant(web2ApiConfig.getAthAdminTid(),
                            WalletTransferToTenantReq.builder()
                                    .fromTid(web2ApiConfig.getAthAdminTid().intValue())
                                    .toTid(athOrderInfo.getTenantId().intValue())
                                    .tid(web2ApiConfig.getAthAdminTid())
                                    .amount(athOrderInfo.getDailyAth())
                                    .build());
                    if (walletTransferRespR.getCode() == R.ok().getCode()) {
                        // add transfer ath bill
                        try {
                            Optional<SubscribedServiceEntity> findTenantNameEntityOpt =
                                    inServiceSubscribedList.stream().filter(item -> item.getOrderId().equals(athOrderInfo.getOrderId())).findFirst();
                            tenantAthTransferRecordService.save(TenantAthTransferRecordEntity.builder()
                                    .orderId(athOrderInfo.getOrderId())
                                    .paymentOrderId(athOrderInfo.getPaymentOrderId())
                                    .athOrderId(athOrderInfo.getAthOrderId())
                                    .athBillId(walletTransferRespR.getData().getBillId().toString())
                                    .athAmount(new BigDecimal(athOrderInfo.getDailyAth()))
                                    .transFromTenantId(web2ApiConfig.getAthAdminTid())
                                    .transToTenantId(athOrderInfo.getTenantId())
                                    .transToTenantName(findTenantNameEntityOpt.isPresent() ? findTenantNameEntityOpt.get().getTenantName() : "")
                                    .transferStatus(walletTransferRespR.getCode())
                                    .failureReason(walletTransferRespR.getMsg())
                                    .createTime(new Timestamp(System.currentTimeMillis()))
                                    .build());
                        } catch (Exception ex) {
                            KvLogger.instance(this)
                                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.TRANSFER_ATH_EVENT)
                                    .p(LogFieldConstants.ERR_MSG, ex.getMessage())
                                    .e(ex);
                        }
                    }
                    MDC.remove(CommonConstants.TRACE_ID);

                    KvLogger.instance(this)
                            .p(LogFieldConstants.EVENT, Web2LoggerEvents.TRANSFER_ATH_EVENT)
                            .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.TRANSFER_ATH_EVENT_ACTION_START_ACTION_TRANSFER_ATH)
                            .p("FromTid", web2ApiConfig.getAthAdminTid())
                            .p("ToTid", athOrderInfo.getTenantId())
                            .p("AthValue", athOrderInfo.getDailyAth())
                            .p("TransferResult", JSON.toJSONString(walletTransferRespR))
                            .i();
                }
            } else {
                KvLogger.instance(this)
                        .p(LogFieldConstants.EVENT, Web2LoggerEvents.TRANSFER_ATH_EVENT)
                        .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.TRANSFER_ATH_EVENT_ACTION_START_ACTION_SELECT_IN_SERVICE_ORDERS)
                        .p("Count", 0)
                        .i();
            }
        } catch (Exception ex) {
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.TRANSFER_ATH_EVENT)
                    .p(LogFieldConstants.ERR_MSG, ex.getMessage())
                    .e(ex);
        }
        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.TRANSFER_ATH_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.TRANSFER_ATH_EVENT_ACTION_END)
                .i();
    }

    @Override
    public String cron() {
        return "0 0 8 * * ?";
    }

    @Override
    public String jobDesc() {
        return "Regularly transfer Ath to tenants";
    }
}
