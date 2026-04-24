package com.salary.controller;

import com.salary.service.SysLogService;
import com.salary.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
public class SystemController {

    private static final Pattern MYSQL_JDBC_PATTERN = Pattern.compile("^jdbc:mysql://([^/:?]+)(?::(\\d+))?/([^?]+).*$");
    private static final DateTimeFormatter BACKUP_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final JwtUtil jwtUtil;
    private final SysLogService sysLogService;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    @GetMapping("/backup")
    public void backupDatabase(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Claims claims = claims(request);
        Integer role = claims.get("role") == null ? null : Integer.valueOf(claims.get("role").toString());
        if (role == null || role != 1) {
            writeJsonError(response, HttpServletResponse.SC_FORBIDDEN, "仅系统管理员可执行数据备份");
            return;
        }

        MysqlConfig mysqlConfig;
        try {
            mysqlConfig = parseMysqlConfig(datasourceUrl);
        } catch (IllegalArgumentException ex) {
            writeJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
            return;
        }

        String mysqldumpExecutable = resolveMysqldumpExecutable();
        if (!StringUtils.hasText(mysqldumpExecutable)) {
            writeJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "未找到 mysqldump.exe，请检查 MySQL 安装路径");
            return;
        }

        List<String> command = new ArrayList<>();
        command.add(mysqldumpExecutable);
        command.add("--default-character-set=utf8mb4");
        command.add("--single-transaction");
        command.add("--skip-lock-tables");
        command.add("--routines");
        command.add("--events");
        command.add("--triggers");
        command.add("--host=" + mysqlConfig.host);
        command.add("--port=" + mysqlConfig.port);
        command.add("--user=" + datasourceUsername);
        command.add(mysqlConfig.database);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        if (StringUtils.hasText(datasourcePassword)) {
            processBuilder.environment().put("MYSQL_PWD", datasourcePassword);
        }

        byte[] dumpBytes;
        try {
            Process process = processBuilder.start();
            ByteArrayOutputStream dumpBuffer = new ByteArrayOutputStream();
            process.getInputStream().transferTo(dumpBuffer);
            int exitCode = process.waitFor();
            dumpBytes = dumpBuffer.toByteArray();
            if (exitCode != 0) {
                String errorText = new String(dumpBytes, StandardCharsets.UTF_8).trim();
                writeJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        StringUtils.hasText(errorText) ? errorText : "数据库备份失败");
                return;
            }
            if (dumpBytes.length == 0) {
                writeJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "数据库备份结果为空，请稍后重试");
                return;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            writeJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "数据库备份被中断，请重试");
            return;
        } catch (IOException ex) {
            writeJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "无法执行 mysqldump：" + ex.getMessage());
            return;
        }

        String fileName = mysqlConfig.database + "-backup-" + BACKUP_TIME_FORMAT.format(LocalDateTime.now()) + ".sql";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        String username = claims.get("username") != null ? claims.get("username").toString() : claims.getSubject();
        sysLogService.recordOperation(username, role, "系统管理", "下载数据库备份[" + mysqlConfig.database + "]");

        response.reset();
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/sql;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
        response.setContentLength(dumpBytes.length);
        response.getOutputStream().write(dumpBytes);
        response.flushBuffer();
    }

    private Claims claims(HttpServletRequest request) {
        return jwtUtil.parseToken(jwtUtil.extractToken(request.getHeader("Authorization")));
    }

    private MysqlConfig parseMysqlConfig(String jdbcUrl) {
        Matcher matcher = MYSQL_JDBC_PATTERN.matcher(jdbcUrl);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("当前数据源不是标准 MySQL 地址，暂不支持自动备份");
        }
        String host = matcher.group(1);
        String port = matcher.group(2) == null ? "3306" : matcher.group(2);
        String database = matcher.group(3);
        return new MysqlConfig(host, port, database);
    }

    private String resolveMysqldumpExecutable() {
        List<Path> candidates = new ArrayList<>();
        String pathEnv = System.getenv("PATH");
        if (StringUtils.hasText(pathEnv)) {
            for (String entry : pathEnv.split(";")) {
                if (!StringUtils.hasText(entry)) {
                    continue;
                }
                candidates.add(Paths.get(entry.trim(), "mysqldump.exe"));
            }
        }
        candidates.add(Paths.get("E:\\mysql\\mysql-8.1.0-winx64\\bin\\mysqldump.exe"));
        candidates.add(Paths.get("C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysqldump.exe"));
        for (Path candidate : candidates) {
            if (candidate != null && Files.isRegularFile(candidate)) {
                return candidate.toAbsolutePath().toString();
            }
        }
        return null;
    }

    private void writeJsonError(HttpServletResponse response, int status, String message) throws IOException {
        response.reset();
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + status + ",\"message\":\"" + escapeJson(message) + "\"}");
        response.getWriter().flush();
    }

    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static class MysqlConfig {
        private final String host;
        private final String port;
        private final String database;

        private MysqlConfig(String host, String port, String database) {
            this.host = host;
            this.port = port;
            this.database = database;
        }
    }
}
