package com.yx.pass.remote.feecenter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.google.common.collect.Maps;
import com.yx.pass.remote.feecenter.config.FeeCenterApiConfig;
import com.yx.pass.remote.feecenter.model.req.BindContainerReq;
import com.yx.pass.remote.feecenter.model.req.CloseOrderReq;
import com.yx.pass.remote.feecenter.model.req.FeeCenterReqBase;
import com.yx.pass.remote.feecenter.model.req.OrderCreateReq;
import com.yx.pass.remote.feecenter.model.resp.OrderCreateResp;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.util.R;

import java.util.Map;

/**
 * FeeCenter 订单相关服务
 * 1：创建订单
 * 2：订单支付
 * 3：订单关闭
 */
public class FeeCenterRemoteOrderService extends FeeCenterRemoteService {

    public FeeCenterRemoteOrderService(RestTemplate restTemplate, FeeCenterApiConfig feeCenterApiConfig) {
        super(restTemplate, feeCenterApiConfig);
    }

    /**
     * 创建订单
     *
     * @param orderCreateReq feeCenter创建订单信息
     * @return 订单信息
     */
    public R<OrderCreateResp> createOrder(OrderCreateReq orderCreateReq) {
        Map<String, String> headers = Maps.newHashMap();
        headers.put(H_X_USER, buildXUser(orderCreateReq.getTid(), orderCreateReq.getTenantType(), orderCreateReq.getAccountId()));
        ResponseEntity<String> responseEntity = postRoute(feeCenterApiConfig.getOrderCreateUrl(), orderCreateReq, headers);
        if (responseEntity != null) {
            return JSON.parseObject(responseEntity.getBody(), new TypeReference<R<OrderCreateResp>>() {
            }.getType());
        }
        return R.failed(143600, "remote call fee-center failed");
    }
//
//    /**
//     * 订单支付
//     *
//     * @param payOrderReq feeCenter支付订单信息
//     * @return 成功or失败
//     */
//    public R<?> payOrder(PayOrderReq payOrderReq) {
//        Map<String, String> headers = Maps.newHashMap();
//        headers.put(H_X_USER, buildXUser(payOrderReq.getTid(), payOrderReq.getTenantType(), payOrderReq.getAccountId()));
//        ResponseEntity<String> responseEntity = postRoute(feeCenterApiConfig.getOrderPayUrl(), payOrderReq, headers);
//        if (responseEntity != null) {
//            return JSON.parseObject(responseEntity.getBody(), R.class);
//        }
//        return R.failed(143600, "remote call fee-center failed");
//    }
//

    /**
     * 订单关闭
     *
     * @param closeOrderReq feeCenter关闭订单信息
     * @return 成功or失败
     */
    public R<?> orderClose(CloseOrderReq closeOrderReq) {
        Map<String, String> headers = Maps.newHashMap();
        headers.put(H_X_USER, buildXUser(10000L, "Admin", 1L));
        ResponseEntity<String> responseEntity = postRoute(feeCenterApiConfig.getOrderCloseUrl(), closeOrderReq, headers);
        if (responseEntity != null) {
            return JSON.parseObject(responseEntity.getBody(), R.class);
        }
        return R.failed(143600, "remote call fee-center failed");
    }

    /**
     * 查询PMS 订单是否存在
     *
     * @param reqBase 租户ID
     * @return 成功or失败
     */
    public R<?> containerOrderExists(FeeCenterReqBase reqBase) {
        Map<String, Object> postBody = Maps.newHashMap();
        postBody.put("resourcePool", reqBase.getResourcePool());
        Map<String, String> headers = Maps.newHashMap();
        headers.put(H_X_USER, buildXUser(reqBase.getTid(), reqBase.getTenantType(), reqBase.getAccountId()));
        ResponseEntity<String> responseEntity = postRoute(feeCenterApiConfig.getOrderExistsUrl(), postBody, headers);
        KvLogger.instance(this)
                .p("method", "Get Fee-Idc-Api push user has order information")
                .p("resBody", JSON.parseObject(responseEntity.getBody(), R.class))
                .p("reqBody", JSONObject.toJSONString(reqBase))
                .p("ReqUrl", feeCenterApiConfig.getOrderExistsUrl())
                .i();
        if (responseEntity != null) {
            return JSON.parseObject(responseEntity.getBody(), R.class);
        }
        return R.failed(143600, "remote call fee-center failed");
    }

    /**
     * 绑定容器
     *
     * @param bindContainerReq 绑定容器请求
     * @return R
     */
    public R<?> bindContainer(BindContainerReq bindContainerReq) {
        Map<String, String> headers = Maps.newHashMap();
        headers.put(H_X_USER, buildXUser(bindContainerReq.getTid(), null, null));
        ResponseEntity<String> responseEntity = postRoute(feeCenterApiConfig.getOrderBindUrl(), bindContainerReq, headers);
        if (responseEntity != null) {
            return JSON.parseObject(responseEntity.getBody(), new TypeReference<R<OrderCreateResp>>() {
            }.getType());
        }
        return R.failed(143600, "remote call fee-center failed");
    }
}
