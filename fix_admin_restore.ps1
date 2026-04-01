$ErrorActionPreference = 'Stop'

$path = 'E:\bishe\salary-system\src\main\resources\static\admin\index.html'
$backup = 'E:\bishe\salary-system\src\main\resources\static\admin\index.restore.backup.html'

$lines = [System.Collections.Generic.List[string]]::new()
Get-Content $path -Encoding UTF8 | ForEach-Object { [void]$lines.Add($_) }
[System.IO.File]::WriteAllLines($backup, $lines, [System.Text.UTF8Encoding]::new($false))

function Replace-Line {
    param(
        [int]$LineNumber,
        [string]$Text
    )
    $script:lines[$LineNumber - 1] = $Text
}

function Replace-Range {
    param(
        [int]$StartLine,
        [int]$EndLine,
        [string[]]$NewLines
    )
    $start = $StartLine - 1
    $count = $EndLine - $StartLine + 1
    $script:lines.RemoveRange($start, $count)
    for ($i = 0; $i -lt $NewLines.Count; $i++) {
        $script:lines.Insert($start + $i, $NewLines[$i])
    }
}

# Basic title and page header text
Replace-Line 6 '    <title>管理员后台 - 员工薪资管理系统</title>'
Replace-Line 596 '            首页 | <span class="breadcrumb-current">{{ currentMenuLabel }}</span>'

# Personal center block
$personalBlock = @'
                <!-- ===== 个人中心 ===== -->
                <div v-if="userInfo.role === 1" style="max-width:520px; margin:0 auto;">
                    <el-card shadow="hover" style="border-radius:18px; margin-bottom:16px;">
                        <template #header>
                            <div style="font-weight:bold;font-size:15px;">管理员信息</div>
                        </template>
                        <el-descriptions :column="1" border>
                            <el-descriptions-item label="登录账号">{{ userInfo.username }}</el-descriptions-item>
                            <el-descriptions-item label="显示名称">{{ userInfo.realName || userInfo.username }}</el-descriptions-item>
                            <el-descriptions-item label="系统角色"><el-tag type="danger">系统管理员</el-tag></el-descriptions-item>
                        </el-descriptions>
                    </el-card>
                    <el-card shadow="hover" style="border-radius:18px;">
                        <template #header>
                            <div style="font-weight:bold;font-size:15px;">修改登录密码</div>
                        </template>
                        <el-form :model="pwdForm" label-width="90px" size="small">
                            <el-form-item label="当前密码"><el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前密码"></el-input></el-form-item>
                            <el-form-item label="新密码"><el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码"></el-input></el-form-item>
                            <el-form-item label="确认密码"><el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码"></el-input></el-form-item>
                            <el-form-item><el-button type="primary" @click="updatePassword">修改密码</el-button></el-form-item>
                        </el-form>
                    </el-card>
                </div>

                <el-row :gutter="24" v-else>
                    <el-col :span="14">
                        <el-card shadow="hover" style="border-radius:18px; margin-bottom:20px;">
                            <template #header>
                                <div style="font-weight:bold;font-size:15px;">个人信息</div>
                            </template>
                            <div style="display:flex;gap:20px;align-items:center;margin-bottom:20px;">
                                <div style="width:72px;height:72px;border-radius:50%;overflow:hidden;border:3px solid #e8f0fa;background:#f0f5fb;display:flex;align-items:center;justify-content:center;flex-shrink:0;">
                                    <img v-if="selfInfo.avatar" :src="selfInfo.avatar" style="width:100%;height:100%;object-fit:cover;" />
                                    <el-icon v-else :size="32" style="color:#b0c4de"><User /></el-icon>
                                </div>
                                <div>
                                    <div style="font-size:17px;font-weight:bold;color:#303133;">{{ selfInfo.realName || userInfo.realName || userInfo.username }}</div>
                                    <el-tag type="warning" style="margin-top:6px;">部门经理</el-tag>
                                </div>
                            </div>
                            <el-descriptions :column="2" border size="small">
                                <el-descriptions-item label="登录账号">{{ userInfo.username }}</el-descriptions-item>
                                <el-descriptions-item label="工号">{{ selfInfo.empNo || '-' }}</el-descriptions-item>
                                <el-descriptions-item label="姓名">{{ selfInfo.realName || '-' }}</el-descriptions-item>
                                <el-descriptions-item label="性别">{{ selfInfo.gender === 1 ? '男' : selfInfo.gender === 2 ? '女' : '-' }}</el-descriptions-item>
                                <el-descriptions-item label="手机">{{ selfInfo.phone || '-' }}</el-descriptions-item>
                                <el-descriptions-item label="部门">{{ selfInfo.deptName || '-' }}</el-descriptions-item>
                                <el-descriptions-item label="基本工资">{{ selfInfo.baseSalary != null ? ('￥' + selfInfo.baseSalary) : '-' }}</el-descriptions-item>
                                <el-descriptions-item label="银行卡号">{{ selfInfo.bankCard || '-' }}</el-descriptions-item>
                                <el-descriptions-item label="岗位">{{ selfInfo.positionName || selfInfo.position || '-' }}</el-descriptions-item>
                                <el-descriptions-item label="入职时间">{{ selfInfo.hireDate || '-' }}</el-descriptions-item>
                            </el-descriptions>
                        </el-card>
                    </el-col>
                    <el-col :span="10">
                        <el-card shadow="hover" style="border-radius:18px; margin-bottom:16px;">
                            <template #header>
                                <div style="font-weight:bold;font-size:15px;">修改资料</div>
                            </template>
                            <el-form :model="selfEditForm" label-width="80px" size="small">
                                <el-form-item label="手机号"><el-input v-model="selfEditForm.phone" placeholder="请输入手机号"></el-input></el-form-item>
                                <el-form-item label="银行卡号"><el-input v-model="selfEditForm.bankCard" placeholder="请输入银行卡号"></el-input></el-form-item>
                                <el-form-item><el-button type="primary" @click="saveSelfInfo">保存资料</el-button></el-form-item>
                            </el-form>
                        </el-card>
                        <el-card shadow="hover" style="border-radius:18px;">
                            <template #header>
                                <div style="font-weight:bold;font-size:15px;">修改登录密码</div>
                            </template>
                            <el-form :model="pwdForm" label-width="80px" size="small">
                                <el-form-item label="当前密码"><el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前密码"></el-input></el-form-item>
                                <el-form-item label="新密码"><el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码"></el-input></el-form-item>
                                <el-form-item label="确认密码"><el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码"></el-input></el-form-item>
                                <el-form-item><el-button type="primary" @click="updatePassword">修改密码</el-button></el-form-item>
                            </el-form>
                        </el-card>
                    </el-col>
                </el-row>
