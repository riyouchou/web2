package com.yx.pass.remote.feecenter;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Predicate;
import com.yx.pass.remote.feecenter.config.FeeCenterApiConfig;
import org.slf4j.MDC;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.yx.lib.utils.constant.CommonConstants;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.logger.LogFieldConstants;
import org.yx.lib.utils.token.YxTokenBuilderUtil;
import org.yx.lib.utils.util.Base64Util;
import org.yx.lib.utils.util.YxRetryUtil;
import util.AESUtil;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class FeeCenterRemoteService {

    protected final RestTemplate restTemplate;
    protected final FeeCenterApiConfig feeCenterApiConfig;

    private final String H_X_TOKEN = "x-token";
    public static final String H_X_USER = "x-user";

    public FeeCenterRemoteService(RestTemplate restTemplate, FeeCenterApiConfig feeCenterApiConfig) {
        this.restTemplate = restTemplate;
        this.feeCenterApiConfig = feeCenterApiConfig;
    }

    protected ResponseEntity<String> postRoute(String url, Object body, Map<String, String> headers) {
        return postRoute(url, body, null, headers, response -> response == null || response.getStatusCode().value() != HttpStatus.OK.value());
    }

    protected ResponseEntity<String> postRoute(String url, Object body, Map<String, Object> customerTokenPayload, Map<String, String> headers) {
        return postRoute(url, body, customerTokenPayload, headers, response -> response.getStatusCode().value() != HttpStatus.OK.value());
    }

    protected ResponseEntity<String> postRoute(String url, Object body, Map<String, Object> customerTokenPayload,
                                               Map<String, String> headers, Predicate<ResponseEntity<String>> predicate) {
        return postRoute(url, body, customerTokenPayload, headers, 3, 1, TimeUnit.SECONDS, predicate);
    }

    protected ResponseEntity<String> postRoute(String url, Object body, Map<String, Object> customerTokenPayload, Map<String, String> headers,
                                               int retryTimes, int retrySleepTime, TimeUnit retrySleepUnit, Predicate<ResponseEntity<String>> predicate) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

//        if (headers == null || !headers.containsKey(H_X_TOKEN)) {
//            String ak = feeCenterApiConfig.getAk();
//            String sk = feeCenterApiConfig.getSk();
//            if (sk.length() > 150) {
//                try {
//                    sk = AESUtil.decrypt(sk);
//                } catch (Exception ex) {
//                    KvLogger.instance(this)
//                            .p(LogFieldConstants.EVENT, "FeeCenterRemoteCall")
//                            .p(LogFieldConstants.ACTION, "DecryptSk")
//                            .p("Ak", ak)
//                            .p("EncryptSk", sk)
//                            .p("ReqUrl", url)
//                            .p(LogFieldConstants.ERR_MSG, ex.getMessage())
//                            .e(ex);
//                    return null;
//                }
//            }
//            Long xTokenExpireS = feeCenterApiConfig.getXTokenExpireS();
//            String xToken = YxTokenBuilderUtil.buildXToken(ak, sk, xTokenExpireS, customerTokenPayload);
//            httpHeaders.add(H_X_TOKEN, xToken);
//        }
        if (headers != null) {
            headers.keySet().forEach(headerName -> {
                httpHeaders.add(headerName, headers.get(headerName));
            });
        }
        httpHeaders.add(CommonConstants.TRACE_ID_HEADER, MDC.get(CommonConstants.TRACE_ID));
        HttpEntity<?> entity = new HttpEntity<>(body, httpHeaders);
        try {
            return YxRetryUtil.retry(() -> restTemplate.postForEntity(url, entity, String.class),
                    retrySleepTime, retrySleepUnit, retryTimes, predicate);
        } catch (Exception e) {
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, "FeeCenterRemoteCall")
                    .p(LogFieldConstants.ACTION, "PostRoute")
                    .p("ReqUrl", url)
                    .p(LogFieldConstants.ERR_MSG, e.getMessage())
                    .e(e);
            return null;
        }
    }

    protected String buildXUser(Long tid, String tenantType, Long accountId) {
        return YxTokenBuilderUtil.buildXUser(tid, tenantType, accountId);
    }
}
