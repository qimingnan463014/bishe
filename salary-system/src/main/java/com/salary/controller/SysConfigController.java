package com.salary.controller;

import com.salary.common.Result;
import com.salary.entity.SysConfig;
import com.salary.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/sys-config")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService sysConfigService;

    /** 获取指定配置 */
    @GetMapping("/{key}")
    public Result<SysConfig> getConfig(@PathVariable String key) {
        return Result.success(sysConfigService.getByConfigKey(key));
    }

    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    /** 保存配置 */
    @PostMapping("/save")
    public Result<Void> saveConfig(@RequestBody Map<String, String> body) {
        String key = body.get("key");
        String value = body.get("value");
        if (key == null || value == null) {
            return Result.error("参数错误");
        }
        sysConfigService.saveOrUpdateByKey(key, value);
        return Result.success();
    }

    /** 物理重置数据库数据 (仅用于演示/开发) */
    @PostMapping("/reset-data")
    public Result<String> resetData() {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get("reset_data.sql");
            if (!java.nio.file.Files.exists(path)) {
                // 尝试在父目录寻找
                path = java.nio.file.Paths.get("salary-system/reset_data.sql");
            }
            if (!java.nio.file.Files.exists(path)) {
                return Result.error("找不到 reset_data.sql 文件");
            }

            String content = new String(java.nio.file.Files.readAllBytes(path), java.nio.charset.StandardCharsets.UTF_8);
            String[] sqls = content.split(";");
            int count = 0;
            for (String sql : sqls) {
                String cleanSql = sql.trim();
                if (cleanSql.isEmpty() || cleanSql.startsWith("--")) continue;
                jdbcTemplate.execute(cleanSql);
                count++;
            }
            return Result.success("成功执行 " + count + " 条 SQL 语句，数据已重置");
        } catch (Exception e) {
            return Result.error("重置失败: " + e.getMessage());
        }
    }
}
