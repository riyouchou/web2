import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapBuilder;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.docusign.esign.api.EnvelopesApi;
import com.docusign.esign.client.ApiClient;
import com.docusign.esign.client.auth.OAuth;
import com.docusign.esign.model.Document;
import com.docusign.esign.model.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.SubscriptionSchedule;
import com.stripe.param.PaymentIntentCreateParams;
import com.xxl.job.core.context.XxlJobHelper;
import com.yx.pass.remote.feecenter.FeeCenterRemoteOrderService;
import com.yx.pass.remote.feecenter.FeeCenterRemoteWalletService;
import com.yx.pass.remote.feecenter.model.req.CloseOrderReq;
import com.yx.pass.remote.feecenter.model.req.OrderCreateReq;
import com.yx.pass.remote.feecenter.model.req.WalletTransferToTenantReq;
import com.yx.pass.remote.feecenter.model.resp.OrderCreateResp;
import com.yx.pass.remote.feecenter.model.resp.WalletTransferResp;
import com.yx.pass.remote.pms.PmsRemoteOrderService;
import com.yx.pass.remote.pms.PmsRemoteTenantService;
import com.yx.pass.remote.pms.model.req.PayOrderReq;
import com.yx.pass.remote.pms.model.resp.tenant.TenantInfoResp;
import com.yx.web2.api.Web2ApiApplication;
import com.yx.web2.api.common.constant.Web2LoggerEvents;
import com.yx.web2.api.common.enums.*;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.common.req.order.DueOrderPayReq;
import com.yx.web2.api.common.req.order.OrderIdReq;
import com.yx.web2.api.common.req.order.OrderPayVirtualReq;
import com.yx.web2.api.config.DocusignConfig;
import com.yx.web2.api.config.Web2ApiConfig;
import com.yx.web2.api.entity.*;
import com.yx.web2.api.service.*;
import com.yx.web2.api.service.mq.KafkaConsumerForOrder;
import com.yx.web2.api.service.payment.AthOrderPaymentService;
import com.yx.web2.api.service.payment.StripePaymentService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.poi.xwpf.usermodel.*;
import org.junit.runner.RunWith;
import org.slf4j.MDC;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.yx.lib.utils.constant.CommonConstants;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.logger.LogFieldConstants;
import org.yx.lib.utils.token.YxTokenBuilderUtil;
import org.yx.lib.utils.util.R;
import org.yx.lib.utils.util.SpringContextHolder;
import org.yx.lib.utils.util.StringUtil;
import util.AESUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static com.yx.web2.api.common.constant.Web2ApiConstants.SOURCE_TYPE_FROM_JOB;
import static com.yx.web2.api.common.constant.Web2ApiConstants.SOURCE_TYPE_FROM_WEB;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Web2ApiApplication.class)
public class Test {
    public Test() {
        MDC.put(CommonConstants.TRACE_ID, UUID.randomUUID().toString());
    }

    @org.junit.Test
    public void web2Test() {
        List<Integer> originalList = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            originalList.add(i);
        }

        int chunkSize = 3; // 每个小List的大小
        int listSize = originalList.size();
        int numChunks = (listSize + chunkSize - 1) / chunkSize; // 计算需要的分块数

        List<List<Integer>> chunkedLists = new ArrayList<>();
        for (int i = 0; i < numChunks; i++) {
            int fromIndex = i * chunkSize;
            int toIndex = Math.min(fromIndex + chunkSize, listSize);
            List<Integer> chunk = originalList.subList(fromIndex, toIndex);
            chunkedLists.add(chunk);
        }

