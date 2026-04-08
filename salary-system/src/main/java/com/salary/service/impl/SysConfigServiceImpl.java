package com.salary.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salary.entity.SysConfig;
import com.salary.mapper.SysConfigMapper;
import com.salary.service.SysConfigService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> 
        implements SysConfigService {

    private static final TypeReference<LinkedHashMap<String, String>> CONFIG_STORE_TYPE =
            new TypeReference<LinkedHashMap<String, String>>() {};

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Path getStorePath() {
        return Paths.get(System.getProperty("user.dir"), "uploads", "sys-config", "sys-config.json");
    }

    private Map<String, String> readStore() {
        Path storePath = getStorePath();
        if (!Files.exists(storePath)) {
            return new LinkedHashMap<>();
        }
        try (Reader reader = Files.newBufferedReader(storePath, StandardCharsets.UTF_8)) {
            Map<String, String> store = objectMapper.readValue(reader, CONFIG_STORE_TYPE);
            return store == null ? new LinkedHashMap<>() : new LinkedHashMap<>(store);
        } catch (IOException e) {
            throw new RuntimeException("读取系统配置失败", e);
        }
    }

    private void writeStore(Map<String, String> store) {
        Path storePath = getStorePath();
        try {
            Files.createDirectories(storePath.getParent());
            try (Writer writer = Files.newBufferedWriter(storePath, StandardCharsets.UTF_8)) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, store);
            }
        } catch (IOException e) {
            throw new RuntimeException("保存系统配置失败", e);
        }
    }

    @Override
    public synchronized SysConfig getByConfigKey(String key) {
        String value = readStore().get(key);
        if (value == null) {
            return null;
        }
        SysConfig config = new SysConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        return config;
    }

    @Override
    public synchronized void saveOrUpdateByKey(String key, String value) {
        Map<String, String> store = readStore();
        store.put(key, value);
        writeStore(store);
    }
}
