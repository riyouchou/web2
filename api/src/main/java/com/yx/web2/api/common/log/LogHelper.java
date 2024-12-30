package com.yx.web2.api.common.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogHelper {
    public static Logger log = LoggerFactory.getLogger(LogHelper.class);

    /**
     * 通用日志记录方法，用于记录请求参数和结果
     *
     * @param methodName 方法名称或操作名称
     * @param url        请求的URL
     * @param params     请求参数
     * @param result     调用结果
     */
    public static void logRequestAndResponse(String methodName, String url, Object params, Object result) {
        log.info("调用{} 参数：{} --- url: {}", methodName, params, url);
        log.info("调用{} 结果：{}", methodName, result);
    }

    /**
     * 通用日志记录方法，用于记录异常
     *
     * @param methodName 方法名称或操作名称
     * @param url        请求的URL
     * @param params     请求参数
     * @param e          异常信息
     */
    public static void logError(String methodName, String url, Object params, Exception e) {
        log.error("调用{} 参数：{} --- url: {} 出现异常：{}", methodName, params, url, e.getMessage(), e);
    }
}
