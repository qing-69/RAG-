package com.xiaoyan.aiassistant.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 启动时加载工作目录下的 .env，让 IDEA 直接 Run 也能读到配置；系统环境变量优先生效。
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "dotEnvFile";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path dotenv = Path.of(".env");
        if (!Files.exists(dotenv)) {
            return;
        }
        Map<String, Object> props = new LinkedHashMap<>();
        try {
            List<String> lines = Files.readAllLines(dotenv, StandardCharsets.UTF_8);
            for (String line : lines) {
                String trimmed = line.trim();
                // 跳过空行和注释行。
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int eq = trimmed.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();
                // 去掉成对包裹的引号（MYSQL_URL 含 & 等字符时常被引号包起来）。
                if (value.length() >= 2
                        && ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'")))) {
                    value = value.substring(1, value.length() - 1);
                }
                // 已由系统环境变量或 JVM 属性提供的键不覆盖，保证外部配置优先。
                if (environment.getSystemEnvironment().containsKey(key)
                        || environment.getSystemProperties().containsKey(key)) {
                    continue;
                }
                props.put(key, value);
            }
        } catch (IOException e) {
            // .env 读取失败不阻断启动，回退到 application.yml 默认值。
            System.err.println("[DotenvEnvironmentPostProcessor] 读取 .env 失败，使用默认配置: " + e.getMessage());
            return;
        }
        if (!props.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, props));
        }
    }
}
