package com.yx.web2.api.controller;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Validator;
import com.yx.web2.api.common.constant.RequestAttributeConstants;
import com.yx.web2.api.common.enums.AccountType;
import com.yx.web2.api.common.enums.ServiceDurationPeriod;
import com.yx.web2.api.common.enums.SysCode;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.common.req.contract.ContractAuditReq;
import com.yx.web2.api.common.req.contract.ContractIdReq;
import com.yx.web2.api.common.req.contract.ContractQueryReq;
import com.yx.web2.api.common.req.contract.CreateOrUpdateContractReq;
import com.yx.web2.api.common.resp.PageResp;
import com.yx.web2.api.common.resp.contract.*;
import com.yx.web2.api.service.IContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.yx.lib.utils.util.R;
import org.yx.lib.utils.util.StringUtil;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/contract")
@RequiredArgsConstructor
public class ContractController {

    private final IContractService contractService;

    @PostMapping("/create")
    public R<CreateContractResp> create(
            @RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId,
            @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel,
            @RequestBody CreateOrUpdateContractReq createContractReq) {
        // ai-bd can operate
//        if (accountModel.getAccountType().intValue() != AccountType.AI_BD.getValue()) {
//            return R.failed(SysCode.x00000403.getValue(), SysCode.x00000403.getMsg());
//        }
        if (createContractReq.getContractTenantId() == null ||
                StringUtil.isBlank(createContractReq.getContractTenantName()) ||
                StringUtil.isBlank(createContractReq.getCustomerLegalEntityName()) ||
                StringUtil.isBlank(createContractReq.getCustomerRegistrationNumber()) ||
                StringUtil.isBlank(createContractReq.getCustomerLegalEntityAddress()) ||
                StringUtil.isBlank(createContractReq.getSignerName()) ||
                StringUtil.isBlank(createContractReq.getSignerEmail()) ||
                StringUtil.isBlank(createContractReq.getStartedTime())) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        try {
            DateUtil.parseDate(createContractReq.getStartedTime());
        } catch (Exception ex) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        if (createContractReq.getServiceDuration() == null || createContractReq.getServiceDuration() <= 0 ||
                createContractReq.getServiceDurationPeriod() == null ||
                createContractReq.getServiceDurationPeriod() < ServiceDurationPeriod.Day.getValue() ||
                createContractReq.getServiceDurationPeriod() > ServiceDurationPeriod.Year.getValue()) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        BigDecimal defaultBigDecimal = new BigDecimal(0);
        if (StringUtil.isBlank(createContractReq.getPrePaymentPrice()) ||
                Convert.toBigDecimal(createContractReq.getPrePaymentPrice(), defaultBigDecimal).doubleValue() == defaultBigDecimal.doubleValue()) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        if (createContractReq.getDevices() == null || createContractReq.getDevices().isEmpty()) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        if (createContractReq.getDevices().stream().anyMatch(item ->
                StringUtil.isBlank(item.getRegionCode()) || item.getQuantity() == null || item.getQuantity() <= 0 ||
                        StringUtil.isBlank(item.getUnitPrice()) || StringUtil.isBlank(item.getDiscountPrice()) ||
                        StringUtil.isBlank(item.getDeployRegionCode()) || StringUtil.isBlank(item.getSpec()))) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        if (createContractReq.getCustomerRegistrationNumber().length() > 256 ||
                createContractReq.getCustomerLegalEntityAddress().length() > 256) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        if (createContractReq.getSignerName().length() > 100) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        if (createContractReq.getCustomerLegalEntityName().length() > 128 ||
                createContractReq.getSignerEmail().length() > 128) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        if (!Validator.isEmail(createContractReq.getSignerEmail())) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        return contractService.create(tenantId, accountModel, createContractReq);
    }

