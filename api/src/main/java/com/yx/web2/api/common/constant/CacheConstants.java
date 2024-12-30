package com.yx.web2.api.common.constant;

import org.yx.lib.utils.constant.GlobalCacheConstants;

public interface CacheConstants {

    String CACHE_MODEL_NAME = GlobalCacheConstants.GLOBALLY + "web2:";
    String ORDER_IDEMPOTENT = CACHE_MODEL_NAME + "order:idempotent:%s";
    String ORDER_ID_GEN = CACHE_MODEL_NAME + "order:id_gen:%s";
    String DOCUSIGN_ACCESS_TOKEN = CACHE_MODEL_NAME + "docusign:uid:%s";
    String DOCUSIGN_ACCOUNT_ID = CACHE_MODEL_NAME + "docusign:account:%s";
    String CONTRACT_IDEMPOTENT = CACHE_MODEL_NAME + "contract:idempotent:%s";
    String JOB = CACHE_MODEL_NAME + "job:%s";
}
