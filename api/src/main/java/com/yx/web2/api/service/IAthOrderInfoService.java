package com.yx.web2.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.entity.AthOrderInfoEntity;
import com.yx.web2.api.entity.OrderEntity;
import org.yx.lib.utils.util.R;

public interface IAthOrderInfoService extends IService<AthOrderInfoEntity> {
    void saveAthOrderInfo(AthOrderInfoEntity entity);

    R<?> closeOrder(AccountModel accountModel, OrderEntity orderEntity);

    AthOrderInfoEntity getAthOrderInfo(AthOrderInfoEntity entity);
}
