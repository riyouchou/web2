package com.yx.web2.api.jobs;

import com.baomidou.dynamic.datasource.annotation.Master;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.yx.web2.api.common.constant.Web2LoggerEvents;
import com.yx.web2.api.common.enums.OrderStatus;
import com.yx.web2.api.entity.OrderEntity;
import com.yx.web2.api.entity.SubscribedServiceEntity;
import com.yx.web2.api.service.IOrderService;
import com.yx.web2.api.service.ISubscribedServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.yx.lib.job.core.YxJobRegister;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.logger.LogFieldConstants;
import org.yx.lib.utils.util.ListUtil;
import org.yx.lib.utils.util.StringPool;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 扫描服务时长到期的订单
 */
@Component
@RequiredArgsConstructor
@RefreshScope
public class ServiceEndOrderScannerJob implements YxJobRegister {
    private final IOrderService orderService;
    private final ISubscribedServiceService subscribedServiceService;

    @Master
    @XxlJob("serviceEndOrderScannerJobHandler")
    public void doHandler() {
        long jobId = XxlJobHelper.getJobId();
        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.SCAN_SERVICE_END_ORDER_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.SCAN_SERVICE_END_ORDER_EVENT_ACTION_START)
                .p("JobId", jobId)
                .i();
        try {
            List<SubscribedServiceEntity> serviceEndSubscribedList = subscribedServiceService.listServiceEndSubscribedList();
            if (!serviceEndSubscribedList.isEmpty()) {
                KvLogger.instance(this)
                        .p(LogFieldConstants.EVENT, Web2LoggerEvents.SCAN_SERVICE_END_ORDER_EVENT)
                        .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.SCAN_SERVICE_END_ORDER_EVENT_ACTION_SELECT_SERVICE_END_ORDERS)
                        .p("Count", serviceEndSubscribedList.size())
                        .i();
                List<String> serviceEndOrderIds = serviceEndSubscribedList.stream().map(SubscribedServiceEntity::getOrderId).collect(Collectors.toList());
                List<List<String>> batchList = ListUtil.batchList(serviceEndOrderIds, 50);
                batchList.forEach(orderIds -> {
                    orderService.update(Wrappers.lambdaUpdate(OrderEntity.class)
                            .set(OrderEntity::getOrderStatus, OrderStatus.End.getValue())
                            .eq(OrderEntity::getOrderStatus, OrderStatus.InService.getValue())
                            .in(OrderEntity::getOrderId, orderIds));
                    KvLogger.instance(this)
                            .p(LogFieldConstants.EVENT, Web2LoggerEvents.SCAN_SERVICE_END_ORDER_EVENT)
                            .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.SCAN_SERVICE_END_ORDER_EVENT_ACTION_UPDATE_ORDER)
                            .p("OrderIds", String.join(StringPool.COMMA, orderIds))
                            .p("SourceStatus", OrderStatus.InService.getName())
                            .p("ToStatus", OrderStatus.End.getName())
                            .i();
                });
            } else {
                KvLogger.instance(this)
                        .p(LogFieldConstants.EVENT, Web2LoggerEvents.SCAN_SERVICE_END_ORDER_EVENT)
                        .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.SCAN_SERVICE_END_ORDER_EVENT_ACTION_SELECT_SERVICE_END_ORDERS)
                        .p("Count", 0)
                        .i();
            }
        } catch (Exception ex) {
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.SCAN_SERVICE_END_ORDER_EVENT)
                    .p(LogFieldConstants.ERR_MSG, ex.getMessage())
                    .e(ex);
        }
        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.SCAN_SERVICE_END_ORDER_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.SCAN_SERVICE_END_ORDER_EVENT_ACTION_END)
                .i();
    }

    @Override
    public String cron() {
        return "0 * * * * ?";
    }

    @Override
    public String jobDesc() {
        return "Scan expired orders";
    }
}
