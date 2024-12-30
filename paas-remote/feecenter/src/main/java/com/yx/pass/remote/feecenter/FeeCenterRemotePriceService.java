package com.yx.pass.remote.feecenter;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.TypeReference;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.yx.pass.remote.feecenter.config.FeeCenterApiConfig;
import com.yx.pass.remote.feecenter.model.req.QueryUsdRateReq;
import com.yx.pass.remote.feecenter.model.resp.UsdRateResp;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.yx.lib.utils.util.R;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * FeeCenter 价格相关服务
 * 1：查詢汇率
 */
public class FeeCenterRemotePriceService extends FeeCenterRemoteService {
    public FeeCenterRemotePriceService(RestTemplate restTemplate, FeeCenterApiConfig feeCenterApiConfig) {
        super(restTemplate, feeCenterApiConfig);
    }

    /**
     * 查詢汇率
     *
     * @param queryUsdRateReq 查询汇率信息
     * @return 汇率列表
     */
    public List<UsdRateResp> queryUsdRate(QueryUsdRateReq queryUsdRateReq) {
        String beginTime = DateUtil.formatDateTime(queryUsdRateReq.getBeginDateTime());
        String endTime = DateUtil.formatDateTime(queryUsdRateReq.getEndDateTime());
        Map<String, String> postBody = Maps.newHashMap();
        postBody.put("beginTime", beginTime);
        postBody.put("endTime", endTime);

        Map<String, String> headers = Maps.newHashMap();
        headers.put(H_X_USER, buildXUser(queryUsdRateReq.getTid(), queryUsdRateReq.getTenantType(), queryUsdRateReq.getAccountId()));

        ResponseEntity<String> responseEntity = postRoute(feeCenterApiConfig.getPriceUsdRateUrl(), postBody, headers);
        if (responseEntity != null) {
            R<List<UsdRateResp>> r = JSON.parseObject(responseEntity.getBody(), new TypeReference<R<List<UsdRateResp>>>() {
            }.getType());
            if (r != null && r.getCode() == 0) {
                return r.getData();
            }
        }
        return null;
    }
}
