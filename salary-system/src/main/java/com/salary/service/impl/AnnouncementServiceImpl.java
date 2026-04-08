package com.salary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.salary.common.PageResult;
import com.salary.entity.Announcement;
import com.salary.mapper.AnnouncementMapper;
import com.salary.service.AnnouncementService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement> implements AnnouncementService {

    @Override
    public PageResult<Announcement> page(int current, int size, String title, Integer status) {
        Page<Announcement> page = new Page<>(current, size);
        LambdaQueryWrapper<Announcement> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(title)) {
            qw.like(Announcement::getTitle, title);
        }
        if (status != null) {
            qw.eq(Announcement::getStatus, status);
        }
        qw.orderByDesc(Announcement::getPubTime)
                .orderByDesc(Announcement::getCreateTime)
                .orderByDesc(Announcement::getId);
        this.page(page, qw);
        return PageResult.of(page);
    }
}
