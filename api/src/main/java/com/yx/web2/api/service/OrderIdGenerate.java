package com.yx.web2.api.service;

import com.yx.web2.api.common.constant.CacheConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.yx.lib.utils.util.SpringContextHolder;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class OrderIdGenerate {

    public static String generateMainOrderId(String orderSourceType) {
        RedisTemplate redisTemplate = SpringContextHolder.getBean(RedisTemplate.class);
        Date currentTime = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyMMdd");
        //格式化当前时间为【年的后2位+月+日】
        String originDateStr = simpleDateFormat.format(currentTime);
        //计算当前时间走过的秒

        LocalDateTime now = LocalDateTime.now();
        ZoneId zoneId = ZoneId.systemDefault();
        long currentTimeMillis = now.atZone(zoneId).toInstant().toEpochMilli();
        long differSecond = currentTimeMillis / 1000;

        //获取【年的后2位+月+日+秒】，秒的长度不足补充0
        String yyMMddSecond = originDateStr + StringUtils.leftPad(String.valueOf(differSecond), 5, '0');
        //获取【业务编码】 + 【年的后2位+月+日+秒】，作为自增key；
        String prefixOrder = orderSourceType + yyMMddSecond;
        //通过key，采用redis自增函数，实现单秒自增；不同的key，从0开始自增，同时设置60秒过期
        String orderIdGenKey = String.format(CacheConstants.ORDER_ID_GEN, orderSourceType);
        Long incrId = redisTemplate.opsForValue().increment(orderIdGenKey);
        Long timeout = redisTemplate.boundValueOps(orderIdGenKey).getExpire();
        if (timeout == null || timeout < 0) {
            redisTemplate.boundValueOps(orderIdGenKey).expire(60 - now.getSecond(), TimeUnit.SECONDS);
        }
        //生成订单编号
        return prefixOrder + StringUtils.leftPad(String.valueOf(incrId), 4, '0');
    }

    public static String generateSimpleOrderId(String orderSourceType) {
        Date currentTime = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyMMdd");
        //格式化当前时间为【年的后2位+月+日】
        String originDateStr = simpleDateFormat.format(currentTime);
        //计算当前时间走过的秒

        LocalDateTime now = LocalDateTime.now();
        ZoneId zoneId = ZoneId.systemDefault();
        long currentTimeMillis = now.atZone(zoneId).toInstant().toEpochMilli();
        long differSecond = currentTimeMillis / 1000;

        //获取【年的后2位+月+日+秒】，秒的长度不足补充0
        String yyMMddSecond = originDateStr + StringUtils.leftPad(String.valueOf(differSecond), 5, '0');
        //获取【业务编码】 + 【年的后2位+月+日+秒】
        if (StringUtils.isBlank(orderSourceType)) {
            return yyMMddSecond;
        } else {
            return orderSourceType + yyMMddSecond;
        }
    }
}
