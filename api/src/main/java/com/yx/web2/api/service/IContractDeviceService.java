package com.yx.web2.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yx.web2.api.entity.ContractDeviceEntity;
import com.yx.web2.api.entity.OrderEntity;

import java.util.List;

public interface IContractDeviceService extends IService<ContractDeviceEntity> {
    List<ContractDeviceEntity> list(String contractId);
}
