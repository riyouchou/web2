package com.yx.web2.api.common.mq;


import com.yx.web2.api.service.mq.KafkaConsumerForOrder;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.logger.LogFieldConstants;

import static com.yx.web2.api.common.constant.Web2LoggerEvents.Actions.KAFKA_RECEIVE_WEB2_PRICK_ORDER_CONTAINER;
import static com.yx.web2.api.common.constant.Web2LoggerEvents.IDC_PICK_ORDER_CONTAINER_CONSUMER;

@Component
@RefreshScope
@RequiredArgsConstructor
public class KafkaMsgConsumer {

    private final KafkaConsumerForOrder kafkaConsumerForOrder;

    /**
     * 获取 计费推送的订单支付成功回调信息
     *
     * @param record topic: idc_pick_order_container
     *            body: {
     *            "wholesaleTid": 123456,
     *            "orderCode": "aaaaa",
     *            "type":"1" # 1.绑定  2 解绑
     *            "cids": [
     *            123,
     *            234,
     *            345
     *            ]
     *            }
     * @date 2024/11/28
     */
    @KafkaListener(id = "web2ReceivePrickOrderContainerConsumer-${applications.topic.listener.consumer-group-id.suffix}",
            topics = "${applications.topic.idc_pick_order_container}")
    public void idcPickOrderContainer(ConsumerRecord<String, String> record) {
        // 打印 offset、partition、topic 等信息
        KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, IDC_PICK_ORDER_CONTAINER_CONSUMER)
                .p(LogFieldConstants.ACTION, KAFKA_RECEIVE_WEB2_PRICK_ORDER_CONTAINER)
                .p("msg:", record.value())
                .p("Offset:", record.offset())
                .p("Partition:", record.partition())
                .p("Topic:", record.topic())
                .i();

        kafkaConsumerForOrder.idcPickOrderContainer(record.value());
    }

}