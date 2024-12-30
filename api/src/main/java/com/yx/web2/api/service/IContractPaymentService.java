package com.yx.web2.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yx.web2.api.entity.ContractPaymentEntity;

import java.util.List;

public interface IContractPaymentService extends IService<ContractPaymentEntity> {
    List<ContractPaymentEntity> list(String contractId);
}
