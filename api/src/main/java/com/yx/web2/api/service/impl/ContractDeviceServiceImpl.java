package com.yx.web2.api.service.impl;

import com.baomidou.dynamic.datasource.annotation.Slave;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yx.web2.api.entity.ContractDeviceEntity;
import com.yx.web2.api.mapper.ContractDeviceMapper;
import com.yx.web2.api.service.IContractDeviceService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContractDeviceServiceImpl extends ServiceImpl<ContractDeviceMapper, ContractDeviceEntity> implements IContractDeviceService {
    @Override
    @Slave
    public List<ContractDeviceEntity> list(String contractId) {
        return list(Wrappers.lambdaQuery(ContractDeviceEntity.class).eq(ContractDeviceEntity::getContractId, contractId));
    }
}
