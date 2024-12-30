package com.yx.web2.api.common.http;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.json.JSONObject;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.yx.lib.utils.constant.TokenPayload;
import org.yx.lib.utils.token.YxTokenBuilderUtil;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class TokenUtil {
    /**
     * 生成签名，30分钟过期
     *
     * @param **userInfo** 用户信息 用户姓名
     * @param **other**    用户其他信息 用户id
     * @return
     */
    public String getToken(String ak, String sk) {
        try {
            // 设置头部信息
//            Map<String, Object> header = new HashMap<>(2);
//            header.put("alg", "HS256");
//            Map<String, Object> payload = new HashMap<>(2);
//            payload.put(AK.getValue(), ak);
//            payload.put(TokenPayload.EXP.getValue(), System.currentTimeMillis() + 1000 * 60 * 5);
//            JWTSigner jwtSigner = new CustomHMacJWTSigner(AlgorithmUtil.getAlgorithm("hs256"), KeyUtil.generateKey(AlgorithmUtil.getAlgorithm("hs256"), sk.getBytes()));

//            return JWTUtils.createToken(header, payload, "", jwtSigner);
            return YxTokenBuilderUtil.buildXToken(ak, sk, 3600L, null);
        } catch (Exception e) {
            return null;
        }
    }


    public static String getTokenNew(String ak, String sk, Long appId, Long uid, Long time) {
        try {
//            // 设置头部信息
//            Map<String, Object> header = new HashMap<>(2);
////            header.put("type", "JWT");
//            header.put("alg", "HS256");
//            Map<String, Object> payload = new HashMap<>(2);
//            payload.put(AK.getValue(), ak);
//            payload.put(TokenPayload.EXP.getValue(), System.currentTimeMillis() + 1000 * time);
//            //自行添加创建的扩展字段和入参
//            payload.put("appId", appId);
//            payload.put("uid", uid);
////            payload.put("tid", tid);
//            payload.put("nonce", System.currentTimeMillis());
//            log.info("token加密的参数：" + payload);
//            JWTSigner jwtSigner = new CustomHMacJWTSigner(AlgorithmUtil.getAlgorithm("hs256"), KeyUtil.generateKey(AlgorithmUtil.getAlgorithm("hs256"), sk.getBytes()));
//            String token = JWTUtils.createToken(header, payload, "", jwtSigner);
//            log.info("token加密的结果：" + token);
            Map<String, Object> customerPayload = Maps.newHashMap();
            customerPayload.put("appId", appId);
            customerPayload.put("uid", uid);
            return YxTokenBuilderUtil.buildXToken(ak, sk, time, customerPayload);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

//    public static void main(String[] args) {
//        String ak = "f1y7bqyeqnl3h4vrbgm8";
//        String sk = "bzt4nhfhdizcazu6h435ily0g7q4qmpu5kwrbghaj90miva5qgn80rwsvgfbdnvj";
//        String sign = getToken(ak, sk);
//        System.out.println(sign);
//    }

    /**
     * generate YXT token
     *
     * @param ak         the access key of the tenant
     * @param sk         the secret key of the tenant
     * @param expireS    the token expire time, unit is second
     * @param reqBody    the request body content that used to be signed
     * @param reqHeaders the request headers that used to be signed
     * @return the YXT token， null means failed
     */
    public static String getYXTToken(String ak, String sk, Long expireS, String reqBody, TreeMap<String, Object> reqHeaders) {
        try {
            // make the header key array by request headers
            List<String> h = new ArrayList<>();
            if (!ObjectUtils.isEmpty(reqHeaders)) {
                reqHeaders.forEach((key, value) -> {
                    h.add(key);
                });
            }
            // build the header part
            JSONObject headers = new JSONObject();
            headers.set("alg", "HS256");
            headers.set("typ", "YXT");

            // build the palylod part
            JSONObject payload = new JSONObject();
            payload.set("exp", System.currentTimeMillis() + 1000 * expireS);
            payload.set("ak", ak);
            payload.set("h", h);

            //Generate random integers between 100000 and 999999
            int sixDigitRandomNumber = ThreadLocalRandom.current().nextInt(100000, 1000000);
            payload.set("nonce", sixDigitRandomNumber);

            // build the request header and request body content that need to be signed
            StringBuffer content = new StringBuffer();
            if (!ObjectUtils.isEmpty(h)) {
                Collections.synchronizedList(h).forEach(it -> {
                    content.append(it.toLowerCase() + ":").append(reqHeaders.get(it).toString().trim()).append("\n");
                });
            }
            content.append(reqBody);

            // encode the header payload and content with base64 algorithm
            String headersStr = Base64.getUrlEncoder().withoutPadding().encodeToString(headers.toString().getBytes(StandardCharsets.UTF_8));
            String payloadStr = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toString().getBytes(StandardCharsets.UTF_8));
            String contentStr = Base64.getUrlEncoder().withoutPadding().encodeToString(content.toString().getBytes(StandardCharsets.UTF_8));

            // make a HMac-SHA256 object with secret key
            HMac hMac = SecureUtil.hmac(HmacAlgorithm.HmacSHA256, sk);

            // get signature by HMac-SHA256
            String signature = hMac.digestBase64(CharSequenceUtil.format("{}.{}", headersStr, CharSequenceUtil.format("{}.{}", payloadStr, contentStr)), StandardCharsets.UTF_8, true);
            String token = headersStr + "." + payloadStr + "." + signature;
            return token;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * generate JWT token
     *
     * @param ak      the access key of the tenant
     * @param sk      the secret key of the tenant
     * @param appId   game id
     * @param expireS the token expire time, unit is second
     * @param uid     user id
     * @return the JWT token， null means failed
     */
    public static String getJWTToken(String ak, String sk, Long appId, Long expireS, String uid) {
        try {
            // buid header part
            JSONObject headers = new JSONObject();
            headers.set("alg", "HS256");
            headers.set("typ", "JWT");

            // buid payload part
            JSONObject payload = new JSONObject();
            payload.set("exp", System.currentTimeMillis() + 1000 * expireS);
            payload.set("ak", ak);
            payload.set("appid", appId);
            payload.set("uid", uid);
            payload.set("nonce", UUID.randomUUID().toString());

            // base64 encode
            String headersStr = Base64.getUrlEncoder().withoutPadding().encodeToString(headers.toString().getBytes(StandardCharsets.UTF_8));
            String payloadStr = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toString().getBytes(StandardCharsets.UTF_8));

            // make a HMac-SHA256 object with secret key
            HMac hMac = SecureUtil.hmac(HmacAlgorithm.HmacSHA256, sk);

            // get signature by HMac-SHA256
            String signature = hMac.digestBase64(CharSequenceUtil.format("{}.{}", headersStr, payloadStr), StandardCharsets.UTF_8, true);
            String token = headersStr + "." + payloadStr + "." + signature;
            return token;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getCDNJWT(String ak, String sk, Long expireS, String sub) {
        Map<String, Object> payload = new HashMap<>(1);
        payload.put(TokenPayload.NBF.getValue(), System.currentTimeMillis() / 1000);
        String pms = "PMS";
        payload.put(TokenPayload.ISSUER.getValue(), pms);
        payload.put(TokenPayload.SUBJECT.getValue(), sub);
        return YxTokenBuilderUtil.buildXToken(ak, sk, expireS, payload);
    }

//    public static void main(String[] args) {
//        //AK: lard29bj3ld2gsewkjrq
//        //SK: 8h35gqbe89uwpqquc9hzz7yx69nx01zt5zrewl6niap46k7jhugbmqgsgooczgx7
////        String ak = "lard29bj3ld2gsewkjrq";
////        String sk = "8h35gqbe89uwpqquc9hzz7yx69nx01zt5zrewl6niap46k7jhugbmqgsgooczgx7";
////        Long expireS = 10000L;
////        JSONObject entries = new JSONObject();
////        entries.set("gameId", 10006);
////        entries.set("uid", "hanyong01");
////        entries.set("appid", 123);
////        String reqBody = JSONUtil.toJsonStr(entries);
////        //date:Wed, 26 Jul 2023 06:12:08 GMT
////        //host:192.168.31.12:10101
////        //content-type:application/json;charset=utf-8
////        //content-length:69
////        //{"gameId":10006,"uid":"hanyong01","appid":123}
////        TreeMap<String, Object> reqHeaders = new TreeMap<>();
////        reqHeaders.put("Content-Type", "application/json;charset=utf-8");
////        reqHeaders.put("date", "Wed, 26 Jul 2023 06:12:08 GMT");
//////        reqHeaders.put("host", "192.168.31.12:10101");
////        reqHeaders.put("content-length", "69");
////
////
//////        String token = getYXTToken(ak, sk, expireS,  reqBody, reqHeaders);
////        String token = getJWTToken(ak, sk, 123L, expireS,  "hanyong01");
////        System.out.println(token);
//
//        System.out.println(getCDNJWT("LzdWGpAoTQ1DqYuzHxE6YBci7G3X2yvNBot8uNXfx5k", 3600L, "21001"));
//    }
}