        // 打印分块后的List
        for (List<Integer> chunk : chunkedLists) {
            System.out.println(chunk);
        }
    }

    @org.junit.Test
    public void dbPasswordTest() throws Exception {
//        String cipherText = ConfigTools.encrypt("admin2022!");
//        System.out.println("cipherText: " + cipherText);
//        String plainText = ConfigTools.decrypt(cipherText);
//        System.out.println("plainText: " + plainText);

        //  java -cp .\druid-1.1.13.jar com.alibaba.druid.filter.config.ConfigTools admin!123
//        String plainText = ConfigTools.decrypt(
//                "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAMChTuUtfKRnG92i0kiBD5+04MebZ9RNYCWzDdR41o3TvV73wjbci7md4p5QO/jM/o/ch95eCGmTPvAewTXuuJ0CAwEAAQ==",
//                "sMmST88gawYCqP2NgsoaAJCvvVEhA68CMiVOKXO8MJF8LnjUNwjxh0A3+MPV9k7cEPYRUrKNRMQ4RHB1mvUyxg==");
//        System.out.println("plainText: " + plainText);
        String sbc = AESUtil.encrypt("123456");
        String aa = AESUtil.decrypt("29D88DFD8EE08F6C7C719B9D0143555D");
        String bb = AESUtil.encrypt("admin2022!");
        System.out.println("aa: " + aa);
        System.out.println("bb: " + bb);
    }

    @org.junit.Test
    public void xToken() throws Exception {
//        String cipherText = ConfigTools.encrypt("admin2022!");
//        System.out.println("cipherText: " + cipherText);
//        String plainText = ConfigTools.decrypt(cipherText);
//        System.out.println("plainText: " + plainText);

        //  java -cp .\druid-1.1.13.jar com.alibaba.druid.filter.config.ConfigTools admin!123
//        String plainText = ConfigTools.decrypt(
//                "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAMChTuUtfKRnG92i0kiBD5+04MebZ9RNYCWzDdR41o3TvV73wjbci7md4p5QO/jM/o/ch95eCGmTPvAewTXuuJ0CAwEAAQ==",
//                "sMmST88gawYCqP2NgsoaAJCvvVEhA68CMiVOKXO8MJF8LnjUNwjxh0A3+MPV9k7cEPYRUrKNRMQ4RHB1mvUyxg==");
//        System.out.println("plainText: " + plainText);
//        String xUser = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1aWQiOjExMTExMTExMTEsImFrIjoiWVU5ZEs1cVlNYjV3RE5uUUhVQm8iLCJleHAiOjE3MjczNTAwMjQyNTIsIm5vbmNlIjoxNzI3MzQ2NDI0MjUyfQ.nV2FcVHS73WmrnjTqFn5GsJX47yV_vnVR2chiy2HImw=";
//        String decode = Base64Util.decode(xUser);
//        JSONObject jsonObject = JSON.parseObject(decode);

        String ak = "pkl4rq2x5ss2voqsv9ya";
        //  String sk = AESUtil.decrypt("CE928411D5480EE7B2002C6E0C8503644C20CDF7875D44BE455150B3F18F3AF4AA516C9B0CC89DF72083295D5D1B13E863A85D8646FE8A323BE9F8879C4F5534254037557971B6E0666BB57C6944D44F");
        String sk = "8cgwv3t9oiqtgck0zgk8k22oprxpswiwlc2is8deosqlsdy52f71heh1ymcwajeu";
        System.out.println("ak: " + ak);
        System.out.println("sk: " + sk);
        Long xTokenExpireS = 36000L;
        Map<String, Object> customerTokenPayload = Maps.newHashMap();
        customerTokenPayload.put("uid", 1111111111);
        String xToken = YxTokenBuilderUtil.buildXToken(ak, sk, xTokenExpireS, customerTokenPayload);
        System.out.println("xToken: " + xToken);
    }

    @org.junit.Test
    public void pmsTest() {
        PmsRemoteTenantService tenantService = SpringContextHolder.getBean(PmsRemoteTenantService.class);
        R<TenantInfoResp> r = tenantService.getTenantInfo(20247L);
        System.out.println(JSON.toJSON(r.getData().getAkSk()));
        System.out.println(JSON.toJSON(r.getData().getAkSk()));
        System.out.println(JSON.toJSON(r));
    }

    @org.junit.Test
    public void hpTest() {
        InstallmentCalculateService.InstallmentInput input = new InstallmentCalculateService.InstallmentInput();
        input.setServiceDuration(62);
        input.setServiceDurationPeriod(ServiceDurationPeriod.Day.getValue());
        input.setFreeServiceTermHours(0);
        input.setPrePaymentPrice(new BigDecimal("2.00"));
        input.setTotalAmount(new BigDecimal("9.00"));
        InstallmentCalculateService.InstallmentOutput output = SpringContextHolder.getBean(InstallmentCalculateService.class).calculate(input);
        System.out.println(JSON.toJSONString(output));
//        AccountModel accountModel = new AccountModel();
//        accountModel.setAccountId(173L);
//        accountModel.setAccountName("18510707707@163.com");
//        OrderEntity orderEntity = OrderEntity.builder()
//                .orderId("USD24101817292506250001")
//                .serviceDuration(1)
//                .serviceDurationPeriod(4)
//                .firstPaymentPrice(BigDecimal.valueOf(0.80))
//                .initialPrice(BigDecimal.valueOf(58.80))
//                .currentPrice(BigDecimal.valueOf(58.80))
//                .build();
//        List<OrderPaymentEntity> list = SpringContextHolder.getBean(IOrderPaymentService.class).prepareOrderInstalmentPlan(20247L, accountModel, orderEntity);
//        System.out.println("总价: " + orderEntity.getCurrentPrice());
//        System.out.println("首付款: " + list.get(0).getHpPrice());
//        System.out.println("分期月份: " + (list.size() - 1));
//
//        for (int i = 1; i <= list.size() - 1; i++) {
//            System.out.println("分期计划-" + (i) + ": " + list.get(i).getHpPrice());
//        }
    }

    @org.junit.Test
    public void orderTest() {
        AccountModel accountModel = new AccountModel();
        accountModel.setAccountId(20310L);
        accountModel.setAccountName("yechao.li");
        OrderIdReq orderIdReq = new OrderIdReq();
        orderIdReq.setOrderId("USD24111517316569080001");
        SpringContextHolder.getBean(IOrderService.class).confirmPaid(20310L, accountModel, orderIdReq, SOURCE_TYPE_FROM_WEB);

        List<DueOrderPayReq> dueOrderPayReq = Lists.newArrayList();
        DueOrderPayReq req = new DueOrderPayReq();
        req.setOrderId("USD24110417307181970001");
        req.setPaymentOrderIds(Lists.newArrayList("PAY24110517308072150001", "PAY24110517308072150002"));
        dueOrderPayReq.add(req);
        req = new DueOrderPayReq();
        req.setOrderId("USD24110517307831840001");
        req.setPaymentOrderIds(Lists.newArrayList("PAY24110517307869620001", "PAY24110517307869680002"));
        dueOrderPayReq.add(req);
        SpringContextHolder.getBean(IOrderService.class).duePayOrder(20247L, new AccountModel(), dueOrderPayReq);

        SpringContextHolder.getBean(IOrderPaymentService.class).updatePaymentPeriod(OrderEntity.builder()
                .orderId("USD24110417307181970001")
                .serviceDuration(67)
                .serviceDurationPeriod(1)
                .build());


        FeeCenterRemoteOrderService feeCenterOrderService = SpringContextHolder.getBean(FeeCenterRemoteOrderService.class);
        PmsRemoteOrderService pmsOrderService = SpringContextHolder.getBean(PmsRemoteOrderService.class);

        List<OrderCreateReq.Resource> resourceList = Lists.newArrayList();
        resourceList.add(OrderCreateReq.Resource.builder()
                .count(1)
                .spec("Mobile-L1")
                .subSpec("")
                .region("KR-Price")
                .resourcePool("ARS")
                .build()
        );
        OrderCreateReq orderCreateReq = OrderCreateReq.builder()
                .thirdPartyOrderCode("PAY" + UUID.randomUUID())
                .orderType(1)
                .autoRenew(false)
                .businessChannel(2)
                .tid(20246L)
                .orderPeriod(3)
                .orderDuration(2)
                .resources(resourceList)
                .build();
        System.out.println(JSON.toJSON(orderCreateReq));
        // create order
        R<OrderCreateResp> athOrderCreateRespR = feeCenterOrderService.createOrder(orderCreateReq);
        System.out.println(JSON.toJSON(athOrderCreateRespR));

        String dailyAth = new BigDecimal(athOrderCreateRespR.getData().getDailyAth()).multiply(BigDecimal.valueOf(2)).setScale(8, RoundingMode.UP).toString();
        System.out.println(dailyAth);
        // wallet transfer
        FeeCenterRemoteWalletService walletService = SpringContextHolder.getBean(FeeCenterRemoteWalletService.class);
        WalletTransferToTenantReq walletTransferToTenantReq = WalletTransferToTenantReq.builder()
                .fromTid(10000)
                .toTid(20246)
                .tid(20246L)
                .accountId(101L)
                .tenantType("GP")
                .amount(athOrderCreateRespR.getData().getAthTotal())
                .build();
        System.out.println(JSON.toJSON(walletTransferToTenantReq));
        R<WalletTransferResp> walletTransferRespR = walletService.transferToTenant(10000L, walletTransferToTenantReq);
        System.out.println(JSON.toJSON(walletTransferRespR));
        // add ath order info
        IAthOrderInfoService athOrderInfoService = SpringContextHolder.getBean(IAthOrderInfoService.class);
        athOrderInfoService.saveAthOrderInfo(AthOrderInfoEntity.builder()
                .orderId("USD" + UUID.randomUUID())
                .paymentOrderId("PAY" + UUID.randomUUID())
                .athOrderId(athOrderCreateRespR.getData().getOrderCode())
                .athTotal(athOrderCreateRespR.getData().getAthTotal())
                .dailyAth(athOrderCreateRespR.getData().getDailyAth())
                .createTime(new Timestamp(System.currentTimeMillis()))
                .build());
        // add transfer ath bill
        ITenantAthTransferRecordService tenantAthTransferRecordService = SpringContextHolder.getBean(ITenantAthTransferRecordService.class);
        tenantAthTransferRecordService.save(TenantAthTransferRecordEntity.builder()
                .orderId("USD" + UUID.randomUUID())
                .paymentOrderId("PAY" + UUID.randomUUID())
                .athOrderId(athOrderCreateRespR.getData().getOrderCode())
                .athBillId(walletTransferRespR.getData().getBillId().toString())
                .athAmount(new BigDecimal(dailyAth))
                .transFromTenantId(10000L)
                .transToTenantId(20246L)
                .transferStatus(walletTransferRespR.getCode())
                .failureReason(walletTransferRespR.getMsg())
                .createTime(new Timestamp(System.currentTimeMillis()))
                .build());

        // pay order
        R<?> r = pmsOrderService.payOrder(PayOrderReq.builder()
                .tid(20246L)
                .orderCode(athOrderCreateRespR.getData().getOrderCode())
                .build());
        System.out.println("+++++  " + JSON.toJSON(r));
        System.out.println("====================");
        if (r.getCode() != R.ok().getCode()) {
            walletTransferToTenantReq = WalletTransferToTenantReq.builder()
                    .fromTid(20246)
                    .toTid(10000)
                    .tid(20246L)
                    .accountId(101L)
                    .tenantType("GP")
                    .amount(dailyAth)
                    .build();
            System.out.println(JSON.toJSON(walletTransferToTenantReq));
            walletTransferRespR = walletService.transferToTenant(10000L, walletTransferToTenantReq);
            System.out.println(JSON.toJSON(walletTransferRespR));

            // add transfer ath bill
            tenantAthTransferRecordService.save(TenantAthTransferRecordEntity.builder()
                    .orderId("USD" + UUID.randomUUID())
                    .paymentOrderId("PAY" + UUID.randomUUID())
                    .athOrderId(athOrderCreateRespR.getData().getOrderCode())
                    .athBillId(walletTransferRespR.getData().getBillId().toString())
                    .athAmount(new BigDecimal(dailyAth))
                    .transFromTenantId(20246L)
                    .transToTenantId(10000L)
                    .transferStatus(walletTransferRespR.getCode())
                    .failureReason(walletTransferRespR.getMsg())
                    .createTime(new Timestamp(System.currentTimeMillis()))
                    .build());

            R<?> closeOrderR = feeCenterOrderService.orderClose(CloseOrderReq.builder()
                    .orderCode(athOrderCreateRespR.getData().getOrderCode())
                    .tid(20246L)
                    .build());
            System.out.println(closeOrderR);
        }
    }

    @org.junit.Test
    public void usdRateTest() {
        AthOrderPaymentService athOrderPaymentService = SpringContextHolder.getBean(AthOrderPaymentService.class);
        BigDecimal usdRate = athOrderPaymentService.getUsdRate(new AccountModel(),
                DateUtil.parseDateTime("2024-09-13 00:00:00"),
                DateUtil.parseDateTime("2024-09-13 23:59:59"), DateUtil.parseDateTime("2024-09-13 12:19:28"));
        System.out.println(usdRate);
    }

    @org.junit.Test
    public void stripeTest() {
        StripePaymentService stripePaymentService = SpringContextHolder.getBean(StripePaymentService.class);
        try {
            SubscriptionSchedule subscriptionSchedule = SubscriptionSchedule.retrieve("sub_sched_1Q8gVBGvp4ie1O8xmpesPSDd");
            subscriptionSchedule.cancel();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(100L)
                    .setCurrency(MonetaryUnit.USD.getValue())
                    .setCustomer("cus_QvOZGdkaLEPZdq")
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);
            System.out.println(paymentIntent.getClientSecret());

            List<PaymentMethod> paymentMethods = stripePaymentService.listPaymentMethod("cus_QvOZGdkaLEPZdq");
            for (PaymentMethod paymentMethod : paymentMethods) {
                if (!paymentMethod.getId().equals("pm_1Q4HrFGvp4ie1O8xwA5CsSnr")) {
                    paymentMethod.detach();
                }
            }
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }
    }

    @org.junit.Test
    public void docusignTest() {
        DocusignConfig docusignConfig = SpringContextHolder.getBean(DocusignConfig.class);
//        DocusignService docusignService = SpringContextHolder.getBean(DocusignService.class);
//        try {
//            Envelope envelope1 = docusignService.getEnvelope("f546b92a-4661-40e3-b5cd-36eca175f417");
//            String preViewUrl = docusignService.getEnvelopePreviewUrl("f546b92a-4661-40e3-b5cd-36eca175f417");
//        } catch (UnAuthorizationException e) {
//            throw new RuntimeException(e);
//        }

        Date date = DateUtil.parseDate("2024-11-18");
        date = DateUtils.addMonths(date, 1);
        //https://account-d.docusign.com/oauth/auth?response_type=code&scope=signature%20impersonation&client_id=05876220-154f-4e11-844f-920e37a93179&redirect_uri=http://localhost:8080/callback
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath("https://demo.docusign.net/restapi");
        apiClient.setOAuthBasePath("account-d.docusign.com");
        try {
            byte[] privateKeyBytes = Files.readAllBytes(Paths.get("C:\\data\\docusgin_private_key.key"));
            OAuth.OAuthToken oAuthToken = apiClient.requestJWTUserToken(
                    "05876220-154f-4e11-844f-920e37a93179",
                    "a68c7ddf-f2e2-43e6-bd8f-1e5f15929bc2",
                    Lists.newArrayList("signature"),
                    privateKeyBytes,
                    3600);
            String accessToken = oAuthToken.getAccessToken();
            OAuth.UserInfo userInfo = apiClient.getUserInfo(accessToken);
            String accountId = userInfo.getAccounts().get(0).getAccountId();

//            String accountId = "6cff5ba6-9f6c-4e87-8036-e74f829c8ea7";
//            String accessToken = "eyJ0eXAiOiJNVCIsImFsZyI6IlJTMjU2Iiwia2lkIjoiNjgxODVmZjEtNGU1MS00Y2U5LWFmMWMtNjg5ODEyMjAzMzE3In0.AQoAAAABAAUABwCAbph1_PPcSAgAgNZc1wT03EgCAN99jKbi8uZDvY8eXxWSm8IVAAEAAAAYAAEAAAAFAAAADQAkAAAAMDU4NzYyMjAtMTU0Zi00ZTExLTg0NGYtOTIwZTM3YTkzMTc5IgAkAAAAMDU4NzYyMjAtMTU0Zi00ZTExLTg0NGYtOTIwZTM3YTkzMTc5EgABAAAABgAAAGp3dF9iciMAJAAAADA1ODc2MjIwLTE1NGYtNGUxMS04NDRmLTkyMGUzN2E5MzE3OQ.FL64Pdt-_pkA2738Bm6D9KDFuk3YTe56-O3X2pXnI9UdQIatRuOHSsRRw3eR6foMyeOJO-jVPfvJv1_y_Ze5wSmdidIy7-7injMMExgUAx_Wo4mGnvP-gcmjcxY2cSXkYEzoqK6nCALfPH9BYiBRynt3YI7PUn6CvodYfbktZRQVyIFo6-xvy6fHR4KcNCakcT_kmHbuvFvRCceQvhakdD8LJBQV69A402gZz6lddIadSDl7FEzOv6vXoDd_68hWq-GQU4AlpQagJfY5XngTKmfH2MkIPP6Ntv_cZWY0Esq15PNugZflet88UbeCdw2dGjzTenYf4yz7FN0pfAlisg";
            System.out.println("accountId: " + accountId);
            System.out.println("accessToken: " + accessToken);

            apiClient.addDefaultHeader("Authorization", "Bearer " + accessToken);
            EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);

            EnvelopesApi.ListStatusChangesOptions options = envelopesApi.new ListStatusChangesOptions();
            options.setEnvelopeIds("f546b92a-4661-40e3-b5cd-36eca175f417,12022b97-8acc-4809-acb8-cd2fb3d978de,56ec2dd6-0070-46d8-a080-2a3340e2470e");
            EnvelopesInformation envelopesInformation = envelopesApi.listStatusChanges(accountId, options);

            Envelope getEnvelope = envelopesApi.getEnvelope(accountId, "f546b92a-4661-40e3-b5cd-36eca175f417");
            if (getEnvelope != null) {
                System.out.println(getEnvelope.getStatus());
//                byte[] document = envelopesApi.getDocument(accountId, getEnvelope.getEnvelopeId(), "1");
//                File file = new File("d:\\doc.txt");
//                try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
//                    fileOutputStream.write(document);
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
                ConsoleViewRequest request = new ConsoleViewRequest();
                request.setEnvelopeId(getEnvelope.getEnvelopeId());
                ViewUrl viewUrlApiResponse = envelopesApi.createConsoleView(accountId, request);
                System.out.println(JSON.toJSON(viewUrlApiResponse));
            }

            // Create envelopeDefinition object
            EnvelopeDefinition envelope = new EnvelopeDefinition();
            envelope.setEmailSubject("Please sign this document set Aethir Earth MSA " + DateUtil.year(new Date()) + " " + DateUtil.formatDateTime(new Date()));
            envelope.setStatus("sent");


            Signer tenantSigner = new Signer();
            tenantSigner.setEmail("1194706361@qq.com");
            tenantSigner.setName("在这签名");
            tenantSigner.recipientId("1");
            tenantSigner.setRoutingOrder("1");

            Signer aethirSigner = new Signer();
            aethirSigner.setEmail("1785402688@qq.com");
            aethirSigner.setName("aethirSigner");
            aethirSigner.recipientId("2");
            aethirSigner.setRoutingOrder("2");
//            aethirSigner.setRecipientType("agent");
//            aethirSigner.setClientUserId("3026166b-7592-4354-8870-7e827aacc60d");


//            SignHere tenantSignHere = new SignHere();
//            tenantSignHere.setDocumentId("1");
//            tenantSignHere.setPageNumber("8");
//            tenantSignHere.setXPosition("350");
//            tenantSignHere.setYPosition("165");

//            SignHere tenantSignHere = new SignHere();
//            tenantSignHere.setAnchorString("IN WITNESS WHEREOF, the Parties have duly executed and delivered this Order, as of the Effective Date."); // 锚点关键词
//            tenantSignHere.setAnchorXOffset("300"); // 右方偏移
//            tenantSignHere.setAnchorYOffset("80");  // 下方偏移
//            tenantSignHere.setAnchorUnits("pixels");
//            tenantSignHere.setDocumentId("1");
            SignHere signHere = new SignHere();
            signHere.setAnchorString("IN WITNESS WHEREOF, the Parties have duly executed and delivered this Order, as of the Effective Date."); // 锚点关键词
            signHere.setAnchorXOffset("300"); // 右方偏移
            signHere.setAnchorYOffset("80");  // 下方偏移
            signHere.setAnchorUnits("pixels");
            signHere.setDocumentId("1");




//            SignHere signAethirHere = new SignHere();
//            signAethirHere.setDocumentId("1");
//            signAethirHere.setPageNumber("8");
//            signAethirHere.setXPosition("50");
//            signAethirHere.setYPosition("145");


            Tabs tenantTabs = new Tabs();
            tenantTabs.setSignHereTabs(Arrays.asList(signHere));
            tenantSigner.setTabs(tenantTabs);

//            Tabs aethirTabs = new Tabs();
//            aethirTabs.setSignHereTabs(Arrays.asList(signAethirHere));
//            aethirSigner.setTabs(aethirTabs);

//            CarbonCopy cc = new CarbonCopy();
//            cc.setEmail(ccEmail);
//            cc.setName(ccName);
//            cc.recipientId("2");
            Recipients recipients = new Recipients();
            recipients.setSigners(Arrays.asList(tenantSigner, aethirSigner));

//            recipients.setSigners(Arrays.asList(tenantSigner));

//            recipients.setCarbonCopies(Arrays.asList(cc));
            envelope.setRecipients(recipients);


            // Add document
//            Document document = new Document();
//            document.setDocumentBase64("VGhhbmtzIGZvciByZXZpZXdpbmcgdGhpcyEKCldlJ2xsIG1vdmUgZm9yd2FyZCBhcyBzb29uIGFzIHdlIGhlYXIgYmFjay4=");
//            document.setName("doc1.txt");
//            document.setFileExtension("txt");
//            document.setDocumentId("1");
            Document document = new Document();
            byte[] data = FileUtils.readFileToByteArray(new File("C:\\data\\Aethir_Earth_MSA_Smart.docx"));


            document.setDocumentBase64(Base64.getEncoder().encodeToString(data));
            document.setName("Aethir Earth MSA 2024");
            document.setFileExtension("docx");
            document.setDocumentId("1");
            envelope.setDocuments(Arrays.asList(document));

            // Send envelope
            EnvelopeSummary results = envelopesApi.createEnvelope(accountId, envelope);


            System.out.println("Successfully sent envelope with envelopeId " + results.getEnvelopeId());
        } catch (Exception ex) {
            // 如果需要自动同意，参考：https://developers.docusign.com/platform/auth/consent/obtaining-admin-consent-internal/
            // 组织认证，参考：https://support.docusign.com/s/document-item?language=en_US&bundleId=rrf1583359212854&topicId=gso1583359141256.html&_LANG=enus
            if (ex.getMessage().contains("consent_required")) {
                String authorizeUrl = "https://" + docusignConfig.getOAuthBasePath() + "/oauth/auth?response_type=code&scope=signature%20impersonation%20correct&client_id=" +
                        docusignConfig.getClientId() + "&redirect_uri=http://localhost:8080/callback";
                System.out.println("authorizeUrl: " + authorizeUrl);
                // TODO return
                // 参加：https://developers.docusign.com/platform/auth/consent/obtaining-individual-consent/
                // return https://account-d.docusign.com/oauth/auth?response_type=code&scope=signature%20impersonation%20correct&client_id=05876220-154f-4e11-844f-920e37a93179&redirect_uri=http://localhost:8080/callback
            }
            if (ex.getMessage().contains("USER_AUTHENTICATION_FAILED")) {
                // TODO RE_LOGIN
            }
            System.out.println("error: " + ex.getMessage());
        }
    }

    @org.junit.Test
    public void replaceWord() {
        try {
            Map<String, Object> replaceData = Maps.newHashMap();
            replaceData.put("REPLACECREATEDATE", DateUtil.format(new Date(), "MM/dd,yyyy"));
            replaceData.put("CUSTOMERLEGALENTITYNAME", "customer_legal_entity_name");
            replaceData.put("CUSTOMERREGISTRATIONNUMBER", "customer_registration_number");
            replaceData.put("CUSTOMERLEGALENTITYADDRESS", "customer_legal_entity_address");
            replaceData.put("SIGNERNAME", "signer_name");
            replaceData.put("SIGNEREMAIL", "signer_email");
            replaceData.put("CONTRACTCOUNT", "1");//用户首次签订线上合同为#1，后续签订的按照整数递增
            replaceData.put("FIRSTSIGNCONTRACTDATE", DateUtil.format(new Date(), "MM/dd,yyyy"));//BD首次为客户创建合同时间
            replaceData.put("CONTRACTSTARTDATE", DateUtil.format(new Date(), "MM/dd,yyyy"));//BD填写的合同生效时间
            replaceData.put("HPPRICE", "1.22");//根据总价及分期数量计算的每月应付账款
            replaceData.put("CPREPAYMENT", "10.00");//DB输入的定金金额
            replaceData.put("FREESERVICETERMDAYS", "13");//BD设置的免费测试时间
            replaceData.put("SERVICEDURATION", "200");
            replaceData.put("SERVICEPERIOD", "Weeks");
            replaceData.put("TOTALAMOUNT", "100.00");
            replaceData.put("CONTRACTDEVICEDETAILS",
                    Lists.newArrayList(
                            MapBuilder.create(new HashMap<String, String>()).put("gpuType", "CPU TYPE")
                                    .put("region", "REGION")
                                    .put("count", "COUNT")
                                    .put("price", "PRICE")
                                    .build()));

//                    "GPU TYPE\t\tREGION\tCOUNT\t\tPRICE\r\n" +
//                            "NVIDA H100 x 8\t\tUSA\t\t2\t\t1.00$/h\r\n" +
//                            "NVIDA H100 x 6\t\tUSA\t\t200\t\t11.00$/h\r\n" +
//                            "NVIDA H100 x 4\t\tHong Kong\t\t21\t\t111.00$/h\r\n" +
//                            "NVIDA H100 x 2\t\tUSA\t\t19\t\t12.00$/h\r\n" +
//                            "NVIDA H100 x 10\t\tUSA\t\t2\t\t122.00$/h\r\n" +
//                            "NVIDA GTX4080 \t\tUSA\t\t2\t\t122.00$/h");

//            File tenantFile = SpringContextHolder.getBean(ContractServiceImpl.class).generateTenantContractDocFromTemplate("test001", 12345L, 1L);
//            SpringContextHolder.getBean(ContractServiceImpl.class).replacePlaceholdersInDocx(tenantFile, replaceData);

            FileUtils.copyFile(new File("C:\\Users\\EDY\\Desktop\\Aethir Earth MSA 2024.docx"), new File("C:\\Users\\EDY\\Desktop\\10000.docx"));
            File file = new File("C:\\Users\\EDY\\Desktop\\10000.docx");
            replacePlaceholdersInDocx(file, replaceData, (key, run) -> {
                Object newObj = replaceData.get(key);
                if (newObj instanceof String) {
                    run.setText(replaceData.get(key).toString(), 0);
                }
                if (newObj instanceof List) {
                    String title = "GPU TYPE \u0009 REGION \u0009 COUNT  \u0009 PRICE";
                    run.setText(title, 0);
                    run.addBreak();
                    String data = "NVIDA GTX4080 x 8 \u0009 Los Angeles \u0009 1022  \u0009 1.00$/h";
                    run.setText(data, run.getTextPosition());
                    run.addBreak();
                    data = "NVIDA H100 x 8 \u0009 Hong Kong \u0009 10  \u0009 221.00$/h";
                    run.setText(data, run.getTextPosition());
                    run.addBreak();
                    data = "NVIDA H100 x 8 \u0009 USA \u0009 10  \u0009 1.00$/h";
                    run.setText(data, run.getTextPosition());
                    run.addBreak();
                    data = "NVIDA H100 x 8 \u0009 USA \u0009 10  \u0009 1.00$/h";
                    run.setText(data, run.getTextPosition());
                }
            });
            System.out.println("ok");
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    private void replacePlaceholdersInDocx(File docFile, Map<String, Object> replacements, BiConsumer<String, XWPFRun> consumer) throws IOException {
        try (InputStream inputStream = FileUtils.openInputStream(docFile)) {
            try (XWPFDocument document = new XWPFDocument(inputStream)) {
                doReplace(document.getParagraphs(), replacements, consumer);
                // 遍历文档里面的table
                List<XWPFTable> tables = document.getTables();
                for (XWPFTable table : tables) {
                    List<XWPFTableRow> rows = table.getRows();
                    for (XWPFTableRow row : rows) {
                        List<XWPFTableCell> cells = row.getTableCells();
                        for (XWPFTableCell cell : cells) {
                            doReplace(cell.getParagraphs(), replacements, consumer);
                        }
                    }
                }
                try (FileOutputStream out = new FileOutputStream(docFile)) {
                    document.write(out);
                }
            }
        }
    }

    private void doReplace(List<XWPFParagraph> xwpfParagraphs, Map<String, Object> replacements, BiConsumer<String, XWPFRun> consumer) {
        for (XWPFParagraph paragraph : xwpfParagraphs) {
            // 获取段落中的文本块
            // 每个段落 (XWPFParagraph) 由一个或多个文本块 (XWPFRun) 组成，文本块是段落中的最小文本单位，可以包含格式化信息（如字体、颜色）。
            List<XWPFRun> runs = paragraph.getRuns();
            // 若包含文本块，对其做处理
            if (runs != null) {
                for (XWPFRun run : runs) {
                    // 获取文本块中的文本内容，参数0表示获取文本块中的第一段文本（注：通常只有一段文本）
                    String text = run.getText(0);
                    if (text != null) {
                        for (String key : replacements.keySet()) {
                            int index = text.indexOf(key);
                            if (index >= 0) {
//                                String newText = fn.apply(entry.getKey());
//                                run.setText(newText, 0);
                                consumer.accept(key, run);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    @org.junit.Test
    public void installmentCalculateTest() {
        InstallmentCalculateService.InstallmentInput input = new InstallmentCalculateService.InstallmentInput();
        input.setServiceDuration(1);
        input.setServiceDurationPeriod(ServiceDurationPeriod.Year.getValue());
        input.setFreeServiceTermHours(1);
        input.setPrePaymentPrice(new BigDecimal("30"));
        input.setInputDetails(Lists.newArrayList(
                input.new InstallmentInputDetail(3, "0.01"),
                input.new InstallmentInputDetail(5, "0.02")
        ));

        InstallmentCalculateService.InstallmentOutput output = SpringContextHolder.getBean(InstallmentCalculateService.class).calculate(input);
        System.out.println(JSON.toJSONString(output, true));
    }

    @org.junit.Test
    public void testJob() {
        try {
            // select contract list
            String startDate = DateUtil.format(DateUtil.offsetDay(DateUtil.date(), -5), "yyyy-MM-dd");
            String endDate = DateUtil.today();
            IContractService contractService = SpringContextHolder.getBean(IContractService.class);
            IOrderService orderService = SpringContextHolder.getBean(IOrderService.class);
            List<ContractEntity> contractsWithoutMatchingOrders = contractService.findContractsWithoutMatchingOrders(startDate, endDate);
            if (!contractsWithoutMatchingOrders.isEmpty()) {
                KvLogger.instance(this)
                        .p(LogFieldConstants.EVENT, Web2LoggerEvents.SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT)
                        .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT_ACTION_SELECT_CONTRACT)
                        .p("Count", contractsWithoutMatchingOrders.size())
                        .i();
                AccountModel accountModel = new AccountModel();
                for (ContractEntity contract : contractsWithoutMatchingOrders) {
                    if (StringUtil.isBlank(contract.getOrderId())) {
                        continue;
                    }
                    accountModel.setAccountId(contract.getTenantId());
                    accountModel.setAccountName(contract.getTenantName());
                    accountModel.setTenantId(contract.getTenantId());
                    accountModel.setTenantName(contract.getTenantName());
                    accountModel.setBdAccountId(contract.getBdAccountId());
                    accountModel.setBdAccountName(contract.getBdAccountName());
                    accountModel.setTenantType("Admin");
                    OrderIdReq orderIdReq = new OrderIdReq();
                    orderIdReq.setOrderId(contract.getOrderId());
                    try {
                        String xUser = YxTokenBuilderUtil.buildXUser(contract.getTenantId(), "Admin", 0L);
                        MDC.put(CommonConstants.X_USER, xUser);
                        R<?> r = orderService.confirmPaid(contract.getTenantId(), accountModel, orderIdReq, SOURCE_TYPE_FROM_JOB);
                        if (r.getCode() == R.ok().getCode()) {
                            KvLogger.instance(this)
                                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT)
                                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT_ACTION_SELECT_CONTRACT_CONFIRM_PAID)
                                    .p("TenantId", contract.getTenantId())
                                    .i();
                        } else {
                            KvLogger.instance(this)
                                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT)
                                    .p(LogFieldConstants.ERR_CODE, r.getCode())
                                    .p(LogFieldConstants.ERR_MSG, r.getMsg())
                                    .p("TenantId", contract.getTenantId())
                                    .e();
                        }
                    } catch (Exception e) {
                        KvLogger.instance(this)
                                .p(LogFieldConstants.EVENT, Web2LoggerEvents.SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT)
                                .p("TenantId", contract.getTenantId())
                                .p(LogFieldConstants.ERR_MSG, e.getMessage())
                                .e(e);
                    }
                }
            } else {
                KvLogger.instance(this)
                        .p(LogFieldConstants.EVENT, Web2LoggerEvents.SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT)
                        .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT_ACTION_SELECT_END_CONTRACT)
                        .p("Count", 0)
                        .i();
            }

        } catch (Exception ex) {
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT)
                    .p(LogFieldConstants.ERR_MSG, ex.getMessage())
                    .e(ex);
        }
        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT_ACTION_END)
                .i();
    }

    @org.junit.Test
    public void testConsumer() {
        KafkaConsumerForOrder kafkaConsumerForOrder = SpringContextHolder.getBean(KafkaConsumerForOrder.class);
        kafkaConsumerForOrder.idcPickOrderContainer("{\n" +
                "\t\"wholesaleTid\": 20247,\n" +
                "\t\"thirdPartyOrderCode\":\"PAY24112917328736790001\",\n" +
                "\t\"orderCode\": \"2002024112909480773721714\",\n" +
                "\t\"type\":\"1\", \n" +
                "\t\"status:\": \"\",\n" +
                "\t\"cids\": []\n" +
                "}");
    }

    @org.junit.Test
    public void test() {
        IOrderDeviceService orderDeviceService =SpringContextHolder.getBean(IOrderDeviceService.class);
        IOrderService orderService =SpringContextHolder.getBean(IOrderService.class);
        System.out.println(AopUtils.isAopProxy(orderService)); // 检查是否为代理
        System.out.println(AopUtils.isJdkDynamicProxy(orderService)); // 检查是否为JDK动态代理
        System.out.println(AopUtils.isCglibProxy(orderService));

        System.out.println(AopUtils.isAopProxy(orderDeviceService)); // 检查是否为代理
        System.out.println(AopUtils.isJdkDynamicProxy(orderDeviceService)); // 检查是否为JDK动态代理
        System.out.println(AopUtils.isCglibProxy(orderDeviceService));

        KafkaConsumerForOrder KafkaConsumerForOrder =SpringContextHolder.getBean(KafkaConsumerForOrder.class);
        System.out.println(AopUtils.isAopProxy(KafkaConsumerForOrder)); // 检查是否为代理
        System.out.println(AopUtils.isJdkDynamicProxy(KafkaConsumerForOrder)); // 检查是否为JDK动态代理
        System.out.println(AopUtils.isCglibProxy(KafkaConsumerForOrder));
        KafkaConsumerForOrder.idcPickOrderContainer("{}");


        OrderEntity orderEntity = orderService.getOrder("USD24120417333032270001");
        //update orderDevice discountPrice
        //orderEntity.getPublishPrice() / orderEntity.getInitialPrice() * unitPrice
        List<OrderDeviceEntity> orderDevices = orderDeviceService.getDeviceList(orderEntity.getOrderId());
        List<OrderDeviceEntity> updatedDevices = orderDevices.stream().map(device -> {
            BigDecimal publishPrice = orderEntity.getPublishPrice();
            BigDecimal initialPrice = orderEntity.getInitialPrice();
            if (device.getUnitPrice() == null) {
                return device;
            }
            BigDecimal unitPrice = new BigDecimal(device.getUnitPrice());
            // publishPrice / initialPrice * unitPrice
            BigDecimal newDiscountPrice = publishPrice.divide(initialPrice, 10, RoundingMode.HALF_UP)
                    .multiply(unitPrice)
                    .setScale(2, RoundingMode.HALF_UP);

            device.setDiscountPrice(newDiscountPrice.toPlainString());
            return device;
        }).collect(Collectors.toList());

        orderDeviceService.updateBatchById(updatedDevices);
        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT)
                .i();
    }


    @org.junit.Test
    public void test2() {
        IOrderContainerService orderContainerService =SpringContextHolder.getBean(IOrderContainerService.class);
        IOrderService orderService = SpringContextHolder.getBean(IOrderService.class);
        OrderEntity orderEntity = orderService.getOrder("USD24112917328721900001");
        orderContainerService.updateOrderContainersStatus(orderEntity, null, 2);
        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT)
                .i();
    }

