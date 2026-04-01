package com.salary.controller;

import com.salary.common.PageResult;
import com.salary.common.Result;
import com.salary.entity.Announcement;
import com.salary.service.AnnouncementService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@Api(tags = "Announcement Management")
@RestController
@RequestMapping("/announcement")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @ApiOperation("List announcements")
    @GetMapping("/list")
    public Result<PageResult<Announcement>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String title) {
        return Result.success(announcementService.page(page, size, title));
    }

    @ApiOperation("Add announcement")
    @PostMapping
    public Result<Void> add(@RequestBody Announcement announcement) {
        announcement.setPubTime(LocalDateTime.now());
        announcementService.save(announcement);
        return Result.successMsg("Added successfully");
    }

    @ApiOperation("Update announcement")
    @PutMapping
    public Result<Void> update(@RequestBody Announcement announcement) {
        announcementService.updateById(announcement);
        return Result.successMsg("Updated successfully");
    }

    @ApiOperation("Delete announcement")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        announcementService.removeById(id);
        return Result.successMsg("Deleted successfully");
    }
}
