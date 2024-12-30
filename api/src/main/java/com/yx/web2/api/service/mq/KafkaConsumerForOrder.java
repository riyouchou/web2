package com.yx.web2.api.service.mq;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yx.pass.remote.pms.PmsRemoteSpecService;
import com.yx.web2.api.common.constant.Web2LoggerEvents;
import com.yx.web2.api.common.enums.OrderContainerStatus;
import com.yx.web2.api.common.enums.OrderContainerTopicStatus;
import com.yx.web2.api.common.enums.OrderResourcePool;
import com.yx.web2.api.common.enums.RefundStatus;
import com.yx.web2.api.common.req.container.dto.FeeCenterPickOrderContainerDTO;
import com.yx.web2.api.common.req.order.CidsByDeviceInfoReq;
import com.yx.web2.api.entity.AthOrderInfoEntity;
import com.yx.web2.api.entity.OrderContainerEntity;
import com.yx.web2.api.entity.OrderDeviceEntity;
import com.yx.web2.api.entity.OrderEntity;
import com.yx.web2.api.service.IAthOrderInfoService;
import com.yx.web2.api.service.IOrderContainerService;
import com.yx.web2.api.service.IOrderDeviceService;
import com.yx.web2.api.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.logger.LogFieldConstants;
import org.yx.lib.utils.util.R;
import org.yx.lib.utils.util.StringUtil;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerForOrder {

    private final IAthOrderInfoService athOrderInfoService;
    private final IOrderService orderService;
    private final IOrderDeviceService orderDeviceService;
    private final IOrderContainerService orderContainerService;
    private final PmsRemoteSpecService pmsRemoteSpecService;

    /**
     * topic: idc_pick_order_container
     * body: {
     * "wholesaleTid": 123456,
     * "orderCode": "aaaaa",
     * "type":"1" # 1.绑定  2 解绑
     * "cids": [
     * 123,
     * 234,
     * 345
     * ]
     * }
     *
     * @date 2024/11/28
     */
    public void idcPickOrderContainer(String msg) {
        if (StringUtils.hasLength(msg)) {
            FeeCenterPickOrderContainerDTO pickOrderContainerDTO = JSONObject.parseObject(msg, FeeCenterPickOrderContainerDTO.class);
            if (StringUtil.isBlank(pickOrderContainerDTO.getThirdPartyOrderCode())
                    || pickOrderContainerDTO.getWholesaleTid() == null
                    || pickOrderContainerDTO.getType() == null) {
                logError("thirdPartyOrderCode or wholesaleTid or type is null", msg);
                return;
            }
            AthOrderInfoEntity athOrderInfoEntity = athOrderInfoService.getOne(Wrappers.lambdaQuery(AthOrderInfoEntity.class)
                    .eq(AthOrderInfoEntity::getPaymentOrderId, pickOrderContainerDTO.getThirdPartyOrderCode())
                    .eq(AthOrderInfoEntity::getTenantId, pickOrderContainerDTO.getWholesaleTid()));
            if (athOrderInfoEntity == null) {
                logError("athOrderInfoEntity find athOrderInfoEntity by PaymentOrderId and TenantId is null", msg);
                return;
            }
            //get orderEntity
            OrderEntity orderEntity = orderService.getOrderByTenantId(athOrderInfoEntity.getOrderId(), pickOrderContainerDTO.getWholesaleTid());
            if (orderEntity == null) {
                logError("orderEntity not found", msg);
                return;
            }

            if (pickOrderContainerDTO.getType() == 1) {
                if (pickOrderContainerDTO.getCids() == null
                        || pickOrderContainerDTO.getCids().isEmpty()) {
                    logError("cids or orderCode is null", msg);
                    return;
                }
                if (Objects.equals(orderEntity.getOrderResourcePool(), OrderResourcePool.ARS.getValue())) {
                    if (StringUtil.isBlank(pickOrderContainerDTO.getOrderCode())) {
                        logError("type=1 and resourcePool=ARS orderCode is null", msg);
                        return;
                    }
                    CidsByDeviceInfoReq cidsByDeviceInfoReq = CidsByDeviceInfoReq.builder()
                            .orderId(pickOrderContainerDTO.getOrderCode())
                            .wholesaleTid(pickOrderContainerDTO.getWholesaleTid().toString())
                            .cids(pickOrderContainerDTO.getCids().stream().map(String::valueOf).collect(Collectors.joining(",")))
                            .build();
                    JSONObject cidsByDeviceInfoReqJson = JSONObject.parseObject(JSONObject.toJSONString(cidsByDeviceInfoReq));
                    R<?> cidsR = pmsRemoteSpecService.getCidsBySpecAndRegion(cidsByDeviceInfoReqJson);

                    if (cidsR != null && cidsR.getCode() == R.ok().getCode()) {
                        JSONArray jsonArray = (JSONArray) cidsR.getData();
                        List<OrderContainerEntity> containerList = new ArrayList<>();
                        for (int i = 0; i < jsonArray.size(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            String[] cids = jsonObject.getString("cids").split(",");
                            String spec = jsonObject.getString("spec");
                            String regionCode = jsonObject.getString("regionCode");
                            List<Long> cidsList = Arrays.stream(cids)
                                    .map(String::trim)
                                    .map(Long::valueOf)
                                    .collect(Collectors.toList());
                            //get orderDevice
                            OrderDeviceEntity orderDeviceEntity = orderDeviceService.getOne(Wrappers.lambdaQuery(OrderDeviceEntity.class)
                                    .eq(OrderDeviceEntity::getOrderId, orderEntity.getOrderId())
                                    .eq(OrderDeviceEntity::getSpec, spec)
                                    .eq(OrderDeviceEntity::getRegionCode, regionCode));

                            if (orderDeviceEntity == null) {
                                logError("orderDeviceEntity not found", msg);
                                return;
                            }
                            for (Long cid : cidsList) {
                                containerList.add(OrderContainerEntity.builder()
                                        .cid(cid)
                                        .orderDeviceId(orderDeviceEntity.getId())
                                        .orderId(orderEntity.getOrderId())
                                        .orderResourcePool(OrderResourcePool.ARS.getValue())
                                        .wholesaleTid(orderEntity.getTenantId())
                                        .spec(orderDeviceEntity.getSpec())
                                        .regionCode(orderDeviceEntity.getRegionCode())
                                        .wholesalePrice(orderDeviceEntity.getDiscountPrice())
                                        .serviceStartTime(new Timestamp(System.currentTimeMillis()))
                                        .status(OrderContainerStatus.IN_SERVICE.getValue())
                                        .refundStatus(RefundStatus.NOT_REFUNDED.getValue())
                                        .build()
                                );
                            }
                        }
                        orderContainerService.saveBatch(containerList);

                    } else {
                        logError("cidsR error", msg);
                    }
                } else if (Objects.equals(orderEntity.getOrderResourcePool(), OrderResourcePool.BM.getValue())) {
                    // modify The Order To InService
                    orderContainerService.update(Wrappers.lambdaUpdate(OrderContainerEntity.class)
                            .set(OrderContainerEntity::getStatus, OrderContainerStatus.IN_SERVICE.getValue())
                            .eq(OrderContainerEntity::getOrderId, orderEntity.getOrderId())
                            .eq(OrderContainerEntity::getWholesaleTid, orderEntity.getTenantId())
                            .eq(OrderContainerEntity::getOrderResourcePool, OrderResourcePool.BM.getValue())
                            .in(OrderContainerEntity::getCid, pickOrderContainerDTO.getCids())
                    );
                } else {
                    logError("idcPickOrderContainer orderResourcePool is not BM or ARS", msg);
                }

            } else if (pickOrderContainerDTO.getType() == 2) {
                if (Objects.equals(pickOrderContainerDTO.getStatus(), OrderContainerTopicStatus.IN_SERVICE.getValue())) {
                    if (pickOrderContainerDTO.getCids() == null
                            || pickOrderContainerDTO.getCids().isEmpty()) {
                        logError("type=2 and status=4 cids is null!", msg);
                        return;
                    }
                    orderContainerService.updateOrderContainersStatus(orderEntity, pickOrderContainerDTO.getCids(), OrderContainerStatus.UNPLEDGED_CONTAINER_UNAVAILABLE.getValue());
                } else if (Objects.equals(pickOrderContainerDTO.getStatus(), OrderContainerTopicStatus.CREDIT_INSUFFICIENT_CLOSED_ABNORMALLY.getValue())) {
                    orderContainerService.updateOrderContainersStatus(orderEntity, null, OrderContainerStatus.CREDIT_INSUFFICIENT_CLOSED_ABNORMALLY.getValue());
                } else if (Objects.equals(pickOrderContainerDTO.getStatus(), OrderContainerTopicStatus.ABNORMAL_CLOSED.getValue())) {
                    orderContainerService.updateOrderContainersStatus(orderEntity, null, OrderContainerStatus.ORDER_ABNORMALLY_CLOSED.getValue());
                } else if (Objects.equals(pickOrderContainerDTO.getStatus(), OrderContainerTopicStatus.CLOSED.getValue())) {
                    orderContainerService.updateOrderContainersStatus(orderEntity, null, OrderContainerStatus.ORDER_CLOSED_NORMALLY.getValue());
                } else {
                    logError("idcPickOrderContainer status is not in （ 3:关闭  4:服务中  5:非正常关闭  6:受信不足强行终止）", msg);
                }
            } else {
                logError("idcPickOrderContainer type is error", msg);
            }
        }
    }

    private void logError(String errMsg, String msg) {
        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.IDC_PICK_ORDER_CONTAINER_CONSUMER)
                .p(LogFieldConstants.ERR_MSG, errMsg)
                .p("msg", msg)
                .i();
    }


}
