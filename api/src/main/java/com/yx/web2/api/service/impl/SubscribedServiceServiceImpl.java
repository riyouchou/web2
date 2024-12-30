package com.yx.web2.api.service.impl;

import com.baomidou.dynamic.datasource.annotation.Master;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yx.web2.api.entity.SubscribedServiceEntity;
import com.yx.web2.api.mapper.SubscribedServiceMapper;
import com.yx.web2.api.service.ISubscribedServiceService;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

@Service
public class SubscribedServiceServiceImpl extends ServiceImpl<SubscribedServiceMapper, SubscribedServiceEntity> implements ISubscribedServiceService {

    @Master
    @Override
    public void saveSubscribedRecord(SubscribedServiceEntity entity) {
        LambdaQueryWrapper<SubscribedServiceEntity> wrappers = Wrappers.lambdaQuery(SubscribedServiceEntity.class);
        wrappers.eq(SubscribedServiceEntity::getOrderId, entity.getOrderId())
                .eq(SubscribedServiceEntity::getPaymentOrderId, entity.getPaymentOrderId())
                .last(" LIMIT 1 ");
        if (getOne(wrappers) == null) {
            this.save(entity);
        }
    }

    @Override
    public List<SubscribedServiceEntity> listServiceEndSubscribedList() {
        LambdaQueryWrapper<SubscribedServiceEntity> wrappers = Wrappers.lambdaQuery(SubscribedServiceEntity.class);
        wrappers.lt(SubscribedServiceEntity::getServiceEndTime, new Timestamp(System.currentTimeMillis()));
        return baseMapper.listServiceEndSubscribedList(wrappers);
    }

    @Override
    public List<SubscribedServiceEntity> listInServiceSubscribedList() {
        LambdaQueryWrapper<SubscribedServiceEntity> wrappers = Wrappers.lambdaQuery(SubscribedServiceEntity.class);
        wrappers.ge(SubscribedServiceEntity::getServiceEndTime, new Timestamp(System.currentTimeMillis()));
        return baseMapper.listServiceEndSubscribedList(wrappers);
    }
}
