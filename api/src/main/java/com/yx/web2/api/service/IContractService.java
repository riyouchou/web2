package com.yx.web2.api.service;

import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.common.req.contract.ContractAuditReq;
import com.yx.web2.api.common.req.contract.ContractQueryReq;
import com.yx.web2.api.common.req.contract.CreateOrUpdateContractReq;
import com.yx.web2.api.common.resp.PageResp;
import com.yx.web2.api.common.resp.contract.*;
import com.yx.web2.api.entity.ContractEntity;
import org.springframework.http.ResponseEntity;
import org.yx.lib.utils.util.R;

import java.util.List;

public interface IContractService extends IService<ContractEntity> {
    R<CreateContractResp> create(Long tenantId, AccountModel accountModel, CreateOrUpdateContractReq createContractReq);

    R<CreateContractResp> update(Long tenantId, AccountModel accountModel, CreateOrUpdateContractReq updateContractReq);

    R<PageResp<ContractListResp>> contractList(Long tenantId, AccountModel accountModel, ContractQueryReq contractQueryReq);

    R<ContractDetailResp> detail(Long tenantId, AccountModel accountModel, String contractId);

    R<?> review(Long tenantId, AccountModel accountModel, ContractAuditReq contractAuditReq);

    R<?> delete(Long tenantId, AccountModel accountModel, String contractId);

    R<ContractPreviewUrlResp> getPreviewUrl(Long tenantId, AccountModel accountModel, String contractId);

    ResponseEntity<?> download(Long tenantId, AccountModel accountModel, String contractId);

    ResponseEntity<?> signed(String envelopeId, String recipientId, DateTime signedTime);

    String contractToOrder(ContractEntity contractEntity);

    ContractEntity getContractByOrderId(String orderId);

    R<ContractTenantSignResp> getTenantSignedCount(Long tenantId);

    List<ContractEntity> findContractsWithoutMatchingOrders(String startDate, String endDate);

    R<List<String>> getCountries();
}
