package com.yx.web2.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yx.web2.api.entity.SubscribedServiceEntity;
import com.yx.web2.api.entity.TenantAthTransferRecordEntity;

import java.util.List;

public interface ISubscribedServiceService extends IService<SubscribedServiceEntity> {
    void saveSubscribedRecord(SubscribedServiceEntity entity);

    List<SubscribedServiceEntity> listServiceEndSubscribedList();

    List<SubscribedServiceEntity> listInServiceSubscribedList();
}
