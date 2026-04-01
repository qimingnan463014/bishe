package com.salary.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.salary.entity.Employee;
import com.salary.entity.User;
import com.salary.mapper.EmployeeMapper;
import com.salary.mapper.UserMapper;
import com.salary.service.UserService;
import com.salary.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户认证 ServiceImpl
 * <p>
 * 职责：登录鉴权、JWT 签发、密码修改、图形验证码
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    private final UserMapper       userMapper;
    private final EmployeeMapper   employeeMapper;
    private final PasswordEncoder  passwordEncoder;
    private final JwtUtil          jwtUtil;
    
    // 使用本地缓存替代 Redis存储验证码（方便本地启动，无需配置 Redis 节点）
    private static final Map<String, String> CAPTCHA_STORE = new ConcurrentHashMap<>();

    // ====================================================
    //  管理端登录（统一登录入口）
    // ====================================================

    @Override
    public Map<String, Object> login(String username, String password, String captcha, String captchaKey) {
        // 1. 验证图形验证码
        validateCaptcha(captchaKey, captcha);

        // 2. 根据用户名查询（含密码字段）
        User user = userMapper.selectByUsernameWithPassword(username);
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 3. BCrypt 校验密码（增加对直接手改数据库引发的非安全哈希格式兼容）
        boolean pwdMatch = false;
        try {
            pwdMatch = passwordEncoder.matches(password, user.getPassword());
        } catch (IllegalArgumentException e) {
            log.warn("检测到数据库存在非标准 BCrypt 格式密码，正在回退明文校验。账号：{}", username);
        }
        
        // 如果哈希不匹配，且因为您手动在库里改成了明文，这里做一个终极兼容放行
        if (!pwdMatch && !password.equals(user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        // 4. 统一登录入口：不再限制普通员工必须走 /employee/login
        // 如果是员工 (role=3)，后续前端根据 token 的 role 展示不同的菜单即可

        // 5. 更新最后登录时间
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(updateUser);

        // 6. 签发 JWT
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 7. 组装返回数据（不含密码）
        Map<String, Object> result = new HashMap<>();
        result.put("token",    "Bearer " + token);
        result.put("userId",   user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("role",     user.getRole());
        result.put("avatar",   user.getAvatar());

        log.info("管理端登录成功：username={} role={}", username, user.getRole());
        return result;
    }

    // ====================================================
    //  员工前台登录
    // ====================================================

    @Override
    public Map<String, Object> employeeLogin(String username, String password) {
        User user = userMapper.selectByUsernameWithPassword(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        if (user.getRole() != 3) {
            throw new RuntimeException("管理员/经理请使用管理端登录");
        }

        // 更新登录时间
        User update = new User();
        update.setId(user.getId());
        update.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(update);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 额外返回员工的工号（员工端个人中心需要）
        Employee emp = employeeMapper.selectByUserId(user.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("token",    "Bearer " + token);
        result.put("userId",   user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("role",     user.getRole());
        result.put("empNo",    emp != null ? emp.getEmpNo() : null);
        result.put("empId",    emp != null ? emp.getId() : null);
        result.put("deptName", emp != null ? emp.getDeptName() : null);
        result.put("avatar",   user.getAvatar());

        log.info("员工端登录成功：username={}", username);
        return result;
    }

    // ====================================================
    //  修改密码
    // ====================================================

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        // 必须用含密码字段的查询
        User user = userMapper.selectByUsernameWithPassword(
                userMapper.selectById(userId).getUsername());
        if (user == null) throw new RuntimeException("用户不存在");

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码不正确");
        }
        if (newPassword.length() < 6) {
            throw new RuntimeException("新密码长度不能少于6位");
        }

        User update = new User();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(update);
        log.info("密码修改成功：userId={}", userId);
    }

    // ====================================================
    //  获取当前用户信息（个人中心）
    // ====================================================

    @Override
    public Object getCurrentUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return null;

        Map<String, Object> info = new HashMap<>();
        info.put("userId",   user.getId());
        info.put("username", user.getUsername());
        info.put("realName", user.getRealName());
        info.put("role",     user.getRole());
        info.put("email",    user.getEmail());
        info.put("avatar",   user.getAvatar());
        info.put("lastLoginTime", user.getLastLoginTime());

        // 若是员工，附加员工档案信息
        if (user.getRole() != null && user.getRole() == 3) {
            Employee emp = employeeMapper.selectByUserId(userId);
            if (emp != null) {
                info.put("empNo",     emp.getEmpNo());
                info.put("empId",     emp.getId());
                info.put("deptName",  emp.getDeptName());
                info.put("phone",     emp.getPhone());
                info.put("hireDate",  emp.getHireDate());
                info.put("bankAccount", emp.getBankAccount());
            }
        }
        return info;
    }

    // ====================================================
    //  图形验证码（本地 Map 存储示例）
    // ====================================================

    @Override
    public Map<String, String> generateCaptcha() {
        String captchaKey = UUID.randomUUID().toString().replace("-", "");
        // 1. 生成 4 位随机字符（排除易混淆字符）
        String chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz";
        StringBuilder code = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 4; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        String captchaCode = code.toString();

        // 2. 存入本地缓存
        CAPTCHA_STORE.put(captchaKey, captchaCode);

        // 3. 构建高复杂度 SVG 字符串
        StringBuilder svg = new StringBuilder("data:image/svg+xml;utf8,<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"130\" height=\"48\">");
        // 背景渐变感
        svg.append("<rect width=\"130\" height=\"48\" fill=\"%23f0f7ff\" rx=\"8\"/>");
        
        // 随机干扰线
        for (int i = 0; i < 4; i++) {
            int x1 = random.nextInt(130), y1 = random.nextInt(48);
            int x2 = random.nextInt(130), y2 = random.nextInt(48);
            svg.append(String.format("<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"rgba(%d,%d,%d,0.2)\" stroke-width=\"1\"/>",
                    x1, y1, x2, y2, random.nextInt(255), random.nextInt(255), random.nextInt(255)));
        }

        // 随机噪点
        for (int i = 0; i < 15; i++) {
            svg.append(String.format("<circle cx=\"%d\" cy=\"%d\" r=\"1\" fill=\"rgba(%d,%d,%d,0.3)\"/>",
                    random.nextInt(130), random.nextInt(48), random.nextInt(200), random.nextInt(200), random.nextInt(200)));
        }

        // 绘制文字
        String[] colors = {"%23409EFF", "%2367C23A", "%23E6A23C", "%23F56C6C", "%23909399", "%237a33ee"};
        for (int i = 0; i < 4; i++) {
            int rotate = random.nextInt(60) - 30; // 随机正负 30 度
            int x = 20 + i * 28;
            int y = 30 + random.nextInt(6) - 3;
            svg.append(String.format("<text x=\"%d\" y=\"%d\" font-size=\"26\" font-weight=\"bold\" fill=\"%s\" font-family=\"Arial\" transform=\"rotate(%d, %d, %d)\">%c</text>",
                    x, y, colors[random.nextInt(colors.length)], rotate, x, y, captchaCode.charAt(i)));
        }
        svg.append("</svg>");

        Map<String, String> result = new HashMap<>();
        result.put("captchaKey", captchaKey);
        result.put("captchaImg", svg.toString()); 
        log.debug("生成美化验证码：key={} code={}", captchaKey, captchaCode);
        return result;
    }

    /** 验证图形验证码 */
    private void validateCaptcha(String captchaKey, String inputCode) {
        if (captchaKey == null || inputCode == null) {
            throw new RuntimeException("请输入验证码");
        }
        String stored = CAPTCHA_STORE.get(captchaKey);
        if (stored == null) {
            throw new RuntimeException("验证码无效或已过期");
        }
        if (!stored.equalsIgnoreCase(inputCode)) {
            throw new RuntimeException("验证码错误");
        }
        CAPTCHA_STORE.remove(captchaKey);
    }
}