    @PostMapping("/update")
    public R<CreateContractResp> update(
            @RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId,
            @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel,
            @RequestBody CreateOrUpdateContractReq updateContractReq) {
        // ai-bd can operate
        if (accountModel.getAccountType().intValue() != AccountType.AI_BD.getValue()) {
            return R.failed(SysCode.x00000403.getValue(), SysCode.x00000403.getMsg());
        }
        if (StringUtil.isBlank(updateContractReq.getContractId()) ||
                updateContractReq.getContractTenantId() == null ||
                StringUtil.isBlank(updateContractReq.getContractTenantName()) ||
                StringUtil.isBlank(updateContractReq.getCustomerLegalEntityName()) ||
                StringUtil.isBlank(updateContractReq.getCustomerRegistrationNumber()) ||
                StringUtil.isBlank(updateContractReq.getCustomerLegalEntityAddress()) ||
                StringUtil.isBlank(updateContractReq.getSignerName()) ||
                StringUtil.isBlank(updateContractReq.getSignerEmail()) ||
                StringUtil.isBlank(updateContractReq.getStartedTime())) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        try {
            DateUtil.parseDate(updateContractReq.getStartedTime());
        } catch (Exception ex) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        if (updateContractReq.getServiceDuration() == null || updateContractReq.getServiceDuration() <= 0 ||
                updateContractReq.getServiceDurationPeriod() == null ||
                updateContractReq.getServiceDurationPeriod() < ServiceDurationPeriod.Day.getValue() ||
                updateContractReq.getServiceDurationPeriod() > ServiceDurationPeriod.Year.getValue()) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        BigDecimal defaultBigDecimal = new BigDecimal(0);
        if (StringUtil.isBlank(updateContractReq.getPrePaymentPrice()) ||
                Convert.toBigDecimal(updateContractReq.getPrePaymentPrice(), defaultBigDecimal).doubleValue() == defaultBigDecimal.doubleValue()) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        if (updateContractReq.getDevices() == null || updateContractReq.getDevices().isEmpty()) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        if (updateContractReq.getDevices().stream().anyMatch(item ->
                StringUtil.isBlank(item.getRegionCode()) || item.getQuantity() == null || item.getQuantity() <= 0 ||
                        StringUtil.isBlank(item.getUnitPrice()) || StringUtil.isBlank(item.getDiscountPrice()) ||
                        StringUtil.isBlank(item.getDeployRegionCode()) || StringUtil.isBlank(item.getSpec()))) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        if (updateContractReq.getCustomerRegistrationNumber().length() > 256 ||
                updateContractReq.getCustomerLegalEntityAddress().length() > 256) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        if (updateContractReq.getCustomerLegalEntityName().length() > 128 ||
                updateContractReq.getSignerEmail().length() > 128 ||
                updateContractReq.getSignerName().length() > 128) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        if (!Validator.isEmail(updateContractReq.getSignerEmail())) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        return contractService.update(tenantId, accountModel, updateContractReq);
    }

    @PostMapping("/list")
    public R<PageResp<ContractListResp>> contractList(
            @RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId,
            @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel,
            @RequestBody ContractQueryReq contractQueryReq) {
        return contractService.contractList(tenantId, accountModel, contractQueryReq);
    }

    @GetMapping("/detail")
    public R<ContractDetailResp> detail(@RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId,
                                        @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel,
                                        @RequestParam(value = "contractId") String contractId) {
        return contractService.detail(tenantId, accountModel, contractId);
    }

    @PostMapping("/review")
    public R<?> review(@RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId,
                       @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel,
                       @RequestBody ContractAuditReq contractAuditReq) {
        // finance or admin can operate
        if (accountModel.getAccountType().intValue() != AccountType.FINANCE.getValue() &&
                accountModel.getAccountType().intValue() != AccountType.Admin.getValue()) {
            return R.failed(SysCode.x00000403.getValue(), SysCode.x00000403.getMsg());
        }
        if (StringUtil.isBlank(contractAuditReq.getContractId())) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        if (!contractAuditReq.isPass() && StringUtil.isBlank(contractAuditReq.getRejectMsg())) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        if (StringUtil.isNotBlank(contractAuditReq.getRejectMsg()) && contractAuditReq.getRejectMsg().length() > 500) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        return contractService.review(tenantId, accountModel, contractAuditReq);
    }

    @PostMapping("/delete")
    public R<?> delete(@RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId,
                       @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel,
                       @RequestBody ContractIdReq contractIdReq) {
        if (StringUtil.isBlank(contractIdReq.getContractId())) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        return contractService.delete(tenantId, accountModel, contractIdReq.getContractId());
    }

    @GetMapping("/getPreviewUrl")
    public R<ContractPreviewUrlResp> getPreviewUrl(@RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId,
                                                   @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel,
                                                   @RequestParam(value = "contractId") String contractId) {
        return contractService.getPreviewUrl(tenantId, accountModel, contractId);
    }

    @GetMapping("/download")
    public ResponseEntity<?> download(@RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId,
                                      @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel,
                                      @RequestParam(value = "contractId") String contractId) {
        return contractService.download(tenantId, accountModel, contractId);
    }

    @GetMapping("/getTenantSignedCount")
    public R<ContractTenantSignResp> getTenantSignedCount(@RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId,
                                                          @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel,
                                                          @RequestParam(value = "tid") Long tid) {
        return contractService.getTenantSignedCount(tid);
    }

    @GetMapping("/getCountries")
    public R<List<String>> getCountries() {
        return contractService.getCountries();
    }
}
