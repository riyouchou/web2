package com.yx.web2.api.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.common.req.order.AthTransferQueryReq;
import com.yx.web2.api.common.resp.PageResp;
import com.yx.web2.api.common.resp.order.AthTransferRecordListResp;
import com.yx.web2.api.common.resp.order.OrderListResp;
import com.yx.web2.api.entity.TenantAthTransferRecordEntity;
import com.yx.web2.api.mapper.TenantAthTransferRecordMapper;
import com.yx.web2.api.service.ITenantAthTransferRecordService;
import org.springframework.stereotype.Service;
import org.yx.lib.utils.util.R;
import org.yx.lib.utils.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

@Service
public class TenantAthTransferRecordServiceImpl extends ServiceImpl<TenantAthTransferRecordMapper, TenantAthTransferRecordEntity> implements ITenantAthTransferRecordService {
    @Override
    public R<PageResp<AthTransferRecordListResp>> recordList(Long tenantId, AccountModel accountModel, AthTransferQueryReq athTransferQueryReq) {
        LambdaQueryWrapper<TenantAthTransferRecordEntity> lambdaQueryWrapper = Wrappers.lambdaQuery();
        if (StringUtil.isNotBlank(athTransferQueryReq.getOrderId())) {
            lambdaQueryWrapper.likeRight(TenantAthTransferRecordEntity::getOrderId, athTransferQueryReq.getOrderId());
        }
        if (StringUtil.isNotBlank(athTransferQueryReq.getAccountName())) {
            lambdaQueryWrapper.likeRight(TenantAthTransferRecordEntity::getTransToTenantName, athTransferQueryReq.getAccountName());
        }
        IPage<TenantAthTransferRecordEntity> page = new Page<>(athTransferQueryReq.getCurrent(), athTransferQueryReq.getSize());
        page.orders().add(OrderItem.desc("id"));
        this.page(page, lambdaQueryWrapper);

        List<AthTransferRecordListResp> resultList = new ArrayList<>();
        page.getRecords().forEach(transferRecord -> {
            resultList.add(AthTransferRecordListResp.builder()
                    .accountId(transferRecord.getTransToTenantId().toString())
                    .accountName(transferRecord.getTransToTenantName())
                    .orderId(transferRecord.getOrderId())
                    .transferStatus(transferRecord.getTransferStatus())
                    .transferType(transferRecord.getTransType())
                    .amount(transferRecord.getAthAmount().toString())
                    .transferredTime(DateUtil.formatDateTime(transferRecord.getCreateTime()))
                    .build());
        });
        PageResp<AthTransferRecordListResp> resultData = new PageResp<>();
        resultData.setCurrent(athTransferQueryReq.getCurrent());
        resultData.setSize(athTransferQueryReq.getSize());
        resultData.setRecords(resultList);
        resultData.setTotal(page.getTotal());
        resultData.setPages(page.getPages());
        return R.ok(resultData);
    }
}