'@ -split "`r?`n"
Replace-Range 709 820 $personalBlock

$mainBlock = @'
                <!-- ===== 部门信息 ===== -->
                <div class="page-section" :class="{active: currentMenu === 'department'}">
                    <div class="search-bar">
                        <el-button type="primary" size="small" @click="openDeptForm()">新增部门</el-button>
                    </div>
                    <div class="table-card">
                        <el-table :data="departments" stripe border size="small" style="width:100%" v-loading="deptLoading">
                            <el-table-column type="index" label="序号" width="80"></el-table-column>
                            <el-table-column prop="id" label="部门ID" width="100"></el-table-column>
                            <el-table-column prop="deptName" label="部门名称" width="180"></el-table-column>
                            <el-table-column prop="deptCode" label="部门编码" width="140"></el-table-column>
                            <el-table-column prop="baseSalary" label="基本工资" width="120"></el-table-column>
                            <el-table-column prop="description" label="部门描述"></el-table-column>
                            <el-table-column label="操作" width="160" fixed="right">
                                <template #default="{row}">
                                    <el-button link type="warning" size="small" @click="openDeptForm(row)">修改</el-button>
                                    <el-button link type="danger" size="small" @click="deleteDept(row.id)">删除</el-button>
                                </template>
                            </el-table-column>
                        </el-table>
                    </div>
                </div>

                <!-- ===== 部门经理 ===== -->
                <div class="page-section" :class="{active: currentMenu === 'dept-manager'}">
                    <div class="search-bar">
                        <el-input v-model="managerSearch.managerNo" placeholder="经理账号" style="width:180px;" clearable></el-input>
                        <el-input v-model="managerSearch.managerName" placeholder="经理姓名" style="width:180px;" clearable></el-input>
                        <el-select v-model="managerSearch.deptId" placeholder="部门" style="width:180px;" clearable>
                            <el-option v-for="d in departments" :key="d.id" :label="d.deptName" :value="d.id"></el-option>
                        </el-select>
                        <el-button type="primary" @click="loadDeptManagers()">查询</el-button>
                    </div>
                    <div class="action-bar">
                        <el-button type="primary" size="small" @click="openDeptManagerForm()">新增</el-button>
                        <el-button type="success" size="small" @click="triggerImport">导入</el-button>
                        <el-button type="warning" size="small" @click="exportDeptManagerCsv">导出</el-button>
                    </div>
                    <div class="table-card">
                        <el-table :data="managerDisplayRows" stripe border size="small" style="width:100%" v-loading="deptManagerLoading">
                            <el-table-column type="index" label="序号" width="70"></el-table-column>
                            <el-table-column label="经理账号" min-width="140"><template #default="{row}">{{ row.managerNo || row.empNo || '-' }}</template></el-table-column>
                            <el-table-column prop="realName" label="经理姓名" min-width="120"></el-table-column>
                            <el-table-column label="性别" width="80"><template #default="{row}">{{ row.gender === 1 ? '男' : '女' }}</template></el-table-column>
                            <el-table-column prop="phone" label="手机" min-width="120"></el-table-column>
                            <el-table-column prop="deptName" label="部门" min-width="120"></el-table-column>
                            <el-table-column label="头像" width="90"><template #default="{row}"><el-image v-if="row.avatar" :src="row.avatar" style="width:34px;height:34px;border-radius:50%" fit="cover"></el-image><span v-else>-</span></template></el-table-column>
                            <el-table-column label="操作" min-width="160" fixed="right"><template #default="{row}"><el-button link type="warning" size="small" @click="openDeptManagerForm(row)">修改</el-button><el-button link type="danger" size="small" @click="deleteDeptManager(row)">删除</el-button></template></el-table-column>
                        </el-table>
                    </div>
                </div>

                <!-- ===== 员工管理 ===== -->
                <div class="page-section" :class="{active: currentMenu === 'employee'}">
                    <div class="search-bar">
                        <el-form :inline="true">
                            <el-form-item label="工号"><el-input v-model="empSearch.empNo" placeholder="工号" clearable></el-input></el-form-item>
                            <el-form-item label="姓名"><el-input v-model="empSearch.realName" placeholder="姓名" clearable></el-input></el-form-item>
                            <el-form-item label="部门"><el-select v-model="empSearch.deptId" placeholder="选择部门" clearable><el-option v-for="d in departments" :key="d.id" :label="d.deptName" :value="d.id"></el-option></el-select></el-form-item>
                            <el-form-item><el-button type="primary" @click="loadEmployees(1)">查询</el-button><el-button @click="resetEmpSearch">重置</el-button></el-form-item>
                        </el-form>
                    </div>
                    <div class="action-bar">
                        <el-button type="primary" size="small" @click="openEmpForm()">添加</el-button>
                        <el-button type="danger" size="small" @click="batchDeleteEmp">删除</el-button>
                        <el-button type="success" size="small" @click="triggerImport">导入</el-button>
                        <el-button type="warning" size="small" @click="exportExcel">导出</el-button>
                    </div>
                    <div class="table-card">
                        <el-table :data="employeeDisplayRows" stripe border size="small" style="width:100%" v-loading="empLoading" @selection-change="handleEmpSelection">
                            <el-table-column type="selection" width="46"></el-table-column>
                            <el-table-column type="index" label="序号" width="60"></el-table-column>
                            <el-table-column prop="empNo" label="工号" width="100"></el-table-column>
                            <el-table-column prop="realName" label="姓名" width="100"></el-table-column>
                            <el-table-column label="性别" width="80"><template #default="{row}">{{ row.gender === 1 ? '男' : '女' }}</template></el-table-column>
                            <el-table-column prop="phone" label="手机" width="120"></el-table-column>
                            <el-table-column prop="deptName" label="部门" width="120"></el-table-column>
                            <el-table-column prop="baseSalary" label="基本工资" width="120"></el-table-column>
                            <el-table-column label="头像" width="96"><template #default="{row}"><el-image v-if="row.avatar" :src="row.avatar" style="width:40px;height:40px;border-radius:4px" fit="cover"></el-image><span v-else>-</span></template></el-table-column>
                            <el-table-column prop="hireDate" label="入职时间" width="120"></el-table-column>
                            <el-table-column prop="status" label="状态" width="90"><template #default="{row}">{{ row.status === 1 ? '在职' : '离职' }}</template></el-table-column>
                            <el-table-column label="操作" fixed="right" min-width="140"><template #default="{row}"><el-button link type="warning" size="small" @click="openEmpForm(row)">修改</el-button><el-button link type="danger" size="small" @click="deleteEmp(row.id)">删除</el-button></template></el-table-column>
                        </el-table>
                        <div style="margin-top:12px;display:flex;justify-content:flex-end;">
                            <el-pagination :total="empTotal" :page-size="10" :current-page="empCurrent" layout="total, sizes, prev, pager, next, jumper" @current-change="loadEmployees"></el-pagination>
                        </div>
                    </div>
                </div>

                <!-- ===== 其余页面先恢复为可用占位，避免白屏 ===== -->
                <div class="page-section" :class="{active: ['notice','attendance','attendance-apply','anomaly','complaint','salary-feedback','tax','performance','salary','salary-payment'].includes(currentMenu)}">
                    <div class="table-card" style="padding:32px;">
                        <div style="font-size:18px;font-weight:700;color:#1a6ab1;margin-bottom:12px;">{{ currentMenuLabel }}</div>
                        <div style="font-size:14px;color:#666;line-height:1.8;">
                            当前页面结构已恢复为可正常渲染状态，接下来会继续逐页把字段和中文文案整理完整。
                        </div>
                    </div>
                </div>

                <!-- ===== 弹窗区 ===== -->
                <el-dialog :title="deptForm.id ? '修改部门' : '新增部门'" v-model="deptDialogVisible" width="460px">
                    <el-form :model="deptForm" label-width="90px">
                        <el-form-item label="部门名称" required><el-input v-model="deptForm.deptName" placeholder="例如：技术部"></el-input></el-form-item>
                        <el-form-item label="部门编码"><el-input v-model="deptForm.deptCode" placeholder="例如：TECH，不填则自动生成"></el-input></el-form-item>
                        <el-form-item label="基本工资" required><el-input-number v-model="deptForm.baseSalary" :min="0" :precision="2" style="width:100%;"></el-input-number></el-form-item>
                        <el-form-item label="描述"><el-input type="textarea" v-model="deptForm.description" placeholder="部门职责说明"></el-input></el-form-item>
                    </el-form>
                    <template #footer><el-button @click="deptDialogVisible = false">取消</el-button><el-button type="primary" @click="saveDept">保存</el-button></template>
                </el-dialog>

                <el-dialog :title="empDialogTitle" v-model="empDialogVisible" width="600px">
                    <el-form :model="empForm" label-width="100px" style="max-height:50vh;overflow-y:auto;padding-right:15px;">
                        <el-form-item label="工号(账号)"><el-input v-model="empForm.empNo" :disabled="!!empForm.id" placeholder="唯一工号，将作为登录账号"></el-input></el-form-item>
                        <el-form-item label="初始密码" v-if="!empForm.id"><el-input v-model="empForm.initialPassword" placeholder="不填则默认为手机号后6位或123456"></el-input></el-form-item>
                        <el-form-item label="姓名"><el-input v-model="empForm.realName"></el-input></el-form-item>
                        <el-form-item label="性别"><el-radio-group v-model="empForm.gender"><el-radio :label="1">男</el-radio><el-radio :label="2">女</el-radio></el-radio-group></el-form-item>
                        <el-form-item label="手机号"><el-input v-model="empForm.phone"></el-input></el-form-item>
                        <el-form-item label="身份证号"><el-input v-model="empForm.idCard"></el-input></el-form-item>
                        <el-form-item label="所属部门"><el-select v-model="empForm.deptId" style="width:100%" placeholder="选择部门" @change="syncEmpDeptSalary"><el-option v-for="d in departments" :key="d.id" :label="d.deptName" :value="d.id"></el-option></el-select></el-form-item>
                        <el-form-item label="头像">
                            <div class="avatar-uploader-box">
                                <div class="avatar-preview"><img v-if="empForm.avatar" :src="empForm.avatar" alt="头像预览"><span v-else>暂无头像</span></div>
                                <div class="avatar-upload-actions">
                                    <el-button type="primary" plain :loading="avatarUploading" @click="triggerAvatarUpload">上传头像</el-button>
                                    <el-button v-if="empForm.avatar" link type="danger" @click="empForm.avatar = ''">移除头像</el-button>
                                    <div class="avatar-tip">支持 jpg、png、jpeg、webp，上传后会自动回填到当前员工资料。</div>
                                </div>
                            </div>
                        </el-form-item>
                        <el-form-item label="基本工资"><el-input :model-value="empBaseSalaryText" disabled></el-input></el-form-item>
                        <el-form-item label="入职时间"><el-date-picker v-model="empForm.hireDate" type="date" format="YYYY-MM-DD" value-format="YYYY-MM-DD" style="width:200px" teleported></el-date-picker></el-form-item>
                    </el-form>
                    <template #footer><el-button @click="empDialogVisible = false">取消</el-button><el-button type="primary" @click="saveEmployee">{{ empDialogSubmitText }}</el-button></template>
                </el-dialog>

                <input type="file" ref="importInput" style="display:none;" accept=".xlsx,.xls" @change="handleImportFile">
                <input type="file" ref="attendImportInput" style="display:none;" accept=".xlsx,.xls" @change="handleAttendImport">
                <input type="file" ref="avatarInput" style="display:none;" accept="image/png,image/jpeg,image/jpg,image/webp" @change="handleAvatarFile">

                <el-dialog :title="noticeForm.id ? '修改公告' : '发布新公告'" v-model="noticeDialogVisible" width="500px">
                    <el-form :model="noticeForm" label-width="80px">
                        <el-form-item label="标题"><el-input v-model="noticeForm.title" placeholder="请输入公告标题"></el-input></el-form-item>
                        <el-form-item label="正文"><el-input type="textarea" :rows="5" v-model="noticeForm.content" placeholder="请输入公告内容..."></el-input></el-form-item>
                    </el-form>
                    <template #footer><el-button @click="noticeDialogVisible = false">取消</el-button><el-button type="primary" @click="saveNotice">发布</el-button></template>
                </el-dialog>

                <el-dialog :title="attendForm.id ? '修改考勤数据' : '添加考勤数据'" v-model="attendDialogVisible" width="600px">
                    <el-form :model="attendForm" label-width="110px" style="max-height:50vh;overflow-y:auto;padding-right:15px;">
                        <el-form-item label="月份"><el-date-picker v-model="attendForm.yearMonth" type="month" format="YYYY-MM" value-format="YYYY-MM" style="width:100%" :disabled="!!attendForm.id" teleported popper-class="numeric-month-popper"></el-date-picker></el-form-item>
                        <el-form-item label="员工"><el-select v-model="attendForm.empId" style="width:100%" filterable :disabled="!!attendForm.id"><el-option v-for="e in allEmployees" :key="e.id" :label="e.empNo + ' - ' + e.realName" :value="e.id"></el-option></el-select></el-form-item>
                        <el-form-item label="出勤天数"><el-input-number v-model="attendForm.attendDays" :min="0" :max="31"></el-input-number></el-form-item>
                        <el-form-item label="旷工天数"><el-input-number v-model="attendForm.absentDays" :min="0"></el-input-number></el-form-item>
                        <el-form-item label="请假天数"><el-input-number v-model="attendForm.leaveDays" :min="0"></el-input-number></el-form-item>
                        <el-form-item label="迟到天数"><el-input-number v-model="attendForm.lateTimes" :min="0"></el-input-number></el-form-item>
                        <el-form-item label="备注"><el-input type="textarea" v-model="attendForm.remark"></el-input></el-form-item>
                    </el-form>
                    <template #footer><el-button @click="attendDialogVisible = false">取消</el-button><el-button type="primary" @click="saveAttend">保存记录</el-button></template>
                </el-dialog>
            </div>
