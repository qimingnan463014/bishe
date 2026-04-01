package com.salary.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.salary.entity.User;

import java.util.Map;

/**
 * 用户/认证Service接口
 */
public interface UserService extends IService<User> {

    /**
     * 管理端登录
     * @param username 账号
     * @param password 密码
     * @param captcha 验证码
     * @param captchaKey 验证码标识
     * @return 返回 Token 及其它基本信息
     */
    Map<String, Object> login(String username, String password, String captcha, String captchaKey);

    /**
     * 员工前台登录
     */
    Map<String, Object> employeeLogin(String username, String password);

    /**
     * 修改密码
     *
     * @param userId      当前用户ID
     * @param oldPassword 旧密码（明文）
     * @param newPassword 新密码（明文）
     */
    void changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 获取当前登录用户信息（个人中心展示用）
     */
    Object getCurrentUserInfo(Long userId);

    /**
     * 生成图形验证码
     *
     * @return {"captchaKey": "uuid", "captchaImg": "base64..."}
     */
    Map<String, String> generateCaptcha();
}
