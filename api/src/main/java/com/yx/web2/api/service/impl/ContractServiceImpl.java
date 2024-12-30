package com.yx.web2.api.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapBuilder;
import com.alibaba.fastjson.JSON;
import com.baomidou.dynamic.datasource.annotation.Master;
import com.baomidou.dynamic.datasource.annotation.Slave;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.docusign.esign.client.ApiException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.yx.web2.api.common.constant.CacheConstants;
import com.yx.web2.api.common.constant.Web2LoggerEvents;
import com.yx.web2.api.common.enums.*;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.common.req.contract.ContractAuditReq;
import com.yx.web2.api.common.req.contract.ContractQueryReq;
import com.yx.web2.api.common.req.contract.CreateOrUpdateContractReq;
import com.yx.web2.api.common.resp.PageResp;
import com.yx.web2.api.common.resp.contract.*;
import com.yx.web2.api.common.web3api.Web3BasicDataService;
import com.yx.web2.api.config.DocusignConfig;
import com.yx.web2.api.config.Web2ApiConfig;
import com.yx.web2.api.entity.ContractDeviceEntity;
import com.yx.web2.api.entity.ContractEntity;
import com.yx.web2.api.entity.ContractPaymentEntity;
import com.yx.web2.api.mapper.ContractMapper;
import com.yx.web2.api.service.*;
import com.yx.web2.api.service.docusign.DocuSignUnAuthorizationException;
import com.yx.web2.api.service.docusign.DocusignService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.yx.lib.utils.constant.CommonConstants;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.logger.LogFieldConstants;
import org.yx.lib.utils.token.YxTokenBuilderUtil;
import org.yx.lib.utils.util.R;
import org.yx.lib.utils.util.SpringContextHolder;
import org.yx.lib.utils.util.StringUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContractServiceImpl extends ServiceImpl<ContractMapper, ContractEntity> implements IContractService {
    private final IContractDeviceService contractDeviceService;
    private final IContractPaymentService contractPaymentService;
    private final InstallmentCalculateService installmentCalculateService;
    private final Web3BasicDataService web3BasicDataService;
    private final DocusignService docusignService;
    private final Web2ApiConfig web2ApiConfig;
    private final DocusignConfig docusignConfig;
    private final IOrderService orderService;
    private final RedisTemplate<String, String> redisTemplate;
    private final String ContractFullTemplateName = "Aethir_Earth_MSA_Full.docx";
    private final String ContractFullWithoutFreeTemplateName = "Aethir_Earth_MSA_Full_Without_Free.docx";
    private final String ContractSmartTemplateName = "Aethir_Earth_MSA_Smart.docx";
    private final String ContractSmartWithoutFreeTemplateName = "Aethir_Earth_MSA_Smart_Without_Free.docx";

    @Value("classpath:/contract/template/Aethir_Earth_MSA_Full.docx")
    private Resource Contract_Aethir_Earth_MSA_Full_Doc;

    @Value("classpath:/contract/template/Aethir_Earth_MSA_Full_Without_Free.docx")
    private Resource Contract_Aethir_Earth_MSA_Full_WITHOUT_FREE_Doc;

    @Value("classpath:/contract/template/Aethir_Earth_MSA_Smart.docx")
    private Resource Contract_Aethir_Earth_MSA_Smart_Doc;

    @Value("classpath:/contract/template/Aethir_Earth_MSA_Smart_Without_Free.docx")
    private Resource Contract_Aethir_Earth_MSA_Smart_Without_Free_Doc;

    @Override
    @Master
    @Transactional(rollbackFor = Exception.class)
    public R<CreateContractResp> create(Long tenantId, AccountModel accountModel, CreateOrUpdateContractReq createContractReq) {
        String contractId = OrderIdGenerate.generateSimpleOrderId("");
        // basic data
        ContractEntity contractEntity = ContractEntity.builder()
                .contractId(contractId)
                .tenantId(createContractReq.getContractTenantId())
                .tenantName(createContractReq.getContractTenantName())
                .customerLegalEntityName(createContractReq.getCustomerLegalEntityName())
                .customerRegistrationNumber(createContractReq.getCustomerRegistrationNumber())
                .customerLegalEntityAddress(createContractReq.getCustomerLegalEntityAddress())
                .customerCountry(createContractReq.getCustomerCountry())
                .signerName(createContractReq.getSignerName())
                .signerEmail(createContractReq.getSignerEmail())
                .startedTime(DateUtil.parseDate(createContractReq.getStartedTime()).toTimestamp())
                .freeServiceTermDays(createContractReq.getFreeServiceTermDays())
                .prePaymentPrice(new BigDecimal(createContractReq.getPrePaymentPrice()).setScale(2, RoundingMode.UP))
                .serviceDuration(createContractReq.getServiceDuration())
                .serviceDurationPeriod(createContractReq.getServiceDurationPeriod())
                .bdAccountId(accountModel.getAccountId())
                .bdAccountName(accountModel.getAccountName())
                .contractStatus(ContractStatus.PendingApprove.getValue())
                .createTime(new Timestamp(System.currentTimeMillis()))
                .lastUpdateAccountId(accountModel.getAccountId())
                .deleted(false)
                .build();
        this.save(contractEntity);
        // device data
        List<ContractDeviceEntity> deviceEntities = Lists.newArrayList();

        InstallmentCalculateService.InstallmentInput input = new InstallmentCalculateService.InstallmentInput();
        List<InstallmentCalculateService.InstallmentInput.InstallmentInputDetail> detailList = Lists.newArrayList();

        for (CreateOrUpdateContractReq.ContractDevice contractDeviceReq : createContractReq.getDevices()) {
            String regionName = web3BasicDataService.getRegion(contractDeviceReq.getRegionCode());
            if (StringUtil.isBlank(regionName)) {
                return R.failed(SysCode.x00000404.getValue(), "Not found region '" + contractDeviceReq.getRegionCode() + "'");
            }
            deviceEntities.add(ContractDeviceEntity.builder()
                    .contractId(contractId)
                    .regionCode(contractDeviceReq.getRegionCode())
                    .regionName(regionName)
                    .deployRegionCode(contractDeviceReq.getDeployRegionCode())
                    .cpuInfo(contractDeviceReq.getCpuInfo())
                    .gpuInfo(contractDeviceReq.getGpuInfo())
                    .mem(contractDeviceReq.getMem())
                    .disk(contractDeviceReq.getDisk())
                    .spec(contractDeviceReq.getSpec())
                    .bandWidth(contractDeviceReq.getBandWidth())
                    .unitPrice(contractDeviceReq.getUnitPrice())
                    .discountPrice(contractDeviceReq.getDiscountPrice())
                    .quantity(contractDeviceReq.getQuantity())
                    .createTime(new Timestamp(System.currentTimeMillis()))
                    .deviceInfo(contractDeviceReq.getDeviceInfo())
                    .build());

            detailList.add(
                    input.new InstallmentInputDetail(contractDeviceReq.getQuantity(), contractDeviceReq.getDiscountPrice()));
        }
        contractDeviceService.saveBatch(deviceEntities);
        // payment data
        input.setServiceDuration(createContractReq.getServiceDuration());
        input.setServiceDurationPeriod(createContractReq.getServiceDurationPeriod());
        input.setFreeServiceTermHours(createContractReq.getFreeServiceTermDays() == null ? 0 : createContractReq.getFreeServiceTermDays() * 24);
        input.setPrePaymentPrice(new BigDecimal(createContractReq.getPrePaymentPrice()));
        input.setInputDetails(detailList);
        // installment calculate
        InstallmentCalculateService.InstallmentOutput output = installmentCalculateService.calculate(input);
        List<ContractPaymentEntity> contractPaymentEntities = Lists.newArrayList();
        for (InstallmentCalculateService.InstallmentOutput.InstallmentOutputDetail detail : output.getOutputDetails()) {
            contractPaymentEntities.add(ContractPaymentEntity.builder()
                    .contractId(contractId)
                    .instalmentMonthTotal(output.getTotalInstalmentCount())
                    .instalmentMonth(detail.getMonth())
                    .hpPrice(detail.getHpPrice())
                    .hpPrePaymentPrice(detail.getHpPrePaymentPrice())
                    .prePayment(detail.isPrePayment())
                    .build());
        }
        // update contract amount, avgAmount
        this.update(Wrappers.lambdaUpdate(ContractEntity.class)
                .set(ContractEntity::getAmount, output.getTotalAmount())
                .set(ContractEntity::getAvgAmount, output.getContractAvgAmount())
                .eq(ContractEntity::getId, contractEntity.getId()));
        contractPaymentService.saveBatch(contractPaymentEntities);

        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_CREATE)
                .p(LogFieldConstants.TENANT_ID, contractEntity.getTenantId())
                .p("ContractId", contractEntity.getContractId())
                .p("TenantName", contractEntity.getTenantName())
                .p("BdAccountId", accountModel.getAccountId())
                .p("BdAccountName", accountModel.getAccountName())
                .p("ContractStatus", ContractStatus.PendingApprove.getName())
                .p("ContractInfo", JSON.toJSONString(createContractReq))
                .p("InstallmentInfo", JSON.toJSONString(contractPaymentEntities))
                .i();

        CreateContractResp resp = new CreateContractResp();
        resp.setContractId(contractId);
        return R.ok(resp);
    }

    @Override
    @Master
    @Transactional(rollbackFor = Exception.class)
    public R<CreateContractResp> update(Long tenantId, AccountModel accountModel, CreateOrUpdateContractReq updateContractReq) {
        ContractEntity contractEntity = getContractByBdAccountId(updateContractReq.getContractId(), accountModel.getAccountId());
        if (contractEntity == null) {
            return R.failed(SysCode.x00000404.getValue(), SysCode.x00000404.getMsg());
        }

        if (contractEntity.getContractStatus().intValue() != ContractStatus.PendingApprove.getValue()) {
            return R.failed(SysCode.x00000405.getValue(), "Contract status is not pending approve");
        }
        // basic data
        contractEntity.setTenantId(updateContractReq.getContractTenantId());
        contractEntity.setTenantName(updateContractReq.getContractTenantName());
        contractEntity.setCustomerLegalEntityName(updateContractReq.getCustomerLegalEntityName());
        contractEntity.setCustomerRegistrationNumber(updateContractReq.getCustomerRegistrationNumber());
        contractEntity.setCustomerLegalEntityAddress(updateContractReq.getCustomerLegalEntityAddress());
        contractEntity.setCustomerCountry(updateContractReq.getCustomerCountry());
        contractEntity.setSignerName(updateContractReq.getSignerName());
        contractEntity.setSignerEmail(updateContractReq.getSignerEmail());
        contractEntity.setStartedTime(DateUtil.parseDate(updateContractReq.getStartedTime()).toTimestamp());
        contractEntity.setFreeServiceTermDays(updateContractReq.getFreeServiceTermDays());
        contractEntity.setPrePaymentPrice(new BigDecimal(updateContractReq.getPrePaymentPrice()).setScale(2, RoundingMode.UP));
        contractEntity.setServiceDuration(updateContractReq.getServiceDuration());
        contractEntity.setServiceDurationPeriod(updateContractReq.getServiceDurationPeriod());
        contractEntity.setLastUpdateAccountId(accountModel.getAccountId());
        this.updateById(contractEntity);

        // device data
        List<ContractDeviceEntity> deviceEntities = Lists.newArrayList();

        InstallmentCalculateService.InstallmentInput input = new InstallmentCalculateService.InstallmentInput();
        List<InstallmentCalculateService.InstallmentInput.InstallmentInputDetail> detailList = Lists.newArrayList();

        for (CreateOrUpdateContractReq.ContractDevice contractDeviceReq : updateContractReq.getDevices()) {
            String regionName = web3BasicDataService.getRegion(contractDeviceReq.getRegionCode());
            if (StringUtil.isBlank(regionName)) {
                return R.failed(SysCode.x00000404.getValue(), "Not found region '" + contractDeviceReq.getRegionCode() + "'");
            }
            deviceEntities.add(ContractDeviceEntity.builder()
                    .contractId(contractEntity.getContractId())
                    .regionCode(contractDeviceReq.getRegionCode())
                    .regionName(regionName)
                    .deployRegionCode(contractDeviceReq.getDeployRegionCode())
                    .cpuInfo(contractDeviceReq.getCpuInfo())
                    .gpuInfo(contractDeviceReq.getGpuInfo())
                    .mem(contractDeviceReq.getMem())
                    .spec(contractDeviceReq.getSpec())
                    .disk(contractDeviceReq.getDisk())
                    .bandWidth(contractDeviceReq.getBandWidth())
                    .unitPrice(contractDeviceReq.getUnitPrice())
                    .discountPrice(contractDeviceReq.getDiscountPrice())
                    .quantity(contractDeviceReq.getQuantity())
                    .createTime(new Timestamp(System.currentTimeMillis()))
                    .deviceInfo(contractDeviceReq.getDeviceInfo())
                    .build());

            detailList.add(
                    input.new InstallmentInputDetail(contractDeviceReq.getQuantity(), contractDeviceReq.getDiscountPrice()));
        }
        // remove exists data
        contractDeviceService.remove(Wrappers.lambdaQuery(ContractDeviceEntity.class).eq(ContractDeviceEntity::getContractId, contractEntity.getContractId()));
        // save new data
        contractDeviceService.saveBatch(deviceEntities);

        // payment data
        input.setServiceDuration(updateContractReq.getServiceDuration());
        input.setServiceDurationPeriod(updateContractReq.getServiceDurationPeriod());
        input.setFreeServiceTermHours(updateContractReq.getFreeServiceTermDays() == null ? 0 : updateContractReq.getFreeServiceTermDays() * 24);
        input.setPrePaymentPrice(new BigDecimal(updateContractReq.getPrePaymentPrice()));
        input.setInputDetails(detailList);
        // installment calculate
        InstallmentCalculateService.InstallmentOutput output = installmentCalculateService.calculate(input);
        List<ContractPaymentEntity> contractPaymentEntities = Lists.newArrayList();
        for (InstallmentCalculateService.InstallmentOutput.InstallmentOutputDetail detail : output.getOutputDetails()) {
            contractPaymentEntities.add(ContractPaymentEntity.builder()
                    .contractId(contractEntity.getContractId())
                    .instalmentMonthTotal(output.getTotalInstalmentCount())
                    .instalmentMonth(detail.getMonth())
                    .hpPrice(detail.getHpPrice())
                    .hpPrePaymentPrice(detail.getHpPrePaymentPrice())
                    .prePayment(detail.isPrePayment())
                    .build());
        }
        // update contract amount, avgAmount
        this.update(Wrappers.lambdaUpdate(ContractEntity.class)
                .set(ContractEntity::getAmount, output.getTotalAmount())
                .set(ContractEntity::getAvgAmount, output.getContractAvgAmount())
                .eq(ContractEntity::getId, contractEntity.getId()));

        // remove exists data
        contractPaymentService.remove(Wrappers.lambdaQuery(ContractPaymentEntity.class).eq(ContractPaymentEntity::getContractId, contractEntity.getContractId()));
        // save new data
        contractPaymentService.saveBatch(contractPaymentEntities);

        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_UPDATE)
                .p(LogFieldConstants.TENANT_ID, contractEntity.getTenantId())
                .p("ContractId", contractEntity.getContractId())
                .p("TenantName", contractEntity.getTenantName())
                .p("BdAccountId", accountModel.getAccountId())
                .p("BdAccountName", accountModel.getAccountName())
                .p("ContractStatus", ContractStatus.PendingApprove.getName())
                .p("ContractInfo", JSON.toJSONString(updateContractReq))
                .p("InstallmentInfo", JSON.toJSONString(contractPaymentEntities))
                .i();

        CreateContractResp resp = new CreateContractResp();
        resp.setContractId(updateContractReq.getContractId());
        return R.ok(resp);
    }

    @Override
    @Slave
    public R<PageResp<ContractListResp>> contractList(Long tenantId, AccountModel accountModel, ContractQueryReq contractQueryReq) {
        LambdaQueryWrapper<ContractEntity> lambdaQueryWrapper = Wrappers.lambdaQuery();
        lambdaQueryWrapper.eq(ContractEntity::isDeleted, false);
        if (contractQueryReq.getTenantId() != null) {
            if ((accountModel.getAccountType().intValue() == AccountType.AI_TENANT_USER.getValue() ||
                    accountModel.getAccountType().intValue() == AccountType.AI_TENANT_OWNER.getValue()) &&
                    contractQueryReq.getTenantId().longValue() != tenantId.longValue()) {
                return R.failed(SysCode.x00000403.getValue(), "Unauthorized query");
            }
            lambdaQueryWrapper.eq(ContractEntity::getTenantId, contractQueryReq.getTenantId());
        }
        if (contractQueryReq.getStatus() != null) {
            lambdaQueryWrapper.in(ContractEntity::getContractStatus, contractQueryReq.getStatus());
        }

        if (StringUtil.isNotBlank(contractQueryReq.getStartCreateDate())) {
            lambdaQueryWrapper.ge(ContractEntity::getCreateTime, DateUtil.parseDateTime(contractQueryReq.getStartCreateDate() + " 00:00:00"));
        }
        if (StringUtil.isNotBlank(contractQueryReq.getEndCreateDate())) {
            lambdaQueryWrapper.le(ContractEntity::getCreateTime, DateUtil.parseDateTime(contractQueryReq.getEndCreateDate() + " 23:59:59"));
        }
        if (accountModel.getAccountType().intValue() == AccountType.AI_BD.getValue()) {
            lambdaQueryWrapper.eq(ContractEntity::getBdAccountId, accountModel.getAccountId());
        }
        IPage<ContractEntity> page = new Page<>(contractQueryReq.getCurrent(), contractQueryReq.getSize());
        page.orders().add(OrderItem.desc("id"));
        this.page(page, lambdaQueryWrapper);

        List<ContractListResp> resultList = new ArrayList<>();
        page.getRecords().forEach(contract -> {
            ContractListResp resp = ContractListResp.builder()
                    .contractId(contract.getContractId())
                    .contractTenantId(contract.getTenantId())
                    .contractStatus(contract.getContractStatus())
                    .accountName(contract.getTenantName())
                    .bdName(contract.getBdAccountName())
                    .orderId(contract.getOrderId())
                    .amount(contract.getAmount() != null ? contract.getAmount().toString() : null)
                    .description(contract.getRejectMsg())
                    .period(DateUtil.formatDate(contract.getStartedTime()))
                    .startTime(DateUtil.formatDate(contract.getStartedTime()))
                    .createTime(DateUtil.formatDateTime(contract.getCreateTime()))
                    .build();
            resultList.add(resp);
        });
        PageResp<ContractListResp> resultData = new PageResp<>();
        resultData.setCurrent(contractQueryReq.getCurrent());
        resultData.setSize(contractQueryReq.getSize());
        resultData.setRecords(resultList);
        resultData.setTotal(page.getTotal());
        resultData.setPages(page.getPages());
        return R.ok(resultData);
    }

    @Override
    @Slave
    public R<ContractDetailResp> detail(Long tenantId, AccountModel accountModel, String contractId) {
        ContractEntity contractEntity;
        if (accountModel.getAccountType().intValue() == AccountType.AI_BD.getValue()) {
            contractEntity = getContractByBdAccountId(contractId, accountModel.getAccountId());
        } else {
            contractEntity = getContract(contractId);
        }
        if (contractEntity == null) {
            return R.failed(SysCode.x00000404.getValue(), SysCode.x00000404.getMsg());
        }
        List<ContractDeviceEntity> deviceEntities = contractDeviceService.list(
                Wrappers.lambdaQuery(ContractDeviceEntity.class).eq(ContractDeviceEntity::getContractId, contractId));

        ContractDetailResp resp = new ContractDetailResp();
        resp.setContractId(contractEntity.getContractId());
        resp.setContractStatus(contractEntity.getContractStatus());
        resp.setContractTenantId(contractEntity.getTenantId());
        resp.setContractTenantName(contractEntity.getTenantName());
        resp.setCustomerRegistrationNumber(contractEntity.getCustomerRegistrationNumber());
        resp.setCustomerLegalEntityAddress(contractEntity.getCustomerLegalEntityAddress());
        resp.setCustomerLegalEntityName(contractEntity.getCustomerLegalEntityName());
        resp.setCustomerCountry(contractEntity.getCustomerCountry());
        resp.setSignerName(contractEntity.getSignerName());
        resp.setSignerEmail(contractEntity.getSignerEmail());
        resp.setStartedTime(DateUtil.formatDate(contractEntity.getStartedTime()));
        resp.setFreeServiceTermDays(contractEntity.getFreeServiceTermDays());
        resp.setAmount(contractEntity.getAmount().toString());
        resp.setPrePaymentPrice(contractEntity.getPrePaymentPrice().toString());
        resp.setServiceDuration(contractEntity.getServiceDuration());
        resp.setServiceDurationPeriod(contractEntity.getServiceDurationPeriod());

        List<ContractDetailResp.ContractDevice> deviceList = Lists.newArrayList();
        for (ContractDeviceEntity deviceEntity : deviceEntities) {
            ContractDetailResp.ContractDevice deviceResp = new ContractDetailResp.ContractDevice();
            deviceResp.setId(deviceEntity.getId());
            deviceResp.setRegionCode(deviceEntity.getRegionCode());
            deviceResp.setRegionName(deviceEntity.getRegionName());
            deviceResp.setBandWidth(deviceEntity.getBandWidth());
            deviceResp.setCpuInfo(deviceEntity.getCpuInfo());
            deviceResp.setGpuInfo(deviceEntity.getGpuInfo());
            deviceResp.setMem(deviceEntity.getMem());
            deviceResp.setDisk(deviceEntity.getDisk());
            deviceResp.setQuantity(deviceEntity.getQuantity());
            deviceResp.setUnitPrice(deviceEntity.getUnitPrice());
            deviceResp.setDiscountPrice(deviceEntity.getDiscountPrice());
            deviceResp.setSpec(deviceEntity.getSpec());
            deviceResp.setDeployRegionCode(deviceEntity.getDeployRegionCode());
            deviceResp.setDeviceInfo(deviceEntity.getDeviceInfo());
            deviceList.add(deviceResp);
        }
        resp.setDevices(deviceList);
        return R.ok(resp);
    }

    @Override
    @Master
    @Transactional
    public R<?> review(Long tenantId, AccountModel accountModel, ContractAuditReq contractAuditReq) {
        String idempotentKey = String.format(CacheConstants.CONTRACT_IDEMPOTENT, contractAuditReq.getContractId());
        Boolean isNotIdempotent = redisTemplate.opsForValue().setIfAbsent(
                idempotentKey, contractAuditReq.getContractId(), Duration.ofSeconds(web2ApiConfig.getContract().getIdempotentInterval()));
        if (isNotIdempotent == null || !isNotIdempotent) {
            return R.failed(SysCode.x00000443.getValue(), SysCode.x00000443.getMsg());
        }
        try {
            ContractEntity contractEntity = getContract(contractAuditReq.getContractId());
            if (contractEntity == null) {
                return R.failed(SysCode.x00000404.getValue(), SysCode.x00000404.getMsg());
            }
            if (contractEntity.getContractStatus().intValue() != ContractStatus.PendingApprove.getValue()) {
                return R.failed(SysCode.x00000405.getValue(), "The Contract status is not pending approve");
            }
            if (!contractAuditReq.isPass()) {
                // reject
                this.update(Wrappers.lambdaUpdate(ContractEntity.class)
                        .set(ContractEntity::getRejectMsg, contractAuditReq.getRejectMsg())
                        .set(ContractEntity::getLastUpdateTime, new Timestamp(System.currentTimeMillis()))
                        .set(ContractEntity::getLastUpdateAccountId, accountModel.getAccountId())
                        .set(ContractEntity::getFinanceAccountId, accountModel.getAccountId())
                        .set(ContractEntity::getFinanceAccountName, accountModel.getAccountName())
                        .eq(ContractEntity::getId, contractEntity.getId()));
            } else {
                this.update(Wrappers.lambdaUpdate(ContractEntity.class)
                        .set(ContractEntity::getContractStatus, ContractStatus.PendingSign.getValue())
                        .set(ContractEntity::getLastUpdateTime, new Timestamp(System.currentTimeMillis()))
                        .set(ContractEntity::getLastUpdateAccountId, accountModel.getAccountId())
                        .set(ContractEntity::getFinanceAccountId, accountModel.getAccountId())
                        .set(ContractEntity::getFinanceAccountName, accountModel.getAccountName())
                        .eq(ContractEntity::getId, contractEntity.getId()));
                // create contract
                List<ContractDeviceEntity> deviceEntities = contractDeviceService.list(contractEntity.getContractId());
                List<ContractPaymentEntity> paymentEntities = contractPaymentService.list(contractEntity.getContractId());
                try {
                    // create contract
                    createTenantContract(contractEntity, deviceEntities, paymentEntities);
                    // update contract
                    update(Wrappers.lambdaUpdate(ContractEntity.class)
                            .set(ContractEntity::getEnvelopeId, contractEntity.getEnvelopeId())
                            .set(ContractEntity::getDocumentId, contractEntity.getDocumentId())
                            .set(ContractEntity::getLastUpdateTime, new Timestamp(System.currentTimeMillis()))
                            .eq(ContractEntity::getId, contractEntity.getId()));
                    KvLogger.instance(this)
                            .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                            .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_DOCUSIGN_CREATE_ENVELOPE)
                            .p("DocumentId", contractEntity.getDocumentId())
                            .p("ContractId", contractEntity.getContractId())
                            .p("EnvelopeId", contractEntity.getEnvelopeId())
                            .i();
                } catch (DocuSignUnAuthorizationException e) {
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    String authorizeUrl = "https://" + docusignConfig.getOAuthBasePath() + "/oauth/auth?response_type=code&scope=signature%20impersonation%20correct&client_id=" +
                            docusignConfig.getClientId() + "&redirect_uri=" + contractAuditReq.getContractAuthorizeCallbackUrl();
                    KvLogger.instance(this)
                            .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                            .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_DOCUSIGN_UNAUTHORIZE)
                            .p("contractId", contractEntity.getContractId())
                            .p("authorizeUrl", authorizeUrl)
                            .e(e);
                    return R.failed(MapBuilder.create(new HashMap<String, String>()).put("authorizeUrl", authorizeUrl).build(),
                            SysCode.x00000440.getValue(), SysCode.x00000440.getMsg());
                } catch (Exception e) {
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    KvLogger.instance(this)
                            .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                            .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_REVIEW)
                            .p(LogFieldConstants.TENANT_ID, contractEntity.getTenantId())
                            .p("contractId", contractEntity.getContractId())
                            .p("tenantName", contractEntity.getTenantName())
                            .p("financeAccountId", accountModel.getAccountId())
                            .p("financeAccountName", accountModel.getAccountName())
                            .p("rejectMsg", contractAuditReq.getRejectMsg())
                            .e(e);
                    return R.failed(SysCode.x00000801.getValue(), SysCode.x00000801.getMsg());
                }
            }
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_REVIEW)
                    .p(LogFieldConstants.TENANT_ID, contractEntity.getTenantId())
                    .p("contractId", contractEntity.getContractId())
                    .p("tenantName", contractEntity.getTenantName())
                    .p("financeAccountId", accountModel.getAccountId())
                    .p("financeAccountName", accountModel.getAccountName())
                    .p("contractStatus", contractAuditReq.isPass() ? ContractStatus.PendingSign.getName() : ContractStatus.PendingApprove.getName())
                    .p("rejectMsg", contractAuditReq.getRejectMsg())
                    .i();
            return R.ok();
        } catch (Exception ex) {
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_REVIEW)
                    .p("contractId", contractAuditReq.getContractId())
                    .e(ex);
            return R.failed(SysCode.x00000802.getValue(), SysCode.x00000802.getMsg());
        } finally {
            redisTemplate.delete(idempotentKey);
        }
    }

    @Override
    @Master
    public R<?> delete(Long tenantId, AccountModel accountModel, String contractId) {
        ContractEntity contractEntity = getContract(contractId);
        if (contractEntity == null) {
            return R.failed(SysCode.x00000404.getValue(), SysCode.x00000404.getMsg());
        }
        if (contractEntity.getContractStatus().intValue() != ContractStatus.PendingApprove.getValue()) {
            return R.failed(SysCode.x00000405.getValue(), "Contract status is not pending approve");
        }
        this.update(Wrappers.lambdaUpdate(ContractEntity.class)
                .set(ContractEntity::isDeleted, true)
                .set(ContractEntity::getLastUpdateTime, new Timestamp(System.currentTimeMillis()))
                .set(ContractEntity::getLastUpdateAccountId, accountModel.getAccountId())
                .eq(ContractEntity::getId, contractEntity.getId())
        );
        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_DELETE)
                .p("contractId", contractEntity.getContractId())
                .p("tenantId", contractEntity.getTenantId())
                .p("tenantName", contractEntity.getTenantName())
                .p("accountId", accountModel.getAccountId())
                .p("accountName", accountModel.getAccountName())
                .i();
        return R.ok();
    }

    @Override
    @Slave
    public R<ContractPreviewUrlResp> getPreviewUrl(Long tenantId, AccountModel accountModel, String contractId) {
        ContractEntity contractEntity = getContract(contractId);
        if (contractEntity == null) {
            return R.failed(SysCode.x00000404.getValue(), SysCode.x00000404.getMsg());
        }
        try {
            String url = docusignService.getEnvelopePreviewUrl(contractEntity.getEnvelopeId());
            ContractPreviewUrlResp resp = new ContractPreviewUrlResp();
            resp.setPreviewUrl(url);
            return R.ok(resp);
        } catch (DocuSignUnAuthorizationException docuSignUnAuthorizationException) {
            return R.failed(SysCode.x00000440.getValue(), SysCode.x00000440.getMsg());
        }
    }

    @Override
    @Slave
    public ResponseEntity<?> download(Long tenantId, AccountModel accountModel, String contractId) {
        ContractEntity contractEntity = getContract(contractId);
        if (contractEntity == null) {
            return ResponseEntity.notFound().build();
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=" + contractEntity.getContractId() + ".pdf");
            try {
                byte[] pdfResource = docusignService.getDocument(contractEntity.getEnvelopeId(), contractEntity.getDocumentId());
                return ResponseEntity.ok()
                        .headers(headers)
                        .contentLength(pdfResource.length)
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(pdfResource);
            } catch (DocuSignUnAuthorizationException docuSignUnAuthorizationException) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(R.failed(SysCode.x00000440));
            }
        }
    }

    @Override
    @Transactional
    @Master
    public ResponseEntity<?> signed(String envelopeId, String recipientId, DateTime signedTime) {
        ContractEntity contractEntity = getContractByEnvelopeId(envelopeId);
        if (contractEntity == null) {
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_DOCUSIGN_CALLBACK_RECIPIENT_COMPLETED)
                    .p("EnvelopeId", envelopeId)
                    .p(LogFieldConstants.ERR_MSG, "not found contract by envelopeId")
                    .i();
            return ResponseEntity.ok().build();
        }
        if (StringUtil.isNotBlank(contractEntity.getOrderId())) {
            return ResponseEntity.ok().build();
        }
        try {
            SpringContextHolder.getBean(IContractService.class).update(Wrappers.lambdaUpdate(ContractEntity.class)
                    .set(ContractEntity::getContractStatus, ContractStatus.Signed.getValue())
                    .set(ContractEntity::getSignerSignTime, signedTime)
                    .set(ContractEntity::getLastUpdateTime, new Timestamp(System.currentTimeMillis()))
                    .eq(ContractEntity::getId, contractEntity.getId())
            );
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_SIGNED)
                    .p(LogFieldConstants.TENANT_ID, contractEntity.getTenantId())
                    .p("ContractId", contractEntity.getContractId())
                    .p("TenantName", contractEntity.getTenantName())
                    .p("ContractStatus", ContractStatus.Signed.getName())
                    .p("SignedTime", DateUtil.formatDateTime(signedTime))
                    .i();
            String xUser = YxTokenBuilderUtil.buildXUser(contractEntity.getTenantId(), "GP", 0L);
            MDC.put(CommonConstants.X_USER, xUser);
            // create order
            String orderId = SpringContextHolder.getBean(IContractService.class).contractToOrder(contractEntity);
            if (StringUtil.isBlank(orderId)) {
                KvLogger.instance(this)
                        .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                        .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_CREATE_ORDER_BY_CONTRACT)
                        .p(LogFieldConstants.TENANT_ID, contractEntity.getTenantId())
                        .p("OrderResourcePool", "BM")
                        .p("ContractId", contractEntity.getContractId())
                        .p(LogFieldConstants.ERR_MSG, "Not found spec")
                        .i();
                return ResponseEntity.ok().build();
            }
            // update contract, set orderId
            update(Wrappers.lambdaUpdate(ContractEntity.class)
                    .set(ContractEntity::getOrderId, orderId)
                    .set(ContractEntity::getLastUpdateTime, new Timestamp(System.currentTimeMillis()))
                    .eq(ContractEntity::getId, contractEntity.getId())
            );
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_CREATE_ORDER_BY_CONTRACT)
                    .p(LogFieldConstants.TENANT_ID, contractEntity.getTenantId())
                    .flat("orderId", orderId)
                    .p("OrderResourcePool", "BM")
                    .p("OrderStatus", OrderStatus.WaitingPayment.getName())
                    .p("ContractId", contractEntity.getContractId())
                    .i();
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_DOCUSIGN_CALLBACK_RECIPIENT_COMPLETED)
                    .p("EnvelopeId", envelopeId)
                    .p("RecipientId", recipientId)
                    .p(LogFieldConstants.ERR_MSG, ex.getMessage())
                    .e(ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public String contractToOrder(ContractEntity contractEntity) {
        List<ContractDeviceEntity> contractDeviceEntities = contractDeviceService.list(contractEntity.getContractId());
        if (contractDeviceEntities == null || contractDeviceEntities.isEmpty()) {
            throw new RuntimeException("not fount contractDevices");
        }
        List<ContractPaymentEntity> contractPaymentEntities = contractPaymentService.list(contractEntity.getContractId());
        if (contractPaymentEntities == null || contractPaymentEntities.isEmpty()) {
            throw new RuntimeException("not fount contractPayments");
        }
        return orderService.createOrderFromContract(contractEntity, contractDeviceEntities, contractPaymentEntities);
    }

    @Override
    public ContractEntity getContractByOrderId(String orderId) {
        return getOne(Wrappers.lambdaQuery(ContractEntity.class).eq(ContractEntity::getOrderId, orderId).last(" LIMIT 1"));
    }

    @Override
    @Slave
    public R<ContractTenantSignResp> getTenantSignedCount(Long tenantId) {
        return R.ok(getTenantSignedContract(tenantId));
    }

    @Override
    @Slave
    public List<ContractEntity> findContractsWithoutMatchingOrders(String startDate, String endDate) {
        return baseMapper.findContractsWithoutMatchingOrders(startDate, endDate);
    }

    @Override
    public R<List<String>> getCountries() {
        return R.ok(web2ApiConfig.getContract().getCountries());
    }

    private ContractEntity getContractByBdAccountId(String contractId, Long bdAccountId) {
        LambdaQueryWrapper<ContractEntity> queryWrapper = Wrappers.lambdaQuery(ContractEntity.class)
                .eq(ContractEntity::getContractId, contractId)
                .eq(ContractEntity::isDeleted, false)
                .eq(ContractEntity::getBdAccountId, bdAccountId);
        return getOne(queryWrapper);
    }

    private ContractEntity getContract(String contractId) {
        LambdaQueryWrapper<ContractEntity> queryWrapper = Wrappers.lambdaQuery(ContractEntity.class)
                .eq(ContractEntity::getContractId, contractId)
                .eq(ContractEntity::isDeleted, false);
        return getOne(queryWrapper);
    }

    private ContractEntity getContractByEnvelopeId(String envelopeId) {
        LambdaQueryWrapper<ContractEntity> queryWrapper = Wrappers.lambdaQuery(ContractEntity.class)
                .eq(ContractEntity::getEnvelopeId, envelopeId)
                .eq(ContractEntity::isDeleted, false);
        return getOne(queryWrapper);
    }

    /**
     * Obtain the number of containers signed by tenants
     *
     * @param tenantId tenantId
     * @return Obtain the number
     */
    private long getTenantSignedContractCount(Long tenantId) {
        return count(Wrappers.lambdaQuery(ContractEntity.class)
                .eq(ContractEntity::getTenantId, tenantId)
                .eq(ContractEntity::getContractStatus, ContractStatus.Signed.getValue()));
    }

    /**
     * Obtain number of containers and started time signed by tenants
     *
     * @date 2024/11/22 20:04
     * @param tenantId
     * @return com.yx.web2.api.common.resp.contract.ContractTenantSignResp
     */
    private ContractTenantSignResp getTenantSignedContract(Long tenantId) {
        long count = getTenantSignedContractCount(tenantId);
        String date = null;
        if (count > 0){
            ContractEntity firstContract = getOne(Wrappers.lambdaQuery(ContractEntity.class)
                    .eq(ContractEntity::getTenantId, tenantId)
                    .eq(ContractEntity::getContractStatus, ContractStatus.Signed.getValue())
                    .orderByAsc(ContractEntity::getId).last(" LIMIT 1"));
            date = DateUtil.format(firstContract.getStartedTime(), "MM/dd/yyyy");
        }
        return ContractTenantSignResp.builder()
                .count(count)
                .startedTime(date)
                .build();
    }

    /**
     * Create a contract for tenants
     *
     * @param contractEntity  ContractEntity
     * @param deviceEntities  ContractDeviceEntity list
     * @param paymentEntities ContractPaymentEntity list
     * @throws IOException,DocuSignUnAuthorizationException
     */
    private void createTenantContract(ContractEntity contractEntity, List<ContractDeviceEntity> deviceEntities, List<ContractPaymentEntity> paymentEntities)
            throws IOException, DocuSignUnAuthorizationException, ApiException {
        // get tenant already signed contract count
        long signedCount = getTenantSignedContractCount(contractEntity.getTenantId());
        // generate tenant contract file
        File tenantContractFile = generateTenantContractDocFromTemplate(contractEntity.getContractId(), contractEntity.getTenantId(), signedCount, contractEntity.getFreeServiceTermDays());
        // build tenant contract replace data
        Map<String, Object> replaceData = buildContractReplaceData(contractEntity, deviceEntities, paymentEntities, signedCount);
        // replace contract data
        replacePlaceholdersInContract(tenantContractFile, replaceData, (key, run) -> {
            Object newObj = replaceData.get(key);
            if (newObj instanceof String) {
                run.setText(replaceData.get(key).toString(), 0);
                return;
            }
            if (newObj instanceof List) {
                if (key.equals("CONTRACTDEVICEDETAILS")) {
                    String title = "GPU TYPE \u0009 REGION \u0009 COUNT  \u0009 PRICE";
                    run.setText(title, 0);
                    run.addBreak();
                    for (ContractDeviceEntity device : (List<ContractDeviceEntity>) newObj) {
                        String data = String.format("%s \u0009 %s \u0009 %s \u0009 %s$/h", device.getGpuInfo(), device.getRegionName(), device.getQuantity(),
                                Convert.toBigDecimal(device.getDiscountPrice()).setScale(2, RoundingMode.UP));
                        run.setText(data, run.getTextPosition());
                        run.addBreak();
                    }
                }
            }
        });
        Long documentId = contractEntity.getId();
        String envelopeId = docusignService.sendContract(tenantContractFile, signedCount <= 0, contractEntity.getContractId(),
                documentId.toString(), web2ApiConfig.getContract().getSendEmailDocument(),
                //web2ApiConfig.getContract().getSendEmailSubject(),
                contractEntity.getContractId(),
                contractEntity.getSignerEmail(), contractEntity.getSignerName());
        contractEntity.setEnvelopeId(envelopeId);
        contractEntity.setDocumentId(documentId.toString());
    }

    /**
     * Create a contract template for tenants
     *
     * @param contractId          contractId
     * @param tenantId            tenantId
     * @param signedCount         Obtain the number of containers signed by tenants
     * @param freeServiceTermDays Free Service Terms Day
     * @return contract template
     * @throws IOException IOException
     */
    private File generateTenantContractDocFromTemplate(String contractId, Long tenantId, Long signedCount, Integer freeServiceTermDays) throws IOException {
        Path tenantContractPath = Paths.get(web2ApiConfig.getContract().getTenantPath(), String.valueOf(tenantId));
        File tenantDir = tenantContractPath.toFile();
        if (!tenantDir.exists()) {
            boolean isSuccess = tenantDir.mkdirs();
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_CREATE_TENANT_CONTRACT_PATH)
                    .p(LogFieldConstants.TENANT_ID, tenantId)
                    .p("ContractId", contractId)
                    .p("Path", tenantContractPath.toString())
                    .p(LogFieldConstants.Success, isSuccess)
                    .i();
        }
        File tenantContractFile = Paths.get(tenantContractPath.toString(), contractId + ".docx").toFile();
        // copy tenant contract.docx from template
        if (!web2ApiConfig.getContract().isUseDefaultTemplate()) {
            File srcFile;
            if (signedCount > 0) {
                if (freeServiceTermDays != null && freeServiceTermDays > 0) {
                    srcFile = Paths.get(web2ApiConfig.getContract().getTemplatePath(), ContractSmartTemplateName).toFile();
                } else {
                    srcFile = Paths.get(web2ApiConfig.getContract().getTemplatePath(), ContractSmartWithoutFreeTemplateName).toFile();
                }
            } else {
                if (freeServiceTermDays != null && freeServiceTermDays > 0) {
                    srcFile = Paths.get(web2ApiConfig.getContract().getTemplatePath(), ContractFullTemplateName).toFile();
                } else {
                    srcFile = Paths.get(web2ApiConfig.getContract().getTemplatePath(), ContractFullWithoutFreeTemplateName).toFile();
                }
            }
            FileUtils.copyFile(srcFile, tenantContractFile);
        } else {
            if (signedCount > 0) {
                if (freeServiceTermDays != null && freeServiceTermDays > 0) {
                    FileUtils.writeByteArrayToFile(tenantContractFile, IOUtils.toByteArray(Contract_Aethir_Earth_MSA_Smart_Doc.getInputStream()));
                } else {
                    FileUtils.writeByteArrayToFile(tenantContractFile, IOUtils.toByteArray(Contract_Aethir_Earth_MSA_Smart_Without_Free_Doc.getInputStream()));
                }
            } else {
                if (freeServiceTermDays != null && freeServiceTermDays > 0) {
                    FileUtils.writeByteArrayToFile(tenantContractFile, IOUtils.toByteArray(Contract_Aethir_Earth_MSA_Full_Doc.getInputStream()));
                } else {
                    FileUtils.writeByteArrayToFile(tenantContractFile, IOUtils.toByteArray(Contract_Aethir_Earth_MSA_Full_WITHOUT_FREE_Doc.getInputStream()));
                }
            }
        }
        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_CREATE_TENANT_CONTRACT_FILE)
                .p(LogFieldConstants.TENANT_ID, tenantId)
                .p("ContractId", contractId)
                .p("File", tenantContractFile.toString())
                .p("UseDefaultTemplate", web2ApiConfig.getContract().isUseDefaultTemplate())
                .p(LogFieldConstants.Success, tenantContractFile.exists())
                .i();
        return tenantContractFile;
    }

    /**
     * Construct contract replacement parameters
     *
     * @param contractEntity  ContractEntity
     * @param deviceEntities  ContractDeviceEntity list
     * @param paymentEntities ContractPaymentEntity list
     * @param signedCount     Obtain the number of containers signed by tenants
     * @return replacement parameters
     */
    private Map<String, Object> buildContractReplaceData(ContractEntity contractEntity, List<ContractDeviceEntity> deviceEntities, List<ContractPaymentEntity> paymentEntities, Long signedCount) {
        ContractEntity firstContract = getOne(Wrappers.lambdaQuery(ContractEntity.class)
                .eq(ContractEntity::getTenantId, contractEntity.getTenantId())
                .eq(ContractEntity::getContractStatus, ContractStatus.Signed.getValue())
                .orderByAsc(ContractEntity::getId).last(" LIMIT 1"));

        // build contract prepare replace data
        Map<String, Object> replaceData = Maps.newHashMap();
        replaceData.put("REPLACECREATEDATE", wrapWithSpaces(DateUtil.format(contractEntity.getCreateTime(), "MM/dd/yyyy")));
        replaceData.put("CUSTOMERLEGALENTITYNAME", wrapWithSpaces(contractEntity.getCustomerLegalEntityName()));
        replaceData.put("CUSTOMERREGISTRATIONNUMBER", wrapWithSpaces(contractEntity.getCustomerRegistrationNumber()));
        replaceData.put("CUSTOMERLEGALENTITYADDRESS", wrapWithSpaces(contractEntity.getCustomerLegalEntityAddress()));
        replaceData.put("COUNTRY", wrapWithSpaces(contractEntity.getCustomerCountry()));
        replaceData.put("SIGNERNAME", wrapWithSpaces(contractEntity.getSignerName()));
        replaceData.put("SIGNEREMAIL", wrapWithSpaces(contractEntity.getSignerEmail()));
        replaceData.put("CONTRACTCOUNT", wrapWithSpaces(String.valueOf(signedCount + 1L)));//用户首次签订线上合同为#1，后续签订的按照整数递增
        replaceData.put("CONTRACTFIRSTSTARTDATE", wrapWithSpaces(DateUtil.format(
                firstContract == null ? contractEntity.getStartedTime() : firstContract.getStartedTime(), "MM/dd/yyyy")));//BD首次为客户创建合同的生效时间
        replaceData.put("CONTRACTSTARTDATE", wrapWithSpaces(DateUtil.format(contractEntity.getStartedTime(), "MM/dd/yyyy")));//BD填写的合同生效时间

        //根据总价及分期数量计算的每月应付账款，没有分期  就是分了一期  就显示总价，如果分期后不能均分，先不考虑最后一个月的
        List<ContractPaymentEntity> hpList = paymentEntities.stream().filter(item -> !item.getPrePayment()).collect(Collectors.toList());
        if (hpList.isEmpty()) {
            replaceData.put("HPPRICE", wrapWithSpaces(contractEntity.getAmount()));
        } else {
            replaceData.put("HPPRICE", wrapWithSpaces(contractEntity.getAvgAmount()));
        }
        replaceData.put("CPREPAYMENT", wrapWithSpaces(contractEntity.getPrePaymentPrice()));
        replaceData.put("FREESERVICETERMDAYS", wrapWithSpaces(String.valueOf(contractEntity.getFreeServiceTermDays())));
        replaceData.put("SERVICEDURATION", wrapWithSpaces(String.valueOf(contractEntity.getServiceDuration())));
        replaceData.put("SERVICEPERIOD", wrapWithSpaces(formatServiceDurationPeriod(contractEntity.getServiceDuration(), contractEntity.getServiceDurationPeriod())));
        replaceData.put("TOTALAMOUNT", wrapWithSpaces(contractEntity.getAmount()));
        replaceData.put("PERCENTAGEOFDOWNPAYMENT", wrapWithSpaces(
                contractEntity.getPrePaymentPrice().divide(contractEntity.getAmount(), 2, RoundingMode.UP)
                        .multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.UP) + "%"));
        replaceData.put("CONTRACTDEVICEDETAILS", deviceEntities);

        KvLogger kvLogger = KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_CREATE_TENANT_CONTRACT_REPLACE_DATA)
                .p(LogFieldConstants.TENANT_ID, contractEntity.getTenantId())
                .p("ContractId", contractEntity.getContractId());
        for (String key : replaceData.keySet()) {
            kvLogger.p(key, replaceData.get(key));
        }
        kvLogger.i();
        return replaceData;
    }

    private String wrapWithSpaces(Object value) {
        return value == null ? "无" : " " + value.toString() + " ";
    }

    private String formatServiceDurationPeriod(Integer serviceDuration, Integer durationPeriod) {
        switch (ServiceDurationPeriod.valueOf(durationPeriod)) {
            case Day:
                if (serviceDuration > 1) {
                    return "Days";
                } else {
                    return "Day";
                }
            case Week:
                if (serviceDuration > 1) {
                    return "Weeks";
                } else {
                    return "Week";
                }
            case Month:
                if (serviceDuration > 1) {
                    return "Months";
                } else {
                    return "Month";
                }
            default: // year
                if (serviceDuration > 1) {
                    return "Years";
                } else {
                    return "Year";
                }
        }
    }

    /**
     * Replace the contract content
     *
     * @param tenantContractFile Tenant Contract Documents
     * @param replacements       replacement parameters
     * @param consumer           call back do replace func
     * @throws IOException IOException
     */
    private void replacePlaceholdersInContract(File tenantContractFile, Map<String, Object> replacements, BiConsumer<String, XWPFRun> consumer) throws IOException {
        try (InputStream inputStream = FileUtils.openInputStream(tenantContractFile)) {
            try (XWPFDocument document = new XWPFDocument(inputStream)) {
                doReplace(document.getParagraphs(), replacements, consumer);
                // 遍历文档里面的table
                List<XWPFTable> tables = document.getTables();
                tables.stream()
                        .flatMap(table -> table.getRows().stream())
                        .flatMap(row -> row.getTableCells().stream())
                        .forEach(cell -> doReplace(cell.getParagraphs(), replacements, consumer));
                try (FileOutputStream out = new FileOutputStream(tenantContractFile)) {
                    document.write(out);
                }
            }
        }
    }

    /**
     * Start replace the contract content
     *
     * @param xwpfParagraphs XWPFParagraph list from tenant contract docx cells
     * @param replacements   replacement parameters
     * @param consumer       call back do replace func
     */
    private void doReplace(List<XWPFParagraph> xwpfParagraphs, Map<String, Object> replacements, BiConsumer<String, XWPFRun> consumer) {
        for (XWPFParagraph paragraph : xwpfParagraphs) {
            List<XWPFRun> runs = paragraph.getRuns();
            // 若包含文本块，对其做处理
            if (runs != null) {
                for (XWPFRun run : runs) {
                    String text = run.getText(0);
                    if (StringUtil.isBlank(text)) {
                        continue;
                    }
                    for (String key : replacements.keySet()) {
                        int index = text.indexOf(key);
                        if (index >= 0) {
                            consumer.accept(key, run);
                            break;
                        }
                    }
                }
            }
        }
    }
}