'@ -split "`r?`n"
Replace-Range 821 1660 $mainBlock

# Fix the most critical broken JS strings so Vue can mount again
Replace-Line 1538 '        <el-dialog :title="noticeForm.id ? ''修改公告'' : ''发布新公告''" v-model="noticeDialogVisible" width="500px">'
Replace-Line 1790 '                    { key: ''tax'', label: ''工资个税与社保'', icon: ''Tickets'' },'
Replace-Line 1857 '                        xAxis: { type: ''category'', data: [''销售部'', ''技术部'', ''财务部'', ''行政部'', ''产品部''] },'
Replace-Line 1884 '                        xAxis: { type: ''category'', boundaryGap: false, data: [''1月'', ''2月'', ''3月'', ''4月'', ''5月'', ''6月'', ''7月'', ''8月'', ''9月''] },'
Replace-Line 1901 '                        title: { text: ''当月整体考勤状态'', top: 15, left: ''center'', textStyle: { fontSize: 16, color: ''#333'' } },'
Replace-Line 1917 '                        yAxis: { type: ''category'', data: [''销售部'', ''技术部'', ''财务部'', ''行政部'', ''产品部''] },'
Replace-Line 1929 '                    if (value === '''' ) callback(new Error(''请再次输入密码''));'
Replace-Line 1930 '                    else if (value !== pwdForm.newPassword) callback(new Error(''两次输入密码不一致''));'
Replace-Line 1934 '                    oldPassword: [{ required: true, message: ''请输入当前密码'', trigger: ''blur'' }],'
Replace-Line 1937 '                        { min: 6, message: ''密码长度至少6位'', trigger: ''blur'' }'
Replace-Line 1994 '                            if (r.data.code === 200) { ElementPlus.ElMessage.success(''删除成功''); loadDepartments(); }'
Replace-Line 2078 '                    ElementPlus.ElMessageBox.confirm(''确认删除该部门经理吗？'', ''提示'', { type: ''warning'' }).then(() => {'
Replace-Line 2126 '                        axios.delete(''/api/announcement/'' + id).then(r => { if (r.data.code === 200) { ElementPlus.ElMessage.success(''删除成功''); loadNotices(); loadDashboard(); } });'
Replace-Line 2191 '                    if (!attendForm.empId || !attendForm.yearMonth) return ElementPlus.ElMessage.warning(''月份和员工必填'');'
Replace-Line 2206 '                            if (r.data.code === 200) { ElementPlus.ElMessage.success(''删除成功''); loadAttendances(); }'
Replace-Line 2291 '                申请类型：${['''', ''补签'', ''销假'', ''加班'', ''异常异议''][row.applyType] || ''-''}<br>'
Replace-Line 2300 '                    if (!row.id) return ElementPlus.ElMessage.warning(''当前记录缺少主键，无法删除'');'
Replace-Line 2337 '                    if (!anomalyForm.processResult) return ElementPlus.ElMessage.warning(''请填写处理结果'');'
Replace-Line 2365 '                    if (!feedbackForm.replyContent) return ElementPlus.ElMessage.warning(''请填写回复说明'');'
Replace-Line 2446 '            `, ''工资个税与社保详情'', { dangerouslyUseHTMLString: true });'
Replace-Line 2498 '                    return ''待提升'';'
Replace-Line 2516 '                    if (!perfForm.empId || !perfForm.yearMonth) return ElementPlus.ElMessage.warning(''员工与月份必填'');'
Replace-Line 2548 '                    if (!row.id) return ElementPlus.ElMessage.warning(''当前记录缺少主键，无法删除'');'
Replace-Line 2558 '                            ElementPlus.ElMessage.warning(''当前后台暂未开放绩效评分删除接口'');'
Replace-Line 2616 '                    if (ids.length === 0) return ElementPlus.ElMessage.warning(''当前账单中没有可发布的草稿记录'');'
Replace-Line 2618 '                        if (r.data.code === 200) { ElementPlus.ElMessage.success(''批量公开完成，员工已能收到账单''); loadSalaries(); }'
Replace-Line 2632 '                发薪账户: ${row.bankAccount || ''未维护''}`,'
Replace-Line 2638 '                    ElementPlus.ElMessage.warning(''当前后台暂未开放薪资核算单条删除接口'');'
Replace-Line 2671 '                    if (row.calcStatus < 2) return ElementPlus.ElMessage.warning(''该账单未公开定表，不可打款'');'
Replace-Line 2672 '                    ElementPlus.ElMessageBox.confirm(`准备向账户 ${row.bankAccount || ''【未绑定对公账户】''} 汇款 ￥${row.netSalary} 元，是否继续执行？`, ''安全支付网关接入中...'').then(() => {'
Replace-Line 2761 '                                ElementPlus.ElMessage.error(''当前浏览器无法处理这张图片'');'
Replace-Line 2767 '                            ElementPlus.ElMessage.warning(''服务器上传暂不可用，已临时本地保存头像'');'
Replace-Line 2774 '                            ElementPlus.ElMessage.error(''头像读取失败，请换一张图片再试'');'
Replace-Line 2781 '                        ElementPlus.ElMessage.error(''头像读取失败，请换一张图片再试'');'

# Month map
Replace-Line 1386 '                jan: ''1月'', feb: ''2月'', mar: ''3月'', apr: ''4月'','
Replace-Line 1387 '                may: ''5月'', jun: ''6月'', jul: ''7月'', aug: ''8月'','
Replace-Line 1388 '                sep: ''9月'', oct: ''10月'', nov: ''11月'', dec: ''12月'''

[System.IO.File]::WriteAllLines($path, $lines, [System.Text.UTF8Encoding]::new($false))
Write-Output 'done'
