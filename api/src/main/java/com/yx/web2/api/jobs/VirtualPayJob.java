package com.yx.web2.api.jobs;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.dynamic.datasource.annotation.Master;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.yx.web2.api.common.constant.CacheConstants;
import com.yx.web2.api.common.constant.Web2LoggerEvents;
import com.yx.web2.api.common.enums.InstalmentPaymentStatus;
import com.yx.web2.api.common.req.order.OrderPayVirtualReq;
import com.yx.web2.api.config.VirtualConfig;
import com.yx.web2.api.entity.OrderVirtualPaymentEntity;
import com.yx.web2.api.service.IOrderReconciliationExceptionService;
import com.yx.web2.api.service.IOrderService;
import com.yx.web2.api.service.IOrderVirtualPaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.yx.lib.job.core.YxJobRegister;
import org.yx.lib.utils.constant.CommonConstants;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.logger.LogFieldConstants;
import org.yx.lib.utils.util.StringUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class VirtualPayJob implements YxJobRegister {

    private final IOrderService orderService;
    private final IOrderReconciliationExceptionService orderReconciliationExceptionService;
    private final IOrderVirtualPaymentService orderVirtualPaymentService;
    private final RedisTemplate<String, String> redisTemplate;
    private final VirtualConfig virtualConfig;


    @Master
    @XxlJob("VirtualPayJobHandler")
    public void doHandler() {
        String jobKey = String.format(CacheConstants.JOB, "VirtualPayJobHandler");

        boolean isLockAcquired = false;
        long jobId = XxlJobHelper.getJobId();
        MDC.put(CommonConstants.TRACE_ID, String.valueOf(jobId));
        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.VIRTUAL_PAY_ORDER_JOB_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.VIRTUAL_PAY_ORDER_EVENT_ACTION_START)
                .p("JobId", MDC.get(CommonConstants.TRACE_ID))
                .i();
        try {
            isLockAcquired = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(jobKey, "LOCK", Duration.ofSeconds(virtualConfig.getVirtualJob())));
            if (!isLockAcquired) {
                KvLogger.instance(this)
                        .p(LogFieldConstants.EVENT, Web2LoggerEvents.VIRTUAL_PAY_ORDER_JOB_EVENT)
                        .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.VIRTUAL_PAY_ORDER_EVENT_ACTION_END)
                        .p("msg", "当前程序有正在执行的VirtualPayJobHandler")
                        .i();
                return;
            }
            // select list
            String startDate = DateUtil.format(DateUtil.offsetMinute(DateUtil.date(), -virtualConfig.getVirtualPaymentTime()), "yyyy-MM-dd HH:mm:ss");
            String endDate = DateUtil.now();
            // query overdue and unpaid data
            List<OrderVirtualPaymentEntity> orderVirtualPaymentOverdueEntities = orderVirtualPaymentService.list(Wrappers.lambdaQuery(OrderVirtualPaymentEntity.class)
                    .lt(OrderVirtualPaymentEntity::getLastUpdateTime, startDate)
                    .eq(OrderVirtualPaymentEntity::getStatus, InstalmentPaymentStatus.None.getValue())
            );
            if (orderVirtualPaymentOverdueEntities != null && !orderVirtualPaymentOverdueEntities.isEmpty()){
                KvLogger.instance(this)
                        .p(LogFieldConstants.EVENT, Web2LoggerEvents.VIRTUAL_PAY_ORDER_JOB_EVENT)
                        .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.VIRTUAL_PAY_ORDER_EVENT_ACTION_SELECT)
                        .p("msg", "处理过期的订单数据")
                        .p("size", orderVirtualPaymentOverdueEntities.size())
                        .i();

                //group the entities by orderId
                Map<String, List<OrderVirtualPaymentEntity>> groupedByOrderId = orderVirtualPaymentOverdueEntities.stream()
                        .collect(Collectors.groupingBy(OrderVirtualPaymentEntity::getOrderId));
                // delete expired data
                List<OrderPayVirtualReq.OrderPay> orderPayList = groupedByOrderId.entrySet().stream()
                        .map(entry -> {
                            OrderPayVirtualReq.OrderPay orderPay = new OrderPayVirtualReq.OrderPay();
                            orderPay.setOrderId(entry.getKey());

                            // Extract payment IDs for this order
                            List<String> paymentIds = entry.getValue().stream()
                                    .map(OrderVirtualPaymentEntity::getPaymentOrderId)
                                    .collect(Collectors.toList());

                            orderPay.setOrderPaymentIdList(paymentIds);
                            return orderPay;
                        })
                        .collect(Collectors.toList());

                for (OrderPayVirtualReq.OrderPay orderPay : orderPayList){
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


            List<OrderVirtualPaymentEntity> orderVirtualPaymentEntities = orderVirtualPaymentService.list(Wrappers.lambdaQuery(OrderVirtualPaymentEntity.class)
                    .ge(OrderVirtualPaymentEntity::getLastUpdateTime, startDate)
                    .le(OrderVirtualPaymentEntity::getLastUpdateTime, endDate)
                    .eq(OrderVirtualPaymentEntity::getStatus, InstalmentPaymentStatus.None.getValue())
                    .ne(OrderVirtualPaymentEntity::getHashCode, "")

            );

            if (orderVirtualPaymentEntities.isEmpty()) {
                KvLogger.instance(this)
                        .p(LogFieldConstants.EVENT, Web2LoggerEvents.VIRTUAL_PAY_ORDER_JOB_EVENT)
                        .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.VIRTUAL_PAY_ORDER_EVENT_ACTION_END)
                        .p("Count", 0)
                        .p("JobId", MDC.get(CommonConstants.TRACE_ID))
                        .i();
                return;
            }
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.VIRTUAL_PAY_ORDER_JOB_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.VIRTUAL_PAY_ORDER_EVENT_ACTION_SELECT)
                    .p("Count", orderVirtualPaymentEntities.size())
                    .p("JobId", MDC.get(CommonConstants.TRACE_ID))
                    .i();

            // group by hashcode
            Map<String, List<OrderVirtualPaymentEntity>> groupedByHashCode = orderVirtualPaymentEntities.stream()
                    .filter(entity -> {
                        String hashCode = entity.getHashCode();
                        return hashCode != null && !hashCode.isEmpty();
                    })
                    .collect(Collectors.groupingBy(OrderVirtualPaymentEntity::getHashCode));

            List<OrderPayVirtualReq> orderPayVirtualReqs = groupedByHashCode.entrySet().stream()
                    .map(entry -> {
                        String hashCode = entry.getKey();
                        List<OrderVirtualPaymentEntity> entities = entry.getValue();

                        OrderPayVirtualReq req = new OrderPayVirtualReq();
                        req.setHashCode(hashCode);
                        req.setFrom(entities.get(0).getFromAddress());
                        req.setTo(entities.get(0).getToAddress());
                        req.setAmount(entities.get(0).getAmount());
                        req.setType(entities.get(0).getType());

                        List<OrderPayVirtualReq.OrderPay> orderPayList = entities.stream()
                                .collect(Collectors.groupingBy(OrderVirtualPaymentEntity::getOrderId))
                                .entrySet().stream()
                                .map(orderEntry -> {
                                    OrderPayVirtualReq.OrderPay orderPay = new OrderPayVirtualReq.OrderPay();
                                    orderPay.setOrderId(orderEntry.getKey());
                                    orderPay.setOrderPaymentIdList(orderEntry.getValue().stream()
                                            .map(OrderVirtualPaymentEntity::getPaymentOrderId)
                                            .collect(Collectors.toList()));
                                    return orderPay;
                                })
                                .collect(Collectors.toList());

                        req.setOrderPayList(orderPayList);
                        return req;
                    })
                    .collect(Collectors.toList());


            // process access 10 threads
            ExecutorService executor = Executors.newFixedThreadPool(10);
            try {
                CompletableFuture.allOf(
                        orderPayVirtualReqs.stream()
                                .map(entity -> CompletableFuture.runAsync(() -> processTransaction(entity), executor))
                                .toArray(CompletableFuture[]::new)
                ).join();

            } finally {
                executor.shutdown();
            }

            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.VIRTUAL_PAY_ORDER_JOB_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.VIRTUAL_PAY_ORDER_EVENT_ACTION_END)
                    .p("JobId", MDC.get(CommonConstants.TRACE_ID))
                    .i();


        } catch (Exception ex) {
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.VIRTUAL_PAY_ORDER_JOB_EVENT)
                    .p(LogFieldConstants.ERR_MSG, ex.getMessage())
                    .p("JobId", MDC.get(CommonConstants.TRACE_ID))
                    .e(ex);
        } finally {
            if (isLockAcquired) {
                redisTemplate.delete(jobKey);
            }
            MDC.remove(CommonConstants.TRACE_ID);
        }
        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.VIRTUAL_PAY_ORDER_JOB_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.VIRTUAL_PAY_ORDER_EVENT_ACTION_END)
                .p("JobId", MDC.get(CommonConstants.TRACE_ID))
                .i();
    }

    private void processTransaction(OrderPayVirtualReq orderPayVirtualReq) {
        try {
            Web3j web3j = Web3j.build(new HttpService(virtualConfig.getVirtualUrl()));
            EthTransaction transaction = web3j.ethGetTransactionByHash(orderPayVirtualReq.getHashCode()).send();
            Optional<Transaction> optionalTransaction = transaction.getTransaction();
            if (optionalTransaction.isPresent()) {
                Transaction tx = optionalTransaction.get();
                String to = tx.getTo();
                if (StringUtil.isNotBlank(to)) {
                    String input = tx.getInput();
                    //ERC20 transfer
                    BigDecimal actualAmount = null;
                    BigInteger amount = null;
                    if (input.startsWith("0xa9059cbb")) {
                        String amountHex = input.substring(74);
                        amount = new BigInteger(amountHex, 16);
                    }else{
                        int transferPosition = input.indexOf("a9059cbb");
                        if (transferPosition != -1) {
                            String transferData = input.substring(transferPosition);
//                            String recipient = "0x" + transferData.substring(32, 72); // 8 (a9059cbb) + 24 (padding) = 32
                            String amountHex = transferData.substring(72, 136); // 下一个32字节
                            amount = new BigInteger(amountHex, 16);
                        }
                    }
                    if (amount != null) {
                        actualAmount = new BigDecimal(amount).divide(BigDecimal.TEN.pow(6), 6, RoundingMode.HALF_UP);
                    }
                    EthGetTransactionReceipt receipt;
                    try {
                        receipt = web3j.ethGetTransactionReceipt(orderPayVirtualReq.getHashCode()).send();
                    } catch (Exception ex) {
                        KvLogger.instance(this)
                                .p(LogFieldConstants.EVENT, Web2LoggerEvents.VIRTUAL_PAY_ORDER_JOB_EVENT)
                                .p(LogFieldConstants.ERR_MSG, ex.getMessage())
                                .p("JobId", MDC.get(CommonConstants.TRACE_ID))
                                .i();
                        return;
                    }
                    Optional<TransactionReceipt> transactionReceipt = receipt.getTransactionReceipt();
                    if (transactionReceipt.isPresent()) {
                        String status = transactionReceipt.get().getStatus();
                        if ("0x1".equals(status)) {
                            orderPayVirtualReq.getOrderPayList().forEach(orderPay -> {
                                try {
                                    orderVirtualPaymentService.saveVirtualPaymentStatusSuccess(orderPay);
                                } catch (Exception ex) {
                                    KvLogger.instance(this)
                                            .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                                            .p(LogFieldConstants.ACTION, "saveVirtualPaymentRecordSuccess")
                                            .p(LogFieldConstants.ERR_MSG, ex.getMessage())
                                            .p("OrderPay", JSON.toJSONString(orderPay))
                                            .e(ex);
                                }
                            });
                        } else if ("0x0".equals(status)) {
                            orderService.savePaymentRecord(orderPayVirtualReq, false);
                        }
                    }
                    //The transfer amount is inconsistent with the actual amount needed or to a different address
                    BigDecimal reqAmount = Convert.toBigDecimal(orderPayVirtualReq.getAmount(), BigDecimal.ZERO);
                    if (actualAmount != null && reqAmount.compareTo(actualAmount) != 0 || !to.equalsIgnoreCase(orderPayVirtualReq.getTo())) {
                        orderReconciliationExceptionService.saveBatchOrderReconciliationException(orderPayVirtualReq, actualAmount, reqAmount, to);
                    }
                }
            }
        } catch (Exception ex) {
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.VIRTUAL_PAY_ORDER_JOB_EVENT)
                    .p(LogFieldConstants.ERR_MSG, ex.getMessage())
                    .p("JobId", MDC.get(CommonConstants.TRACE_ID))
                    .e(ex);
        }
    }

    @Override
    public String cron() {
        return "0/10 * * * * ?";
    }

    @Override
    public String jobDesc() {
        return "scan virtual pay";
    }
}
