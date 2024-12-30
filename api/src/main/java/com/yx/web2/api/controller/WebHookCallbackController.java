package com.yx.web2.api.controller;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yx.web2.api.common.constant.Web2LoggerEvents;
import com.yx.web2.api.config.DocusignConfig;
import com.yx.web2.api.service.IContractService;
import com.yx.web2.api.service.webhook.IWebHookCallbackService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.logger.LogFieldConstants;
import org.yx.lib.utils.util.StringUtil;

import javax.crypto.Mac;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebHookCallbackController {

    private final IWebHookCallbackService paymentCallbackService;
    private final DocusignConfig docusignConfig;
    private final IContractService contractService;

    @PostMapping("/stripe/callback")
    public ResponseEntity<?> stripeWebhookCallback(@RequestBody String payload, @RequestHeader(name = "Stripe-Signature") String sigHeader) {
        if (StringUtil.isBlank(payload) || StringUtil.isBlank(sigHeader)) {
            new ResponseEntity<>("Invalid payload or Stripe-Signature", HttpStatus.BAD_REQUEST);
        }
        try {
            return paymentCallbackService.doStripeWebhookCallback(payload, sigHeader);
        } catch (Exception ex) {
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_WEB_HOOK_CALL_BACK)
                    .p(LogFieldConstants.ERR_MSG, ex.getMessage())
                    .e(ex);
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/docusign/callback")
    public ResponseEntity<?> docusignWebhookCallback(@RequestBody String payload, @RequestHeader(name = "x-docusign-signature-1") String sigHeader) {
        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_DOCUSIGN_CALLBACK_RECIPIENT_COMPLETED)
                .p("Payload", payload)
                .p("x-docusign-signature-1", sigHeader)
                .i();

        Mac mac = HmacUtils.getInitializedMac("HmacSHA256", docusignConfig.getWebhookCallbackSk().getBytes(StandardCharsets.UTF_8));
        String base64Hash = new String(Base64.getEncoder().encode(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8))));
        boolean isEqual = MessageDigest.isEqual(base64Hash.getBytes(StandardCharsets.UTF_8),
                sigHeader.getBytes(StandardCharsets.UTF_8));
        if (!isEqual) {
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.CONTRACT_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.CONTRACT_EVENT_ACTION_DOCUSIGN_CALLBACK_RECIPIENT_COMPLETED)
                    .p("Payload", payload)
                    .p("Sign", sigHeader)
                    .p(LogFieldConstants.ERR_MSG, "signature error")
                    .i();
            return ResponseEntity.badRequest().build();
        }
        JSONObject payloadObject = JSON.parseObject(payload);
        if (payloadObject.getString("event").equals("recipient-completed")) {
            String generatedDateTime = payloadObject.getString("generatedDateTime");
            DateTime signedTime = DateUtil.parseUTC(generatedDateTime);
            // {"accountId":"6cff5ba6-9f6c-4e87-8036-e74f829c8ea7","envelopeId":"4f4dd6c7-b625-420d-bfce-5c76324eed5f","recipientId":"1","userId":"a68c7ddf-f2e2-43e6-bd8f-1e5f15929bc2"}
            JSONObject data = JSON.parseObject(payload).getJSONObject("data");
            String envelopeId = data.getString("envelopeId");
            String recipientId = data.getString("recipientId");
            return contractService.signed(envelopeId, recipientId, signedTime);
        }
        return ResponseEntity.ok().build();
    }
}
