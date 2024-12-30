package com.yx.web2.api.service.impl;

import com.baomidou.dynamic.datasource.annotation.Master;
import com.baomidou.dynamic.datasource.annotation.Slave;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yx.pass.remote.feecenter.FeeCenterRemoteOrderService;
import com.yx.pass.remote.feecenter.model.req.CloseOrderReq;
import com.yx.web2.api.common.constant.Web2LoggerEvents;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.entity.AthOrderInfoEntity;
import com.yx.web2.api.entity.OrderEntity;
import com.yx.web2.api.mapper.AthOrderInfoMapper;
import com.yx.web2.api.service.IAthOrderInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.logger.LogFieldConstants;
import org.yx.lib.utils.util.R;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AthOrderInfoServiceImpl extends ServiceImpl<AthOrderInfoMapper, AthOrderInfoEntity> implements IAthOrderInfoService {

    private final FeeCenterRemoteOrderService feeCenterRemoteOrderService;

    @Master
    @Override
    public void saveAthOrderInfo(AthOrderInfoEntity entity) {
        if (count(Wrappers.lambdaQuery(AthOrderInfoEntity.class).eq(AthOrderInfoEntity::getAthOrderId, entity.getAthOrderId())
                .eq(AthOrderInfoEntity::getPaymentOrderId, entity.getPaymentOrderId())
                .eq(AthOrderInfoEntity::getOrderId, entity.getOrderId())) > 0) {
            return;
        }
        this.save(entity);
    }

    @Master
    @Override
    public R<?> closeOrder(AccountModel accountModel, OrderEntity orderEntity) {
        List<AthOrderInfoEntity> athOrderInfoEntities = this.list(Wrappers.lambdaQuery(AthOrderInfoEntity.class)
                .eq(AthOrderInfoEntity::getOrderId, orderEntity.getOrderId()));
        for (AthOrderInfoEntity athOrderInfo : athOrderInfoEntities) {
            KvLogger kvLogger = KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.ATH_ORDER_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ATH_ORDER_EVENT_ACTION_CLOSE_ORDER)
                    .p(LogFieldConstants.ACCOUNT_ID, orderEntity.getAccountId())
                    .p(LogFieldConstants.ACCOUNT_NAME, orderEntity.getAccountName())
                    .p(LogFieldConstants.TENANT_ID, orderEntity.getTenantId())
                    .p("OrderId", athOrderInfo.getOrderId())
                    .p("AthOrderId", athOrderInfo.getAthOrderId());

            R<?> closeR = feeCenterRemoteOrderService.orderClose(CloseOrderReq.builder()
                    .orderCode(athOrderInfo.getAthOrderId())
                    .tid(orderEntity.getTenantId())
                    .build());
            if (closeR.getCode() == R.ok().getCode()) {
                kvLogger.i();
            } else {
                kvLogger.p(LogFieldConstants.ERR_CODE, closeR.getCode())
                        .p(LogFieldConstants.ERR_MSG, closeR.getMsg())
                        .i();
                return closeR;
            }
        }
        return R.ok();
    }

    @Slave
    @Override
    public AthOrderInfoEntity getAthOrderInfo(AthOrderInfoEntity entity) {
        return getOne(Wrappers.lambdaQuery(AthOrderInfoEntity.class)
                .eq(AthOrderInfoEntity::getPaymentOrderId, entity.getPaymentOrderId())
                .eq(AthOrderInfoEntity::getTenantId, entity.getTenantId()));
    }
}
