package com.salary.filter;

import com.salary.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器
 * <p>
 * 在每次请求到达 Controller 前，拦截并解析 Token，
 * 若有效则将用户信息注入 Spring Security Context 中。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. 获取 Authorization 请求头
            String authHeader = request.getHeader("Authorization");
            String token = jwtUtil.extractToken(authHeader);

            // 2. 解析 Token 并验证
            if (StringUtils.hasText(token) && jwtUtil.isTokenValid(token)) {
                
                // 从 Token 取出关键数据
                Claims claims = jwtUtil.parseToken(token);
                String username = claims.getSubject();
                Integer roleId = Integer.valueOf(claims.get("role").toString());

                // 转换角色标识为 Spring Security Authority（如 ROLE_ADMIN）
                String roleName = mapRole(roleId);
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority(roleName);

                // 3. 构建 Authentication 对象并填充至上下文
                // 此处将 userId 存入 credentials 或者 principal 都可以，这里将其作为 details 附加信息存入
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, Collections.singletonList(authority));
                
                // 为了方便后续Controller（如 getCurrentUserId()）获取userId，将其设置到 details 中
                // 这里简单复用 WebAuthenticationDetailsSource，实际业务中可自定义 WebAuthenticationDetails
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // 为了极致方便，直接把原 Claims 塞进 details 里以便提取 userId
                authentication.setDetails(claims);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            log.error("JWT 过滤器认证异常：{}", e.getMessage());
            // 异常不中断链，Security会根据后面的拦截规则返回401
        }

        // 4. 继续执行过滤器链
        filterChain.doFilter(request, response);
    }

    /**
     * 将业务表 role (1=管理员, 2=经理, 3=员工) 映射到 Security Authority
     */
    private String mapRole(Integer roleId) {
        if (roleId == null) return "ROLE_USER";
        switch (roleId) {
            case 1:  return "ROLE_ADMIN";
            case 2:  return "ROLE_MANAGER";
            case 3:  return "ROLE_EMPLOYEE";
            default: return "ROLE_USER";
        }
    }
}
