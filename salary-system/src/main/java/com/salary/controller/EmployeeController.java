package com.salary.controller;

import com.salary.common.PageResult;
import com.salary.common.Result;
import com.salary.entity.Employee;
import com.salary.service.EmployeeService;
import com.salary.service.SysLogService;
import com.salary.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Api(tags = "员工管理")
@RestController
@RequestMapping("/employee")
@RequiredArgsConstructor
@Slf4j
public class EmployeeController {

    private final EmployeeService employeeService;
    private final SysLogService sysLogService;
    private final JwtUtil jwtUtil;
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Path AVATAR_UPLOAD_DIR = Paths.get(System.getProperty("user.dir"), "uploads", "avatar")
            .toAbsolutePath()
            .normalize();

    @ApiOperation("分页查询员工")
    @GetMapping("/page")
    public Result<PageResult<Employee>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String empNo,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        Claims c = jwtUtil.parseToken(jwtUtil.extractToken(request.getHeader("Authorization")));
        Integer role = Integer.valueOf(c.get("role").toString());
        Long currentUserId = Long.valueOf(c.get("userId").toString());

        // 逻辑：如果是经理(role=2)，只能看自己名下的员工；管理员(role=1)看全部
        Long managerId = (role != null && role == 2) ? currentUserId : null;

        return Result.success(employeeService.page(current, size, empNo, realName, deptId, status, managerId));
    }

    @ApiOperation("分页查询部门经理")
    @GetMapping("/manager-page")
    public Result<PageResult<Employee>> managerPage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String empNo,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer status) {
        return Result.success(employeeService.pageManager(current, size, empNo, realName, deptId, status));
    }

    @ApiOperation("获取在职员工总数")
    @GetMapping("/stat/total")
    public Result<Long> count() {
        return Result.success(employeeService.count());
    }

    @ApiOperation("获取部门人数分布统计")
    @GetMapping("/stat/dept-dist")
    public Result<java.util.List<java.util.Map<String, Object>>> getDeptDist() {
        return Result.success(employeeService.getDeptDistribution());
    }

    @ApiOperation("获取员工详情")
    @GetMapping("/{id:[0-9]+}")
    public Result<Employee> detail(@PathVariable Long id) {
        return Result.success(employeeService.getById(id));
    }

    @ApiOperation("新增员工")
    @PostMapping
    public Result<Void> add(@RequestBody Employee employee,
                            @RequestParam(required = false, defaultValue = "123456") String initialPassword) {
        employeeService.addEmployee(employee, initialPassword);
        return Result.successMsg("新增成功");
    }

    @ApiOperation("修改员工")
    @PutMapping
    public Result<Void> update(@RequestBody Employee employee) {
        employeeService.updateEmployee(employee);
        return Result.successMsg("修改成功");
    }

    @ApiOperation("删除员工")
    @DeleteMapping("/{id:[0-9]+}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Employee employee = employeeService.getById(id);
        employeeService.deleteEmployee(id);
        Claims claims = jwtUtil.parseToken(jwtUtil.extractToken(request.getHeader("Authorization")));
        String module = isManagerRecord(employee) ? "部门经理" : "员工管理";
        String actionPrefix = isManagerRecord(employee) ? "删除部门经理[" : "删除员工[";
        String target = employee == null
                ? "ID=" + id
                : (employee.getRealName() == null ? "-" : employee.getRealName()) + "/" +
                (employee.getEmpNo() == null ? "-" : employee.getEmpNo());
        sysLogService.recordOperation(
                claims.get("username") != null ? claims.get("username").toString() : claims.getSubject(),
                claims.get("role") == null ? null : Integer.valueOf(claims.get("role").toString()),
                module,
                actionPrefix + target + "]");
        return Result.successMsg("删除成功");
    }

    @ApiOperation("通过 Excel 导入员工")
    @PostMapping("/import")
    public Result<Void> importExcel(@RequestParam("file") MultipartFile file) {
        employeeService.importByExcel(file);
        return Result.successMsg("导入成功");
    }

    @ApiOperation("上传员工头像")
    @PostMapping("/upload-avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("请选择头像文件后再上传");
        }

        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String normalizedExtension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(normalizedExtension)) {
            return Result.error("仅支持 jpg、jpeg、png、webp 格式头像");
        }

        try {
            Files.createDirectories(AVATAR_UPLOAD_DIR);
            String fileName = UUID.randomUUID().toString().replace("-", "") + "." + normalizedExtension;
            Path targetPath = AVATAR_UPLOAD_DIR.resolve(fileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return Result.success("/api/uploads/avatar/" + fileName);
        } catch (Exception e) {
            log.error("头像上传失败", e);
            return Result.error("头像上传失败：" + e.getMessage());
        }
    }


    @ApiOperation("导出员工 Excel")
    @GetMapping("/export")
    public void exportExcel(javax.servlet.http.HttpServletResponse response,
                            @RequestParam(required = false) String empNo,
                            @RequestParam(required = false) String realName,
                            @RequestParam(required = false) Long deptId) {
        employeeService.exportToExcel(response, empNo, realName, deptId);
    }

    private boolean isManagerRecord(Employee employee) {
        if (employee == null) {
            return false;
        }
        Integer roleValue = employee.getRole() != null ? employee.getRole() : employee.getUserRole();
        return roleValue != null && roleValue == 2;
    }
}
