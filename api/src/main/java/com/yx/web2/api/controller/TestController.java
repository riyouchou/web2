package com.yx.web2.api.controller;

import com.yx.web2.api.common.enums.AccountType;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.common.req.contract.ContractAuditReq;
import com.yx.web2.api.service.IContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yx.lib.utils.util.R;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {
    private final IContractService contractService;

    @GetMapping("/start")
    public R<?> testStart() {
        ContractAuditReq contractAuditReq = new ContractAuditReq();
        contractAuditReq.setContractId("Aethir-2410281730111523");
        contractAuditReq.setPass(true);

        AccountModel accountModel = new AccountModel();
        accountModel.setAccountId(174L);
        accountModel.setAccountName("siyuan.sun@sunwayig.com");
        accountModel.setAccountName("siyuan.sun@sunwayig.com");
        accountModel.setTenantId(10000L);
        accountModel.setTenantName("admin");
        accountModel.setTenantType("GP");
        accountModel.setBdAccountId(174L);
        accountModel.setBdAccountName("siyuan.sun@sunwayig.com");
        accountModel.setAccountType(AccountType.FINANCE.getValue());

        return contractService.review(1000L, accountModel, contractAuditReq);
    }
}
