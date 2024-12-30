package com.yx.web2.api.common.http;

import com.alibaba.fastjson.JSONObject;
import com.yx.pass.remote.feecenter.config.FeeCenterApiConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.yx.lib.utils.util.Base64Util;

import javax.annotation.Resource;

@Component
@Slf4j
@RefreshScope
public class HttpUtils {
    @Resource(name = "yxRestTemplate")
    private RestTemplate restTemplate;
    @Resource
    private TokenUtil tokenUtil;
    @Resource
    private FeeCenterApiConfig feeCenterApiConfig;

    /**
     * http 调用api
     *
     * @author yijian
     * @date 2024/9/10 14:13
     */
    public String sendPostToken(String url, String reqData, Long tid, String tenantType, String ak, String sk) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String token = tokenUtil.getToken(feeCenterApiConfig.getAk(), feeCenterApiConfig.getSk());
        // 添加 x-user 头部信息
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("tid", tid);
        jsonObject.put("tenantType", tenantType);
        String encode = Base64Util.encode(jsonObject.toJSONString());
        headers.set("x-user", encode);
        headers.set("x-token", token);

        HttpEntity<String> param = new HttpEntity<>(reqData, headers);
        JSONObject result = new JSONObject();

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, param, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                result = JSONObject.parseObject(response.getBody());
            } else {
                log.error("调用异常: {}", response.getBody());
                result.put("error", "请求失败，状态码: " + response.getStatusCodeValue());
            }
        } catch (HttpStatusCodeException e) {
            log.error("HTTP状态异常: {}", e.getStatusCode(), e);
            result.put("error", "HTTP状态异常: " + e.getStatusCode());
        } catch (ResourceAccessException e) {
            log.error("连接超时或读取超时", e);
            result.put("error", "连接超时或读取超时");
        } catch (Exception e) {
            log.error("请求异常: ", e);
            result.put("error", "请求异常: " + e.getMessage());
        }

        return result.toJSONString();
    }


    /**
     * http 调用api
     *
     * @author yijian
     * @date 2024/9/10 14:13
     */
    public String sendGetToken(String url, String reqData, String tid, String tenantType, String ak, String sk) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String token = tokenUtil.getToken(feeCenterApiConfig.getAk(), feeCenterApiConfig.getSk());
        // 添加 x-user 头部信息
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("tid", tid);
        jsonObject.put("tenantType", tenantType);
        String encode = Base64Util.encode(jsonObject.toJSONString());
        headers.set("x-user", encode);
        headers.set("x-token", token);

        HttpEntity<String> param = new HttpEntity<>(reqData, headers);
        JSONObject result = new JSONObject();

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class, param);
            if (response.getStatusCode().is2xxSuccessful()) {
                result = JSONObject.parseObject(response.getBody());
            } else {
                log.error("调用异常: {}", response.getBody());
                result.put("error", "请求失败，状态码: " + response.getStatusCodeValue());
            }
        } catch (HttpStatusCodeException e) {
            log.error("HTTP状态异常: {}", e.getStatusCode(), e);
            result.put("error", "HTTP状态异常: " + e.getStatusCode());
        } catch (ResourceAccessException e) {
            log.error("连接超时或读取超时", e);
            result.put("error", "连接超时或读取超时");
        } catch (Exception e) {
            log.error("请求异常: ", e);
            result.put("error", "请求异常: " + e.getMessage());
        }

        return result.toJSONString();
    }
}