package com.yx.web2.api.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.nacos.shaded.com.google.gson.Gson;
import com.google.common.reflect.TypeToken;
import com.yx.pass.remote.pms.PmsRemoteAccountService;
import com.yx.pass.remote.pms.model.resp.account.TenantDetailResp;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.common.resp.PageResp;
import com.yx.web2.api.service.IAccountService;
import com.yx.web2.api.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yx.lib.utils.util.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements IAccountService {

    private final PmsRemoteAccountService pmsRemoteAccountService;
    private final IOrderService orderService;

    @Override
    public R<PageResp<TenantDetailResp>> webPage(Integer current, Integer size, AccountModel accountModel, Long accountId, String accountName,Integer source) {
        R<String> accountR = pmsRemoteAccountService.webPage(current, size, accountModel.getAccountId(), accountModel.getTenantId(),accountId,accountName,source);
        log.info("title:{}, result:{}", "查询账号信息", JSONUtil.toJsonStr(accountR));

        PageResp<TenantDetailResp> resultData = new PageResp<>();
        resultData.setRecords(new ArrayList<>());
        if (accountR.getCode() != R.ok().getCode() || StrUtil.isBlank(accountR.getData())) {
            return R.ok(resultData);
        }

        try {
            // 使用 Gson 进行类型安全的反序列化
            Gson gson = new Gson();
            resultData = gson.fromJson(accountR.getData(), new TypeToken<PageResp<TenantDetailResp>>() {
            }.getType());
        } catch (Exception e) {
            log.error("Failed to parse JSON data to PageResp<TenantDetailResp>", e);
            return R.ok(resultData);
        }

        if (CollectionUtil.isEmpty(resultData.getRecords())) {
            return R.ok(resultData);
        }
        //log.info("accountuserList:{}", JSONUtil.toJsonStr(resultData.getRecords()));
        // 获取用户ID列表
        List<Long> tids = resultData.getRecords().stream()
                .map(TenantDetailResp::getTid) // 直接使用方法引用
                .collect(Collectors.toList());
        //等于1 是下拉

            // 获取每个用户的订单数量
            Map<Long, Integer> orderNumMap = orderService.getAccountOrderNum(tids);

            // 更新每个用户的信息
            resultData.getRecords().forEach(a -> {
                Integer orderNum = orderNumMap.get(a.getTid());
                if (orderNum != null) {
                    a.setOrderNum(orderNum);
                }
            });

            log.info("title:{}, accountData:{}", "查询账号信息转换后的信息", JSONUtil.toJsonStr(resultData));
            return R.ok(resultData);


    }
}
