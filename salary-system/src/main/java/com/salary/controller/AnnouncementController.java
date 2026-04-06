package com.salary.controller;

import com.salary.common.PageResult;
import com.salary.common.Result;
import com.salary.entity.Announcement;
import com.salary.service.AnnouncementService;
import com.salary.service.SysLogService;
import com.salary.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Api(tags = "Announcement Management")
@RestController
@RequestMapping("/announcement")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final SysLogService sysLogService;
    private final JwtUtil jwtUtil;

    @ApiOperation("List announcements")
    @GetMapping("/list")
    public Result<PageResult<Announcement>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer status) {
        return Result.success(announcementService.page(page, size, title, status));
    }

    @ApiOperation("Get announcement detail")
    @GetMapping("/{id}")
    public Result<Announcement> detail(@PathVariable Long id) {
        return Result.success(announcementService.getById(id));
    }

    @ApiOperation("Add announcement")
    @PostMapping
    public Result<Void> add(@RequestBody Announcement announcement) {
        if (announcement.getStatus() == null) {
            announcement.setStatus(1);
        }
        if (announcement.getIsTop() == null) {
            announcement.setIsTop(0);
        }
        announcement.setPubTime(LocalDateTime.now());
        announcementService.save(announcement);
        return Result.successMsg("Added successfully");
    }

    @ApiOperation("Update announcement")
    @PutMapping
    public Result<Void> update(@RequestBody Announcement announcement) {
        if (announcement.getStatus() == null) {
            announcement.setStatus(1);
        }
        if (announcement.getIsTop() == null) {
            announcement.setIsTop(0);
        }
        if (announcement.getPubTime() == null) {
            announcement.setPubTime(LocalDateTime.now());
        }
        announcementService.updateById(announcement);
        return Result.successMsg("Updated successfully");
    }

    @ApiOperation("Delete announcement")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Announcement announcement = announcementService.getById(id);
        announcementService.removeById(id);
        Claims claims = claims(request);
        sysLogService.recordOperation(
                claims.get("username") != null ? claims.get("username").toString() : claims.getSubject(),
                claims.get("role") == null ? null : Integer.valueOf(claims.get("role").toString()),
                "公告管理",
                "删除公告[" + (announcement != null ? announcement.getTitle() : "ID=" + id) + "]");
        return Result.successMsg("Deleted successfully");
    }

    private Claims claims(HttpServletRequest request) {
        return jwtUtil.parseToken(jwtUtil.extractToken(request.getHeader("Authorization")));
    }
}