//    @org.junit.Test
//    public void test3() {
//        IOrderService orderService = SpringContextHolder.getBean(IOrderService.class);
//        Web2ApiConfig web2ApiConfig = SpringContextHolder.getBean(Web2ApiConfig.class);
//        IOrderVirtualPaymentService orderVirtualPaymentService = SpringContextHolder.getBean(IOrderVirtualPaymentService.class);
//        Web3j web3j = Web3j.build(new HttpService(web2ApiConfig.getVirtualUrl()));
//        long jobId = XxlJobHelper.getJobId();
//        MDC.put(CommonConstants.TRACE_ID, String.valueOf(jobId));
//        KvLogger.instance(this)
//                .p(LogFieldConstants.EVENT, Web2LoggerEvents.VIRTUAL_PAY_ORDER_JOB_EVENT)
//                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.VIRTUAL_PAY_ORDER_EVENT_ACTION_START)
//                .p("JobId", MDC.get(CommonConstants.TRACE_ID))
//                .i();
//        try {
//            // select contract list
//            String startDate = DateUtil.format(DateUtil.offsetMinute(DateUtil.date(), -web2ApiConfig.getVirtualPaymentTime()), "yyyy-MM-dd HH:mm:ss");
//            String endDate = DateUtil.now();
//            List<OrderVirtualPaymentEntity> orderVirtualPaymentEntities = orderVirtualPaymentService.list(Wrappers.lambdaQuery(OrderVirtualPaymentEntity.class)
//                    .ge(OrderVirtualPaymentEntity::getCreateTime, startDate)
//                    .le(OrderVirtualPaymentEntity::getCreateTime, endDate)
//                    .eq(OrderVirtualPaymentEntity::getStatus, InstalmentPaymentStatus.None.getValue())
//                    .ne(OrderVirtualPaymentEntity::getHashCode, "")
//
//            );
//
//            if (orderVirtualPaymentEntities.isEmpty()) {
//                KvLogger.instance(this)
//                        .p(LogFieldConstants.EVENT, Web2LoggerEvents.VIRTUAL_PAY_ORDER_JOB_EVENT)
//                        .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.VIRTUAL_PAY_ORDER_EVENT_ACTION_END)
//                        .p("Count", 0)
//                        .p("JobId", MDC.get(CommonConstants.TRACE_ID))
//                        .i();
//                return;
//            }
//            KvLogger.instance(this)
//                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.VIRTUAL_PAY_ORDER_JOB_EVENT)
//                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.VIRTUAL_PAY_ORDER_EVENT_ACTION_SELECT)
//                    .p("Count", orderVirtualPaymentEntities.size())
//                    .p("JobId", MDC.get(CommonConstants.TRACE_ID))
//                    .i();
//
//            // group by hashcode
//            Map<String, List<OrderVirtualPaymentEntity>> groupedByHashCode = orderVirtualPaymentEntities.stream()
//                    .collect(Collectors.groupingBy(OrderVirtualPaymentEntity::getHashCode));
//
//            List<OrderPayVirtualReq> orderPayVirtualReqs = groupedByHashCode.entrySet().stream()
//                    .map(entry -> {
//                        String hashCode = entry.getKey();
//                        List<OrderVirtualPaymentEntity> entities = entry.getValue();
//
//                        OrderPayVirtualReq req = new OrderPayVirtualReq();
//                        req.setHashCode(hashCode);
//                        req.setFrom(entities.get(0).getFromAddress());
//                        req.setTo(entities.get(0).getToAddress());
//                        req.setAmount(entities.get(0).getAmount());
//                        req.setType(entities.get(0).getType());
//
//                        List<OrderPayVirtualReq.OrderPay> orderPayList = entities.stream()
//                                .collect(Collectors.groupingBy(OrderVirtualPaymentEntity::getOrderId))
//                                .entrySet().stream()
//                                .map(orderEntry -> {
//                                    OrderPayVirtualReq.OrderPay orderPay = new OrderPayVirtualReq.OrderPay();
//                                    orderPay.setOrderId(orderEntry.getKey());
//                                    orderPay.setOrderPaymentIdList(orderEntry.getValue().stream()
//                                            .map(OrderVirtualPaymentEntity::getPaymentOrderId)
//                                            .collect(Collectors.toList()));
//                                    return orderPay;
//                                })
//                                .collect(Collectors.toList());
//
//                        req.setOrderPayList(orderPayList);
//                        return req;
//                    })
//                    .collect(Collectors.toList());
//
//
//            // process access 10 threads
//            ExecutorService executor = Executors.newFixedThreadPool(10);
//            try {
//                // 提交所有任务到线程池，并等待它们全部完成
//
//                processTransaction(orderPayVirtualReqs.get(0));
//                CompletableFuture.allOf(
//                        orderPayVirtualReqs.stream()
//                                .map(entity -> CompletableFuture.runAsync(() -> processTransaction(entity), executor))
//                                .toArray(CompletableFuture[]::new)
//                ).join();
//
//            } finally {
//                executor.shutdown();
//            }
//
//            KvLogger.instance(this)
//                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.VIRTUAL_PAY_ORDER_JOB_EVENT)
//                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.VIRTUAL_PAY_ORDER_EVENT_ACTION_END)
//                    .p("JobId", MDC.get(CommonConstants.TRACE_ID))
//                    .i();
//
//
//        } catch (Exception ex) {
//            KvLogger.instance(this)
//                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.VIRTUAL_PAY_ORDER_JOB_EVENT)
//                    .p(LogFieldConstants.ERR_MSG, ex.getMessage())
//                    .p("JobId", MDC.get(CommonConstants.TRACE_ID))
//                    .e(ex);
//        } finally {
//            MDC.remove(CommonConstants.TRACE_ID);
//        }
//        KvLogger.instance(this)
//                .p(LogFieldConstants.EVENT, Web2LoggerEvents.VIRTUAL_PAY_ORDER_JOB_EVENT)
//                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.VIRTUAL_PAY_ORDER_EVENT_ACTION_END)
//                .p("JobId", MDC.get(CommonConstants.TRACE_ID))
//                .i();
//    }
//
//    private void processTransaction(OrderPayVirtualReq orderPayVirtualReq) {
//        IOrderService orderService = SpringContextHolder.getBean(IOrderService.class);
//        IOrderReconciliationExceptionService orderReconciliationExceptionService = SpringContextHolder.getBean(IOrderReconciliationExceptionService.class);
//        Web2ApiConfig web2ApiConfig = SpringContextHolder.getBean(Web2ApiConfig.class);
//        try {
////            OkHttpClient okHttpClient = new OkHttpClient.Builder()
////                    .connectTimeout(30, TimeUnit.SECONDS) // 设置连接超时时间
////                    .writeTimeout(30, TimeUnit.SECONDS)   // 设置写超时时间
////                    .readTimeout(30, TimeUnit.SECONDS)    // 设置读取超时时间
////                    .build();
//            Web3j web3j = Web3j.build(new HttpService(web2ApiConfig.getVirtualUrl()));
//            EthTransaction transaction = web3j.ethGetTransactionByHash(orderPayVirtualReq.getHashCode()).send();
//            Optional<Transaction> optionalTransaction = transaction.getTransaction();
//
//            if (optionalTransaction.isPresent()) {
//                Transaction tx = optionalTransaction.get();
//                String to = tx.getTo();
////                BigDecimal value = new BigDecimal(tx.getValue()).divide(BigDecimal.TEN.pow(18), 18, RoundingMode.HALF_UP);
//                if (StringUtil.isNotBlank(to)) {
//                    String input = tx.getInput();
//                    //ERC20 transfer
//                    if (input.startsWith("0xa9059cbb")) {
//                        String recipient = "0x" + input.substring(34, 74);
//                        String amountHex = input.substring(74);
//                        BigInteger amount = new BigInteger(amountHex, 16);
//
//                        // usdt has an accuracy of 6
//                        BigDecimal actualAmount = new BigDecimal(amount).divide(BigDecimal.TEN.pow(6), 6, RoundingMode.HALF_UP);
//                        EthGetTransactionReceipt receipt;
//                        try {
//                            receipt = web3j.ethGetTransactionReceipt(orderPayVirtualReq.getHashCode()).send();
//                        }catch (Exception ex){
//                            KvLogger.instance(this)
//                                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.VIRTUAL_PAY_ORDER_JOB_EVENT)
//                                    .p(LogFieldConstants.ERR_MSG, ex.getMessage())
//                                    .p("JobId", MDC.get(CommonConstants.TRACE_ID))
//                                    .i();
//                            return;
//                        }
//                        Optional<TransactionReceipt> transactionReceipt = receipt.getTransactionReceipt();
//                        if (transactionReceipt.isPresent()) {
//                            String status = transactionReceipt.get().getStatus();
//                            if ("0x1".equals(status)) {
//                                orderService.savePaymentRecord(orderPayVirtualReq, true);
//                            } else if ("0x0".equals(status)) {
//                                orderService.savePaymentRecord(orderPayVirtualReq, false);
//                            }
//                        }
//                        //The transfer amount is inconsistent with the actual amount needed or to a different address
//                        BigDecimal reqAmount = Convert.toBigDecimal(orderPayVirtualReq.getAmount(), BigDecimal.ZERO);
//                        if (reqAmount.compareTo(actualAmount) != 0 || !to.equalsIgnoreCase(orderPayVirtualReq.getTo())) {
//                            orderReconciliationExceptionService.saveBatchOrderReconciliationException(orderPayVirtualReq, actualAmount, reqAmount, to);
//                        }
//                    }
//                }
//
//            }
//        } catch (Exception ex) {
//            KvLogger.instance(this)
//                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.VIRTUAL_PAY_ORDER_JOB_EVENT)
//                    .p(LogFieldConstants.ERR_MSG, ex.getMessage())
//                    .p("JobId", MDC.get(CommonConstants.TRACE_ID))
//                    .e(ex);
//        }
//    }

     @org.junit.Test
    public void test4() {
         BigDecimal actualAmount = new BigDecimal("160000").divide(BigDecimal.TEN.pow(6), 6, RoundingMode.HALF_UP);
         BigDecimal req = new BigDecimal("0.16");
         log.info("22");
    }

    @org.junit.Test
    public void test3() {
        String orderId = "USD24111617317273650001";
        IOrderPaymentService orderPaymentService = SpringContextHolder.getBean(IOrderPaymentService.class);
        Timestamp nowTimestamp = new Timestamp(System.currentTimeMillis());

        List<OrderPaymentEntity> orderPayments = orderPaymentService.list(
                Wrappers.lambdaQuery(OrderPaymentEntity.class)
                        .eq(OrderPaymentEntity::getOrderId, orderId)
                        .eq(OrderPaymentEntity::getValidFlag, ValidFlagStatus.Effective.getValue())
                        .eq(OrderPaymentEntity::getPaymentStatus, InstalmentPaymentStatus.None.getValue())
                        .orderByAsc(OrderPaymentEntity::getDueDate)
        );

        if (orderPayments.isEmpty()) {
            return;
        }

        List<Long> updateIds = getUpdateIds(orderPayments, nowTimestamp);

        if (!updateIds.isEmpty()) {
            orderPaymentService.update(
                    Wrappers.lambdaUpdate(OrderPaymentEntity.class)
                            .set(OrderPaymentEntity::getValidFlag, ValidFlagStatus.InVain.getValue())
                            .in(OrderPaymentEntity::getId, updateIds)
            );
        }

    }

    private List<Long> getUpdateIds(List<OrderPaymentEntity> orderPayments, Timestamp nowTimestamp) {
        // 如果是预付款，更新所有记录
        if (orderPayments.get(0).getPrePayment()) {
            return orderPayments.stream()
                    .map(OrderPaymentEntity::getId)
                    .collect(Collectors.toList());
        }

        List<OrderPaymentEntity> notDuePayments = orderPayments.stream()
                .filter(payment -> payment.getDueDate() != null && payment.getDueDate().after(nowTimestamp))
                .collect(Collectors.toList());

        if (!notDuePayments.isEmpty()) {
            return notDuePayments.stream()
                    .skip(1)
                    .map(OrderPaymentEntity::getId)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    @org.junit.Test
    public void test22() {
        IContractService contractService = SpringContextHolder.getBean(IContractService.class);
        // create order
        ContractEntity contractEntity = getContractByEnvelopeId("33afda2e-b782-477d-9541-3f70ec610719");
        MDC.put(CommonConstants.X_USER, YxTokenBuilderUtil.buildXUser(contractEntity.getTenantId(), "GP", 0L));
        String orderId = SpringContextHolder.getBean(IContractService.class).contractToOrder(contractEntity);
        if (StringUtil.isBlank(orderId)) {
            KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_CREATE_ORDER_BY_CONTRACT)
                    .p(LogFieldConstants.TENANT_ID, contractEntity.getTenantId())
                    .p("OrderResourcePool", "BM")
                    .p("ContractId", contractEntity.getContractId())
                    .p(LogFieldConstants.ERR_MSG, "Not found spec")
                    .i();
            return ;
        }
        // update contract, set orderId
        contractService.update(Wrappers.lambdaUpdate(ContractEntity.class)
                .set(ContractEntity::getOrderId, orderId)
                .set(ContractEntity::getLastUpdateTime, new Timestamp(System.currentTimeMillis()))
                .eq(ContractEntity::getId, contractEntity.getId())
        );
        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ORDER_EVENT_ACTION_CREATE_ORDER_BY_CONTRACT)
                .p(LogFieldConstants.TENANT_ID, contractEntity.getTenantId())
                .flat("orderId", orderId)
                .p("OrderResourcePool", "BM")
                .p("OrderStatus", OrderStatus.WaitingPayment.getName())
                .p("ContractId", contractEntity.getContractId())
                .i();
        log.info("create order success") ;
    }

    private ContractEntity getContractByEnvelopeId(String envelopeId) {
         IContractService contractService = SpringContextHolder.getBean(IContractService.class);
        LambdaQueryWrapper<ContractEntity> queryWrapper = Wrappers.lambdaQuery(ContractEntity.class)
                .eq(ContractEntity::getEnvelopeId, envelopeId)
                .eq(ContractEntity::isDeleted, false);
        return contractService.getOne(queryWrapper);
    }


    @org.junit.Test
    public void test5() {
         try {
             IOrderVirtualPaymentService orderVirtualPaymentService = SpringContextHolder.getBean(IOrderVirtualPaymentService.class);
             String startDate = DateUtil.format(DateUtil.offsetMinute(DateUtil.date(), -30), "yyyy-MM-dd HH:mm:ss");
             // query overdue and unpaid data
             List<OrderVirtualPaymentEntity> orderVirtualPaymentOverdueEntities = orderVirtualPaymentService.list(Wrappers.lambdaQuery(OrderVirtualPaymentEntity.class)
                     .lt(OrderVirtualPaymentEntity::getLastUpdateTime, startDate)
                     .eq(OrderVirtualPaymentEntity::getStatus, InstalmentPaymentStatus.None.getValue())
             );
             if (orderVirtualPaymentOverdueEntities != null && !orderVirtualPaymentOverdueEntities.isEmpty()){
                 KvLogger.instance(this)
                         .p(LogFieldConstants.EVENT, Web2LoggerEvents.VIRTUAL_PAY_ORDER_JOB_EVENT)
                         .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.VIRTUAL_PAY_ORDER_EVENT_ACTION_SELECT)
                         .p("msg", "处理过期的订单数据")
                         .p("size", orderVirtualPaymentOverdueEntities.size())
                         .i();

                 //group the entities by orderId
                 Map<String, List<OrderVirtualPaymentEntity>> groupedByOrderId = orderVirtualPaymentOverdueEntities.stream()
                         .collect(Collectors.groupingBy(OrderVirtualPaymentEntity::getOrderId));
                 // delete expired data
                 List<OrderPayVirtualReq.OrderPay> orderPayList = groupedByOrderId.entrySet().stream()
                         .map(entry -> {
                             OrderPayVirtualReq.OrderPay orderPay = new OrderPayVirtualReq.OrderPay();
                             orderPay.setOrderId(entry.getKey());

                             // Extract payment IDs for this order
                             List<String> paymentIds = entry.getValue().stream()
                                     .map(OrderVirtualPaymentEntity::getPaymentOrderId)
                                     .collect(Collectors.toList());

                             orderPay.setOrderPaymentIdList(paymentIds);
                             return orderPay;
                         })
                         .collect(Collectors.toList());

                 for (OrderPayVirtualReq.OrderPay orderPay : orderPayList){
                     try {
                         orderVirtualPaymentService.savePaymentRecordFail(orderPay);
                         orderVirtualPaymentService.saveVirtualPaymentStatusFail(orderPay);
                     } catch (Exception ex) {
                         ex.printStackTrace();
                         KvLogger.instance(this)
                                 .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                                 .p(LogFieldConstants.ACTION, "savePaymentRecordFail")
                                 .p(LogFieldConstants.ERR_MSG, ex.getMessage())
                                 .p("OrderPay", JSON.toJSONString(orderPay))
                                 .e(ex);
                     }
                 }

             }
         }catch (Exception e){
             e.printStackTrace();
         }

    }

    @org.junit.Test
    public void test6() {
        KafkaConsumerForOrder kafkaConsumerForOrder = SpringContextHolder.getBean(KafkaConsumerForOrder.class);
        kafkaConsumerForOrder.idcPickOrderContainer("{\n" +
                "\t\"wholesaleTid\": 20958,\n" +
                "\t\"orderCode\": \"20020241227100907207197675\",\n" +
                "\t\"thirdPartyOrderCode\": \"PAY24122717352941030001\",\n" +
                "\t\"type\": 1,\n" +
                "\t\"status\": 4,\n" +
                "\t\"cids\": [\n" +
                "\t\t1000297172\n" +
                "\t]\n" +
                "}");
    }




}
