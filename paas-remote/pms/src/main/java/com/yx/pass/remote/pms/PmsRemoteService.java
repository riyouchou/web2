package com.yx.pass.remote.pms;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Predicate;
import com.yx.pass.remote.pms.config.PmsApiConfig;
import org.slf4j.MDC;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.yx.lib.utils.constant.CommonConstants;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.logger.LogFieldConstants;
import org.yx.lib.utils.token.YxTokenBuilderUtil;
import org.yx.lib.utils.util.StringUtil;
import org.yx.lib.utils.util.YxRetryUtil;
import util.AESUtil;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PmsRemoteService {

    protected final RestTemplate restTemplate;
    protected final PmsApiConfig pmsApiConfig;

    private final String H_X_TOKEN = CommonConstants.X_TOKEN;

    public PmsRemoteService(RestTemplate restTemplate, PmsApiConfig pmsApiConfig) {
        this.restTemplate = restTemplate;
        this.pmsApiConfig = pmsApiConfig;
    }

    protected ResponseEntity<String> postRoute(String url, Object body) {
        return postRoute(url, body, null, null);
    }

    protected ResponseEntity<String> postRoute(String url, Object body, String ak, String sk) {
        return postRoute(url, body, ak, sk, null, response -> response == null || response.getStatusCode().value() != HttpStatus.OK.value());
    }

    protected ResponseEntity<String> postRoute(String url, Object body, Map<String, Object> customerTokenPayload) {
        return postRoute(url, body, null, null, customerTokenPayload);
    }

    protected ResponseEntity<String> postRoute(String url, Object body, String ak, String sk, Map<String, Object> customerTokenPayload) {
        return postRoute(url, body, ak, sk, customerTokenPayload, response -> response.getStatusCode().value() != HttpStatus.OK.value());
    }

    protected ResponseEntity<String> postRoute(String url, Object body, String ak, String sk, Map<String, Object> customerTokenPayload, Predicate<ResponseEntity<String>> predicate) {
        return postRoute(url, body, ak, sk, customerTokenPayload, 3, 1, TimeUnit.SECONDS, predicate);
    }

    protected ResponseEntity<String> postRoute(String url, Object body, String ak, String sk, Map<String, Object> customerTokenPayload,
                                               int retryTimes, int retrySleepTime, TimeUnit retrySleepUnit, Predicate<ResponseEntity<String>> predicate) {
//        if (StringUtil.isBlank(ak)) {
//            ak = pmsApiConfig.getPmsApiAk();
//        }
//        if (StringUtil.isBlank(sk)) {
//            sk = pmsApiConfig.getPmsApiSk();
//        }
//        if (sk.length() > 150) {
//            try {
//                sk = AESUtil.decrypt(sk);
//            } catch (Exception ex) {
//                KvLogger.instance(this)
//                        .p(LogFieldConstants.EVENT, "PmsRemoteCall")
//                        .p(LogFieldConstants.ACTION, "decryptSk")
//                        .p("Ak", ak)
//                        .p("EncryptSk", sk)
//                        .p(LogFieldConstants.ReqUrl, url)
//                        .p(LogFieldConstants.ERR_MSG, ex.getMessage())
//                        .e(ex);
//                return null;
//            }
//        }
//        Long xTokenExpireS = pmsApiConfig.getXTokenExpireS();
//        String xToken = YxTokenBuilderUtil.buildXToken(ak, sk, xTokenExpireS, customerTokenPayload);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.add(H_X_TOKEN, xToken);
        headers.add(CommonConstants.X_USER, MDC.get(CommonConstants.X_USER));
        headers.add(CommonConstants.TRACE_ID_HEADER, MDC.get(CommonConstants.TRACE_ID));
        HttpEntity<?> entity = new HttpEntity<>(body, headers);
        try {
            return YxRetryUtil.retry(() ->
                            restTemplate.postForEntity(url, entity, String.class),
                    retrySleepTime, retrySleepUnit, retryTimes, predicate);
        } catch (Exception e) {
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, "PmsRemoteCall")
                    .p(LogFieldConstants.ACTION, "PostRoute")
                    .p(LogFieldConstants.ReqUrl, url)
                    .p(LogFieldConstants.ERR_MSG, e.getMessage())
                    .e(e);
            return null;
        }
    }

    protected ResponseEntity<String> getRoute(String url) {
        return getRoute(url, null, response -> response.getStatusCode().value() != HttpStatus.OK.value());
    }

    protected ResponseEntity<String> getRoute(String url, Map<String, Object> customerTokenPayload) {
        return getRoute(url, customerTokenPayload, response -> response.getStatusCode().value() != HttpStatus.OK.value());
    }

    protected ResponseEntity<String> getRoute(String url, Map<String, Object> customerTokenPayload, Predicate<ResponseEntity<String>> predicate) {
        return getRoute(url, customerTokenPayload, 3, 1, TimeUnit.SECONDS, predicate);
    }

    protected ResponseEntity<String> getRoute(String url, Map<String, Object> customerTokenPayload, int retryTimes, int retrySleepTime,
                                              TimeUnit retrySleepUnit, Predicate<ResponseEntity<String>> predicate) {
//        // 获取 AK, SK 和 x-token
//        String ak = pmsApiConfig.getPmsApiAk();
//        String sk = pmsApiConfig.getPmsApiSk();
//        if (sk.length() > 150) {
//            try {
//                sk = AESUtil.decrypt(sk);
//            } catch (Exception ex) {
//                KvLogger.instance(this)
//                        .p(LogFieldConstants.EVENT, "PmsRemoteCall")
//                        .p(LogFieldConstants.ACTION, "DecryptSk")
//                        .p("Ak", ak)
//                        .p("EncryptSk", sk)
//                        .p(LogFieldConstants.ReqUrl, url)
//                        .p(LogFieldConstants.ERR_MSG, ex.getMessage())
//                        .e(ex);
//                return null;
//            }
//        }
//        Long xTokenExpireS = pmsApiConfig.getXTokenExpireS();
//        String xToken = YxTokenBuilderUtil.buildXToken(ak, sk, xTokenExpireS, customerTokenPayload);

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
//        headers.add(H_X_TOKEN, xToken);
        headers.add(CommonConstants.X_USER, MDC.get(CommonConstants.X_USER));
        headers.add(CommonConstants.TRACE_ID_HEADER, MDC.get(CommonConstants.TRACE_ID));
        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            // 使用重试机制发送 GET 请求
            return YxRetryUtil.retry(() ->
                            restTemplate.exchange(
                                    url,
                                    HttpMethod.GET,
                                    entity,
                                    String.class),
                    retrySleepTime, retrySleepUnit, retryTimes, predicate);
        } catch (Exception e) {
            // 记录日志
            KvLogger.instance(this)
                    .p(LogFieldConstants.ReqUrl, url)
                    .p(LogFieldConstants.EVENT, "PmsRemoteCall")
                    .p(LogFieldConstants.ACTION, "GetRoute")
                    .p(LogFieldConstants.ERR_MSG, e.getMessage())
                    .e(e);
            return null;
        }
    }
}
