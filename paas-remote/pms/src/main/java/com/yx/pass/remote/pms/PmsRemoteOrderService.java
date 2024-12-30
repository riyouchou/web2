package com.yx.pass.remote.pms;

import com.alibaba.fastjson.JSON;
import com.yx.pass.remote.pms.config.PmsApiConfig;
import com.yx.pass.remote.pms.model.req.PayOrderReq;
import com.yx.pass.remote.pms.model.resp.tenant.TenantInfoResp;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.yx.lib.utils.util.R;
import org.yx.lib.utils.util.SpringContextHolder;

/**
 * Pms 订单相关服务
 * 1：创建订单
 * 2：订单支付
 * 3：订单关闭
 */
public class PmsRemoteOrderService extends PmsRemoteService {

    public PmsRemoteOrderService(RestTemplate restTemplate, PmsApiConfig pmsApiConfig) {
        super(restTemplate, pmsApiConfig);
    }

//    /**
//     * 创建订单
//     *
//     * @param orderCreateReq pms创建订单信息
//     * @return 订单信息
//     */
//    public R<OrderCreateResp> createOrder(OrderCreateReq orderCreateReq) {
//        PmsRemoteTenantService tenantService = SpringContextHolder.getBean(PmsRemoteTenantService.class);
//        R<TenantInfoResp> r = tenantService.getTenantInfo(orderCreateReq.getTid());
//        if (r == null || r.getCode() != R.ok().getCode()) {
//            return R.failed(404, "not found tid info from pms");
//        }
//        ResponseEntity<String> responseEntity = postRoute(pmsApiConfig.getOrderCreateUrl(), orderCreateReq, r.getData().getAkSk().getAk(), r.getData().getAkSk().getSk());
//        if (responseEntity != null) {
//            return JSON.parseObject(responseEntity.getBody(), new TypeReference<R<OrderCreateResp>>() {
//            }.getType());
//        }
//        return R.failed(143700, "remote call pms failed");
//    }

    /**
     * 订单支付
     *
     * @param payOrderReq pms支付订单信息
     * @return 成功or失败
     */
    public R<?> payOrder(PayOrderReq payOrderReq) {
        PmsRemoteTenantService tenantService = SpringContextHolder.getBean(PmsRemoteTenantService.class);
        R<TenantInfoResp> r = tenantService.getTenantInfo(payOrderReq.getTid());
        if (r == null || r.getCode() != R.ok().getCode()) {
            return R.failed(404, "not found tid info from pms");
        }
        ResponseEntity<String> responseEntity = postRoute(pmsApiConfig.getOrderPayUrl(), payOrderReq, r.getData().getAkSk().getAk(), r.getData().getAkSk().getSk());
        if (responseEntity != null) {
            return JSON.parseObject(responseEntity.getBody(), R.class);
        }
        return R.failed(143700, "remote call pms failed");
    }

//    /**
//     * 订单关闭
//     *
//     * @param closeOrderReq pms关闭订单信息
//     * @return 成功or失败
//     */
//    public R<?> orderClose(CloseOrderReq closeOrderReq) {
//        ResponseEntity<String> responseEntity = postRoute(pmsApiConfig.getOrderCloseUrl(), closeOrderReq);
//        if (responseEntity != null) {
//            return JSON.parseObject(responseEntity.getBody(), R.class);
//        }
//        return R.failed(143700, "remote call pms failed");
//    }
}
