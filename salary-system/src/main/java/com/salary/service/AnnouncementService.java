package com.salary.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.salary.entity.Announcement;
import com.salary.common.PageResult;

public interface AnnouncementService extends IService<Announcement> {
    PageResult<Announcement> page(int current, int size, String title);
}
