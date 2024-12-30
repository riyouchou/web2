package com.yx.web2.api.jobs;

import cn.hutool.core.date.DateUtil;
import com.baomidou.dynamic.datasource.annotation.Master;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.yx.web2.api.common.constant.Web2LoggerEvents;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.common.req.order.OrderIdReq;
import com.yx.web2.api.entity.ContractEntity;
import com.yx.web2.api.service.IContractService;
import com.yx.web2.api.service.IOrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.yx.lib.job.core.YxJobRegister;
import org.yx.lib.utils.constant.CommonConstants;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.logger.LogFieldConstants;
import org.yx.lib.utils.token.YxTokenBuilderUtil;
import org.yx.lib.utils.util.R;
import org.yx.lib.utils.util.StringUtil;

import java.util.List;

import static com.yx.web2.api.common.constant.Web2ApiConstants.SOURCE_TYPE_FROM_JOB;

@Component
@RefreshScope
@RequiredArgsConstructor
public class ContractScannerToCreateOrderJob implements YxJobRegister {

    private final IContractService contractService;
    private final IOrderService orderService;


    @Master
    @XxlJob("ContractScannerToCreateOrderJobHandler")
    public void doHandler() {
        long jobId = XxlJobHelper.getJobId();
        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT_ACTION_START)
                .p("JobId", jobId)
                .i();
        MDC.put(CommonConstants.TRACE_ID, String.valueOf(jobId));
        try {
            // select contract list
            String startDate = DateUtil.format(DateUtil.offsetDay(DateUtil.date(), -5), "yyyy-MM-dd");
            String endDate = DateUtil.today();
            List<ContractEntity> contractsWithoutMatchingOrders = contractService.findContractsWithoutMatchingOrders(startDate, endDate);
            if (!contractsWithoutMatchingOrders.isEmpty()) {
                KvLogger.instance(this)
                        .p(LogFieldConstants.EVENT, Web2LoggerEvents.SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT)
                        .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT_ACTION_SELECT_CONTRACT)
                        .p("Count", contractsWithoutMatchingOrders.size())
                        .i();
                AccountModel accountModel = new AccountModel();
                for (ContractEntity contract : contractsWithoutMatchingOrders) {
                    if (StringUtil.isBlank(contract.getOrderId())) {
                        continue;
                    }
                    accountModel.setAccountId(contract.getTenantId());
                    accountModel.setAccountName(contract.getTenantName());
                    accountModel.setTenantId(contract.getTenantId());
                    accountModel.setTenantName(contract.getTenantName());
                    accountModel.setBdAccountId(contract.getBdAccountId());
                    accountModel.setBdAccountName(contract.getBdAccountName());
                    accountModel.setTenantType("Admin");
                    OrderIdReq orderIdReq = new OrderIdReq();
                    orderIdReq.setOrderId(contract.getOrderId());
                    try {
                        String xUser = YxTokenBuilderUtil.buildXUser(contract.getTenantId(), "Admin", 0L);
                        MDC.put(CommonConstants.X_USER, xUser);
                        R<?> r = orderService.confirmPaid(contract.getTenantId(), accountModel, orderIdReq, SOURCE_TYPE_FROM_JOB);
                        if (r.getCode() == R.ok().getCode()) {
                            KvLogger.instance(this)
                                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT)
                                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT_ACTION_SELECT_CONTRACT_CONFIRM_PAID)
                                    .p("TenantId", contract.getTenantId())
                                    .i();
                        } else {
                            KvLogger.instance(this)
                                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT)
                                    .p(LogFieldConstants.ERR_CODE, r.getCode())
                                    .p(LogFieldConstants.ERR_MSG, r.getMsg())
                                    .p("TenantId", contract.getTenantId())
                                    .e();
                        }
                    } catch (Exception e) {
                        KvLogger.instance(this)
                                .p(LogFieldConstants.EVENT, Web2LoggerEvents.SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT)
                                .p("TenantId", contract.getTenantId())
                                .p(LogFieldConstants.ERR_MSG, e.getMessage())
                                .e(e);
                    }
                }
            } else {
                KvLogger.instance(this)
                        .p(LogFieldConstants.EVENT, Web2LoggerEvents.SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT)
                        .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT_ACTION_SELECT_END_CONTRACT)
                        .p("Count", 0)
                        .i();
            }

        } catch (Exception ex) {
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT)
                    .p(LogFieldConstants.ERR_MSG, ex.getMessage())
                    .e(ex);
        }
        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT_ACTION_END)
                .i();
    }

    @Override
    public String cron() {
        return "0 0 2 * * ?";
    }

    @Override
    public String jobDesc() {
        return "Regularly scan contract and create order";
    }
}
