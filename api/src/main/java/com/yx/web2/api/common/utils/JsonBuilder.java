package com.yx.web2.api.common.utils;

import com.alibaba.fastjson.JSONObject;

public class JsonBuilder {

    private JSONObject jsonObject;

    public JsonBuilder() {
        this.jsonObject = new JSONObject();
    }

    public JsonBuilder add(String key, Object value) {
        this.jsonObject.put(key, value);
        return this;
    }

    public String build() {
        return this.jsonObject.toJSONString();
    }

    public static void main(String[] args) {
        // 使用 Builder 模式逐步构建 JSON 对象
        String jsonString = new JsonBuilder()
            .add("resourcePool", "Tenant123")
            .add("isPriceRegion", true)
            .add("extraField", 123)
            .build();
        System.out.println(jsonString);
    }
}
