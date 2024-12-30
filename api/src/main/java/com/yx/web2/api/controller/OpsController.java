package com.yx.web2.api.controller;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.stripe.Stripe;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yx.lib.datasource.DataSourceType;
import org.yx.lib.datasource.config.RoutingDataSource;
import org.yx.lib.utils.util.AppStartTimeHolder;
import org.yx.lib.utils.util.SpringContextHolder;

import javax.sql.DataSource;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.*;

@RestController
@RequestMapping("/ops")
@RequiredArgsConstructor
public class OpsController {

    private final AppStartTimeHolder appStartTimeHolder;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${version}")
    private String projectVersion;

    @GetMapping(value = "/health")
    public JSONObject health() {
        JSONObject result = new JSONObject();
        JSONArray detail = new JSONArray();
        String gitVersion = "unKnow";
        String buildTime = "unKnow";
        try {
            Resource resource = new ClassPathResource("git.properties");
            Properties properties = new Properties();
            try (InputStream inputStream = resource.getInputStream()) {
                try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream)) {
                    properties.load(inputStreamReader);
                    gitVersion = properties.getProperty("git.commit.id.abbrev");
                    buildTime = properties.getProperty("git.build.time");
                }
            }
        } catch (Exception ignored) {
        }
        result.set("version", projectVersion + "." + gitVersion);
        result.set("buildTime", buildTime);
        result.set("startAt", appStartTimeHolder.getStartTime());
        result.set("runtimes", System.currentTimeMillis() / 1000L - appStartTimeHolder.getStartTime());
        result.set("stripeApiVersion", Stripe.API_VERSION);
        detail.add(checkRedis());
        detail.add(checkMySQL());
        result.put("detail", detail);
        Set<Integer> statusList = new HashSet<>();
        for (int i = 0; i < detail.size(); i++) {
            JSONObject detailObject = detail.getJSONObject(i);
            int status = detailObject.getInt("status");
            statusList.add(status);
        }
        result.set("health", (statusList.contains(NumberUtils.INTEGER_TWO) && statusList.size() == 1) ? 2 :
                (statusList.contains(NumberUtils.INTEGER_TWO) && statusList.size() > 1) ? 1 : 0);

        System.out.println(result);
        return result;
    }

    private JSONObject checkRedis() {
        JSONObject redis = new JSONObject();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            redis.set("point", "Redis");
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        redisTemplate.opsForValue().get("test");
                        redisTemplate.opsForValue().set("test", "health");
                    }
            );
            future.get(500, TimeUnit.MILLISECONDS);
            redis.set("status", NumberUtils.INTEGER_ZERO);
        } catch (Exception e) {
            redis.set("status", NumberUtils.INTEGER_TWO);
            redis.set("describe", e.getMessage());
        } finally {
            shutdownExecutor(executor);
        }
        return redis;
    }

    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    private JSONObject checkMySQL() {
        JSONObject mysql = new JSONObject();
        mysql.set("point", "Web2Mysql");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Map<Object, DataSource> dataSourceMap = SpringContextHolder.getBean(RoutingDataSource.class).getResolvedDataSources();
            CompletableFuture<Void> future = CompletableFuture.runAsync(() ->
                    new JdbcTemplate(dataSourceMap.get(DataSourceType.MASTER)).queryForObject("SELECT 1", Integer.class)
            );
            future.get(1000, TimeUnit.MILLISECONDS);
            mysql.set("status", NumberUtils.INTEGER_ZERO);
            mysql.set("master_status", NumberUtils.INTEGER_ZERO);
        } catch (TimeoutException e) {
            // 捕获超时异常，设置 MySQL 不可用状态
            mysql.set("status", NumberUtils.INTEGER_TWO);
            mysql.set("master_status", NumberUtils.INTEGER_TWO);
            mysql.set("describe", "MySQL 连接超时");
        } catch (Exception e) {
            mysql.set("status", NumberUtils.INTEGER_TWO);
            mysql.set("master_status", NumberUtils.INTEGER_TWO);
            mysql.set("describe", e.getMessage());
        } finally {
            shutdownExecutor(executor);
        }
        try {
            Map<Object, DataSource> dataSourceMap = SpringContextHolder.getBean(RoutingDataSource.class).getResolvedDataSources();
            CompletableFuture<Void> future = CompletableFuture.runAsync(() ->
                    new JdbcTemplate(dataSourceMap.get(DataSourceType.SLAVE)).queryForObject("SELECT 1", Integer.class)
            );
            future.get(1000, TimeUnit.MILLISECONDS);
            mysql.set("status", NumberUtils.INTEGER_ZERO);
            mysql.set("slave_status", NumberUtils.INTEGER_ZERO);
        } catch (TimeoutException e) {
            // 捕获超时异常，设置 MySQL 不可用状态
            mysql.set("status", NumberUtils.INTEGER_TWO);
            mysql.set("slave_status", NumberUtils.INTEGER_TWO);
            mysql.set("describe", "MySQL 连接超时");
        } catch (Exception e) {
            mysql.set("status", NumberUtils.INTEGER_TWO);
            mysql.set("slave_status", NumberUtils.INTEGER_TWO);
            mysql.set("describe", e.getMessage());
        } finally {
            shutdownExecutor(executor);
        }
        return mysql;
    }
}
