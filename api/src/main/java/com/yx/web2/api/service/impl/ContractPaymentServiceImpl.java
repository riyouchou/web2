package com.yx.web2.api.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yx.web2.api.entity.ContractPaymentEntity;
import com.yx.web2.api.mapper.ContractPaymentMapper;
import com.yx.web2.api.service.IContractPaymentService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContractPaymentServiceImpl extends ServiceImpl<ContractPaymentMapper, ContractPaymentEntity> implements IContractPaymentService {
    @Override
    public List<ContractPaymentEntity> list(String contractId) {
        return list(Wrappers.lambdaQuery(ContractPaymentEntity.class).eq(ContractPaymentEntity::getContractId, contractId));
    }
}
