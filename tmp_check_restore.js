
        const { createApp, ref, reactive, onMounted, computed } = Vue;

        // Element Plus 本地化：强制月份显示为数字（1月-12月）
        const __elZhLocale = window.ElementPlusLocaleZhCn || window.ElementPlusLocaleZhCN;
        if (__elZhLocale && __elZhLocale.el) {
            const monthMap = {
                jan: '1月', feb: '2月', mar: '3月', apr: '4月',
                may: '5月', jun: '6月', jul: '7月', aug: '8月',
                sep: '9月', oct: '10月', nov: '11月', dec: '12月'
            };
            if (__elZhLocale.el.datepicker) __elZhLocale.el.datepicker.months = monthMap;
            if (__elZhLocale.el.datePicker) __elZhLocale.el.datePicker.months = monthMap;
        }

        // Axios 拦截器添加 token
        axios.interceptors.request.use(cfg => {
            const token = localStorage.getItem('token');
            if (token) cfg.headers['Authorization'] = token;
            return cfg;
        });
        axios.interceptors.response.use(res => res, err => {
            if (err.response?.status === 401) {
                localStorage.clear();
                window.location.href = '../login.html';
            }
            return Promise.reject(err);
        });

        const app = createApp({
            setup() {
                const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'));
                const currentMenu = ref('home');

                // --- 个人中心数据 ---
                const selfInfo = reactive({ empNo: '', realName: '', gender: '', phone: '', deptName: '', baseSalary: '', bankCard: '', positionName: '', hireDate: '', managerNo: '', managerName: '', avatar: '', id: '' });
                const selfEditForm = reactive({ phone: '', bankCard: '' });
                const loadSelfInfo = () => {
                    const username = userInfo.value.username;
                    if (!username) return;
                    // 先尝试通过 /api/auth/info 拿当前登录员工信息
                    axios.get('/api/auth/info').then(r => {
                        if (r.data.code === 200 && r.data.data) {
                            Object.assign(selfInfo, r.data.data);
                            selfEditForm.phone = selfInfo.phone || '';
                            selfEditForm.bankCard = selfInfo.bankCard || '';
                            return;
                        }
                        // fallback: 通过员工分页搜索按账号
                        axios.get('/api/employee/page', { params: { username, size: 5 } }).then(r2 => {
                            const emp = r2.data.data?.records?.[0] || r2.data.data?.[0];
                            if (emp) {
                                Object.assign(selfInfo, emp);
                                selfEditForm.phone = selfInfo.phone || '';
                                selfEditForm.bankCard = selfInfo.bankCard || '';
                            }
                        }).catch(() => { });
                    }).catch(() => {
                        // fallback
                        axios.get('/api/employee/page', { params: { username, size: 5 } }).then(r2 => {
                            const emp = r2.data.data?.records?.[0] || r2.data.data?.[0];
                            if (emp) {
                                Object.assign(selfInfo, emp);
                                selfEditForm.phone = selfInfo.phone || '';
                                selfEditForm.bankCard = selfInfo.bankCard || '';
                            }
                        }).catch(() => { });
                    });
                };
                const saveSelfInfo = () => {
                    if (!selfInfo.id) return ElementPlus.ElMessage.warning('尚未关联到员工信息，无法修改');
                    axios.put('/api/employee', { id: selfInfo.id, phone: selfEditForm.phone, bankCard: selfEditForm.bankCard }).then(r => {
                        if (r.data.code === 200) {
                            ElementPlus.ElMessage.success('信息保存成功');
                            selfInfo.phone = selfEditForm.phone;
                            selfInfo.bankCard = selfEditForm.bankCard;
                        } else ElementPlus.ElMessage.error(r.data.message || '保存失败');
                    });
                };

                const menus = ref([
                    { key: 'home', label: '系统首页', icon: 'House' },
                    { key: 'personal', label: '个人中心', icon: 'User' },
                    { key: 'employee', label: '员工', icon: 'UserFilled' },
                    { key: 'dept-manager', label: '部门经理', icon: 'Avatar' },
                    { key: 'department', label: '部门信息', icon: 'OfficeBuilding' },
                    { key: 'attendance-apply', label: '考勤申诉', icon: 'Document' },
                    { key: 'attendance', label: '考勤数据', icon: 'Calendar' },
                    { key: 'salary', label: '薪资核算', icon: 'Money' },
                    { key: 'salary-feedback', label: '薪资反馈', icon: 'ChatDotRound' },
                    { key: 'salary-payment', label: '薪资发放', icon: 'Wallet' },
                    { key: 'tax', label: '工资个税与社保', icon: 'Tickets' },
                    { key: 'performance', label: '绩效评分', icon: 'Histogram' },
                    { key: 'complaint', label: '投诉反馈', icon: 'ChatLineRound' },
                    { key: 'anomaly', label: '异常上报', icon: 'Warning' },
                    { key: 'notice', label: '通知公告', icon: 'Bell' }
                ]);
                const currentMenuLabel = computed(() => menus.value.find(m => m.key === currentMenu.value)?.label || '系统首页');
                if (userInfo.value.role === 2) {
                    menus.value = menus.value.filter(m => ['home', 'personal', 'employee', 'attendance-apply', 'attendance', 'performance', 'salary', 'salary-feedback', 'anomaly', 'notice'].includes(m.key));
                }

                const switchMenu = (menu) => {
                    currentMenu.value = menu.key;
                    if (menu.key === 'personal') loadSelfInfo();
                    if (menu.key === 'employee' || menu.key === 'department' || menu.key === 'dept-manager') loadDepartments();
                    if (menu.key === 'employee') loadEmployees(1);
                    if (menu.key === 'dept-manager') loadDeptManagers();
                    if (menu.key === 'notice') loadNotices(1);
                    if (menu.key === 'attendance') {
                        loadDepartments();
                        loadAttendances(1);
                        loadAllEmployeesForSelect();
                    }
                    if (menu.key === 'attendance-apply') {
                        loadDepartments();
                        loadAllEmployeesForSelect();
                        loadApplies(1);
                    }
                    if (menu.key === 'anomaly') loadAnomalies(1);
                    if (menu.key === 'complaint' || menu.key === 'salary-feedback') loadFeedbacks(1);
                    { key: 'tax', label: '工资个税与社保', icon: 'Tickets' },
                        loadDepartments();
                        loadTaxRecords(1);
                    }
                    if (menu.key === 'salary') {
                        loadDepartments();
                        loadSalaries(1);
                    }
                    if (menu.key === 'salary-payment') {
                        loadDepartments();
                        loadPayments(1);
                    }
                    if (menu.key === 'performance') {
                        loadDepartments();
                        loadAllEmployeesForSelect();
                        loadPerformances(1);
                    }
                };

                // --- 首页数据 ---
                const stats = reactive({ employeeCount: 0, salaryCount: 0, paymentCount: 0 });
                const notices = ref([]);
                const loadDashboard = () => {
                    axios.get('/api/employee/count').then(r => { if (r.data.code === 200) stats.employeeCount = r.data.data; }).catch(() => { });
                    axios.get('/api/salary/count').then(r => { if (r.data.code === 200) stats.salaryCount = r.data.data; }).catch(() => { });
                    axios.get('/api/salary/payment/count').then(r => { if (r.data.code === 200) stats.paymentCount = r.data.data; }).catch(() => { });
                    setTimeout(initCharts, 200);
                };
                const initCharts = () => {
                    const getEl = id => document.getElementById(id);
                    if (!getEl('echart-top1')) return;

                    // 1. 部门人数分布 (柱状图)
                    const c1 = echarts.init(getEl('echart-top1'));
                    c1.setOption({
                        title: { text: '部门人数分布', top: 15, left: 'center', textStyle: { fontSize: 16, color: '#333' } },
                        tooltip: { trigger: 'axis' },
                        xAxis: { type: 'category', data: ['销售部', '技术部', '财务部', '行政部', '产品部'] },
                        yAxis: { type: 'value' },
                        series: [{ type: 'bar', data: [15, 23, 8, 5, 12], itemStyle: { color: '#0956FF', borderRadius: [4, 4, 0, 0] } }],
                        grid: { left: 40, right: 30, top: 60, bottom: 30 }
                    });

                    // 2. 薪资结构分布 (饼图)
                    const c2 = echarts.init(getEl('echart-top2'));
                    c2.setOption({
                        title: { text: '当月薪资结构占比', top: 15, left: 'center', textStyle: { fontSize: 16, color: '#333' } },
                        tooltip: { trigger: 'item' },
                        legend: { bottom: 10, left: 'center' },
                        series: [{
                            name: '薪资类型', type: 'pie', radius: ['40%', '70%'], center: ['50%', '50%'],
                            data: [
                                { value: 124500, name: '基本工资' }, { value: 34000, name: '绩效奖金' },
                                { value: 12000, name: '加班补贴' }, { value: 8500, name: '其他补助' }
                            ],
                            itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 }
                        }]
                    });

                    // 3. 全年薪资核发走势 (折线面积图)
                    const c3 = echarts.init(getEl('echart-mid1'));
                    c3.setOption({
                        title: { text: '月实发工资走势(元)', top: 15, left: 'center', textStyle: { fontSize: 16, color: '#333' } },
                        tooltip: { trigger: 'axis' },
                        xAxis: { type: 'category', boundaryGap: false, data: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月'] },
                        yAxis: { type: 'value' },
                        series: [{
                        xAxis: { type: 'category', data: ['销售部', '技术部', '财务部', '行政部', '产品部'] },
                            smooth: true, itemStyle: { color: '#00D1B2' },
                            areaStyle: {
                                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                                    { offset: 0, color: 'rgba(0, 209, 178, 0.4)' }, { offset: 1, color: 'rgba(0, 209, 178, 0.05)' }
                                ])
                            }
                        }],
                        grid: { left: 60, right: 40, top: 60, bottom: 30 }
                    });

                    // 4. 员工考勤状态分析 (环形图)
                    const c4 = echarts.init(getEl('echart-bot1'));
                    c4.setOption({
                        title: { text: '当月整体考勤状态', top: 15, left: 'center', textStyle: { fontSize: 16, color: '#333' } },
                        tooltip: { trigger: 'item' },
                        series: [{
                            type: 'pie', radius: '65%', center: ['50%', '55%'],
                            data: [{ value: 92, name: '全勤正常' }, { value: 3, name: '迟到/早退' }, { value: 2, name: '矷工' }, { value: 5, name: '请假' }],
                            itemStyle: { borderRadius: 6 }
                        }],
                        color: ['#0956FF', '#E6A23C', '#F56C6C', '#909399']
                    });

                    // 5. 各部门人均薪资对比 (雷达图/横向柱图)
                    const c5 = echarts.init(getEl('echart-bot2'));
                    c5.setOption({
                        xAxis: { type: 'category', boundaryGap: false, data: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月'] },
                        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
                        xAxis: { type: 'value' },
                        yAxis: { type: 'category', data: ['销售部', '技术部', '财务部', '行政部', '产品部'] },
                        series: [{ type: 'bar', data: [9800, 14200, 8500, 7200, 12500], itemStyle: { color: '#8884d8' }, label: { show: true, position: 'right' } }],
                        grid: { left: 80, right: 40, top: 60, bottom: 30 }
                    });

                    window.addEventListener('resize', () => { c1.resize(); c2.resize(); c3.resize(); c4.resize(); c5.resize(); });
                };

                // --- 个人中心 ---
                const pwdFormRef = ref(null);
                const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' });
                const validatePass2 = (rule, value, callback) => {
                    if (value === '') callback(new Error('请再次输入密码'));
                    else if (value !== pwdForm.newPassword) callback(new Error('两次输入密码不一致'));
                        title: { text: '当月整体考勤状态', top: 15, left: 'center', textStyle: { fontSize: 16, color: '#333' } },
                };
                const pwdRules = reactive({
                    oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
                    newPassword: [
                        { required: true, message: '请输入新密码', trigger: 'blur' },
                        { min: 6, message: '密码长度至少6位', trigger: 'blur' }
                    ],
                    confirmPassword: [
                        { required: true, message: '请确认新密码', trigger: 'blur' },
                        { validator: validatePass2, trigger: 'blur' }
                    ]
                });
                const updatePassword = () => {
                    if (!pwdFormRef.value) return;
                    pwdFormRef.value.validate((valid) => {
                        yAxis: { type: 'category', data: ['销售部', '技术部', '财务部', '行政部', '产品部'] },
                            axios.put('/api/auth/password', null, { params: { oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword } }).then(r => {
                                if (r.data.code === 200) {
                                    ElementPlus.ElMessage.success('密码修改成功，请重新登录');
                                    setTimeout(logout, 1500);
                                } else {
                                    ElementPlus.ElMessage.error(r.data.message || '修改失败');
                                }
                            });
                        }
                    });
                };
                    if (value === '' ) callback(new Error('请再次输入密码'));
                    else if (value !== pwdForm.newPassword) callback(new Error('两次输入密码不一致'));
                const departments = ref([]);
                const deptLoading = ref(false);
                const deptDialogVisible = ref(false);
                    oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],

                const loadDepartments = () => {
                        { min: 6, message: '密码长度至少6位', trigger: 'blur' }
                    axios.get('/api/department/list').then(r => {
                        if (r.data.code === 200) departments.value = r.data.data || [];
                    }).finally(() => { deptLoading.value = false; });
                };
                const openDeptForm = (row) => {
                    deptForm.id = row?.id || '';
                    deptForm.deptName = row?.deptName || '';
                    deptForm.deptCode = row?.deptCode || '';
                    deptForm.baseSalary = row?.baseSalary ?? null;
                    deptForm.description = row?.description || '';
                    deptDialogVisible.value = true;
                };
                const saveDept = () => {
                    if (!deptForm.deptName) return ElementPlus.ElMessage.warning('部门名称必填');
                    const req = deptForm.id ? axios.put('/api/department', deptForm) : axios.post('/api/department', deptForm);
                    req.then(r => {
                        if (r.data.code === 200) {
                            ElementPlus.ElMessage.success('保存成功');
                            deptDialogVisible.value = false;
                            loadDepartments();
                        } else ElementPlus.ElMessage.error(r.data.message || '失败');
                    });
                };
                const deleteDept = (id) => {
                    ElementPlus.ElMessageBox.confirm('确认解散该部门？', '警告', { type: 'warning' }).then(() => {
                        axios.delete(`/api/department/${id}`).then(r => {
                            if (r.data.code === 200) { ElementPlus.ElMessage.success('已删除'); loadDepartments(); }
                            else ElementPlus.ElMessage.error(r.data.message || '失败');
                        });
                    }).catch(() => {});
                };

                // --- 通知公告管理 ---
                const managerSearch = reactive({ managerNo: '', managerName: '', deptId: '' });
                const deptManagers = ref([]);
                const deptManagerLoading = ref(false);
                const managerEmpNos = ref(JSON.parse(localStorage.getItem('deptManagerEmpNos') || '[]'));
                const employeeAvatarStore = ref(JSON.parse(localStorage.getItem('employeeAvatarStore') || '{}'));
                const persistEmployeeAvatarStore = () => {
                    localStorage.setItem('employeeAvatarStore', JSON.stringify(employeeAvatarStore.value));
                };
                const getStoredAvatar = (empNo) => {
                    if (empNo === null || empNo === undefined || empNo === '') return '';
                    return employeeAvatarStore.value[String(empNo)] || '';
                };
                const rememberEmployeeAvatar = (empNo, avatar) => {
                    if (!empNo) return;
                    if (avatar) employeeAvatarStore.value[String(empNo)] = avatar;
                    else delete employeeAvatarStore.value[String(empNo)];
                    persistEmployeeAvatarStore();
                };
                const hasManagerEmpNo = (empNo) => {
                    if (empNo === null || empNo === undefined || empNo === '') return false;
                    return managerEmpNos.value.map(item => String(item)).includes(String(empNo));
                };
                const persistManagerEmpNos = () => {
                            if (r.data.code === 200) { ElementPlus.ElMessage.success('删除成功'); loadDepartments(); }
                };
                const rememberManagerEmpNo = (empNo) => {
                    if (!empNo) return;
                    if (!managerEmpNos.value.includes(empNo)) {
                        managerEmpNos.value.push(empNo);
                        persistManagerEmpNos();
                    }
                };
                const forgetManagerEmpNo = (empNo) => {
                    managerEmpNos.value = managerEmpNos.value.filter(item => item !== empNo);
                    persistManagerEmpNos();
                };
                const isManagerRecord = (row) => {
                    const positionText = [row.positionName, row.position, row.postName, row.jobName].filter(Boolean).join('');
                    return positionText.includes('经理')
                        || Number(row.role) === 2
                        || String(row.userRole || '') === '2'
                        || (!!row.empNo && hasManagerEmpNo(row.empNo))
                        || (!!row.managerNo && row.managerNo === row.empNo);
                };
                const managerDisplayRows = computed(() => deptManagers.value
                    .filter(row => isManagerRecord(row))
                    .map(row => ({ ...row, avatar: row.avatar || getStoredAvatar(row.empNo) || '' })));
                const loadDeptManagers = () => {
                    deptManagerLoading.value = true;
                    axios.get('/api/employee/page', {
                        params: {
                            current: 1,
                            size: 300,
                            empNo: managerSearch.managerNo || '',
                            realName: managerSearch.managerName || '',
                            deptId: managerSearch.deptId || '',
                            status: 1
                        }
                    }).then(r => {
                        if (r.data.code === 200) {
                            const rows = r.data.data?.records || r.data.data || [];
                            deptManagers.value = rows.filter(row => isManagerRecord(row));
                            deptManagers.value.forEach(row => rememberManagerEmpNo(row.empNo));
                        }
                    }).finally(() => {
                        deptManagerLoading.value = false;
                    });
                };
                const openDeptManagerForm = (row) => {
                    openEmpForm(row, 'manager');
                    empForm.positionName = '部门经理';
                    empForm.position = '部门经理';
                    empForm.role = 2;
                    if (empForm.empNo) rememberManagerEmpNo(empForm.empNo);
                    syncEmpDeptSalary(empForm.deptId);
                };
                const deleteDeptManager = (row) => {
                    ElementPlus.ElMessageBox.confirm('确认删除该部门经理吗？', '提示', { type: 'warning' }).then(() => {
                        axios.delete('/api/employee/' + row.id).then(r => {
                            if (r.data.code === 200) {
                                forgetManagerEmpNo(row.empNo);
                                rememberEmployeeAvatar(row.empNo, '');
                                ElementPlus.ElMessage.success('删除成功');
                                loadDeptManagers();
                                loadEmployees(1);
                            } else {
                                ElementPlus.ElMessage.error(r.data.message || '删除失败');
                            }
                        });
                    }).catch(() => { });
                };

                const noticeList = ref([]);
                const noticeTotal = ref(0);
                const noticeCurrent = ref(1);
                const noticeLoading = ref(false);
                const noticeSearch = reactive({ title: '' });
                const noticeDialogVisible = ref(false);
                const noticeForm = reactive({ id: '', title: '', content: '' });

                const loadNotices = (page = noticeCurrent.value) => {
                    noticeCurrent.value = page;
                    noticeLoading.value = true;
                    axios.get('/api/announcement/list', { params: { page, size: 10, title: noticeSearch.title } }).then(r => {
                        if (r.data.code === 200) {
                            noticeList.value = r.data.data?.records || r.data.data || [];
                            noticeTotal.value = r.data.data?.total || 0;
                    ElementPlus.ElMessageBox.confirm('确认删除该部门经理吗？', '提示', { type: 'warning' }).then(() => {
                    }).finally(() => noticeLoading.value = false);
                };
                const openNoticeForm = (row) => {
                    noticeDialogVisible.value = true;
                    if (row) Object.assign(noticeForm, row);
                    else Object.assign(noticeForm, { id: '', title: '', content: '' });
                };
                const saveNotice = () => {
                    if (!noticeForm.title || !noticeForm.content) return ElementPlus.ElMessage.warning('内容不能为空');
                    const req = noticeForm.id ? axios.put('/api/announcement', noticeForm) : axios.post('/api/announcement', noticeForm);
                    req.then(r => {
                        if (r.data.code === 200) { ElementPlus.ElMessage.success('发布成功'); noticeDialogVisible.value = false; loadNotices(); loadDashboard(); }
                        else ElementPlus.ElMessage.error(r.data.message || '失败');
                    });
                };
                const deleteNotice = (id) => {
                    ElementPlus.ElMessageBox.confirm('确认撤回删除该公告？', '提示').then(() => {
                        axios.delete('/api/announcement/' + id).then(r => { if (r.data.code === 200) { ElementPlus.ElMessage.success('已删除'); loadNotices(); loadDashboard(); } });
                    }).catch(() => { });
                };

                // --- 考勤数据管理 ---
                const attendances = ref([]);
                const attendTotal = ref(0);
                const attendCurrent = ref(1);
                const attendLoading = ref(false);
                const attendSearch = reactive({ empNo: '', empName: '', deptId: '' });
                const attendImportMonth = ref(new Date().getFullYear() + '-' + String(new Date().getMonth() + 1).padStart(2, '0'));
                const attendDialogVisible = ref(false);
                const attendForm = reactive({ id: '', yearMonth: '', empId: '', attendDays: 0, absentDays: 0, lateTimes: 0, leaveDays: 0, businessTripTime: '', registerDate: '', remark: '' });
                const attendImportInput = ref(null);
                const allEmployees = ref([]);
                const attendanceDisplayRows = computed(() => attendances.value.map(row => {
                    const employee = allEmployees.value.find(item => item.id === row.empId) || {};
                    return {
                        ...row,
                        empName: row.empName || row.realName || employee.realName || '-',
                        deptName: row.deptName || employee.deptName || '-',
                        deptId: row.deptId || employee.deptId || '',
                        businessTripTime: row.businessTripTime || row.remark || '-',
                        registerDate: (row.registerDate || row.createTime || '').toString().slice(0, 10) || '-',
                        managerNo: row.managerNo || employee.managerNo || '-',
                        managerName: row.managerName || employee.managerName || '-'
                    };
                }).filter(row => {
                    const matchEmpNo = !attendSearch.empNo || String(row.empNo || '').includes(attendSearch.empNo);
                    const matchEmpName = !attendSearch.empName || String(row.empName || '').includes(attendSearch.empName);
                        axios.delete('/api/announcement/' + id).then(r => { if (r.data.code === 200) { ElementPlus.ElMessage.success('删除成功'); loadNotices(); loadDashboard(); } });
                    return matchEmpNo && matchEmpName && matchDept;
                }));

                const loadAllEmployeesForSelect = () => {
                    if (allEmployees.value.length > 0) return;
                    axios.get('/api/employee/page?size=1000').then(r => {
                        if (r.data.code === 200) allEmployees.value = r.data.data?.records || [];
                    });
                };
                const loadAttendances = (page = attendCurrent.value) => {
                    attendCurrent.value = page;
                    attendLoading.value = true;
                    axios.get('/api/attendance/page', { params: { current: page, size: 10, empNo: attendSearch.empNo, empName: attendSearch.empName, realName: attendSearch.empName, deptId: attendSearch.deptId } }).then(r => {
                        if (r.data.code === 200) {
                            attendances.value = r.data.data?.records || r.data.data || [];
                            attendTotal.value = r.data.data?.total || 0;
                        }
                    }).finally(() => attendLoading.value = false);
                };
                const openAttendForm = (row) => {
                    attendDialogVisible.value = true;
                    if (row) Object.assign(attendForm, {
                        ...row,
                        businessTripTime: row.businessTripTime || row.remark || '',
                        registerDate: (row.registerDate || row.createTime || '').toString().slice(0, 10)
                    });
                    else {
                        const now = new Date();
                        const ym = now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0');
                        const dateText = now.toISOString().slice(0, 10);
                        Object.assign(attendForm, { id: '', yearMonth: ym, empId: '', attendDays: 22, absentDays: 0, lateTimes: 0, leaveDays: 0, businessTripTime: '', registerDate: dateText, remark: '' });
                    }
                };
                const saveAttend = () => {
                    if (!attendForm.empId || !attendForm.yearMonth) return ElementPlus.ElMessage.warning('月份和员工必填');
                    const payload = {
                        ...attendForm,
                        remark: attendForm.businessTripTime || attendForm.remark || '',
                        registerDate: attendForm.registerDate || new Date().toISOString().slice(0, 10)
                    };
                    const req = attendForm.id ? axios.put('/api/attendance', payload) : axios.post('/api/attendance', payload);
                    req.then(r => {
                        if (r.data.code === 200) { ElementPlus.ElMessage.success('保存成功'); attendDialogVisible.value = false; loadAttendances(); }
                        else ElementPlus.ElMessage.error(r.data.message || '失败');
                    });
                };
                const deleteAttend = (id) => {
                    ElementPlus.ElMessageBox.confirm('确认删除该条考勤记录吗？', '提示', { type: 'warning' }).then(() => {
                        axios.delete('/api/attendance/' + id).then(r => {
                            if (r.data.code === 200) { ElementPlus.ElMessage.success('已删除'); loadAttendances(); }
                            else ElementPlus.ElMessage.error(r.data.message);
                        });
                    }).catch(() => { });
                };
                const triggerAttendImport = () => {
                    if (!attendImportMonth.value) return ElementPlus.ElMessage.warning('导入考勤前，请先选择导入月份');
                    attendImportInput.value.click();
                };
                const handleAttendImport = (e) => {
                    const file = e.target.files[0];
                    if (!file) return;
                    const fd = new FormData();
                    fd.append('file', file);
                    fd.append('yearMonth', attendImportMonth.value);
                    if (!attendForm.empId || !attendForm.yearMonth) return ElementPlus.ElMessage.warning('月份和员工必填');
                    axios.post('/api/attendance/import/clock', fd, { headers: { 'Content-Type': 'multipart/form-data' } }).then(r => {
                        if (r.data.code === 200) { ElementPlus.ElMessage.success('导入结算完成'); loadAttendances(1); }
                        else ElementPlus.ElMessage.error(r.data.message || '导入失败');
                    }).catch(err => ElementPlus.ElMessage.error(err.response?.data?.message || '网络错误'))
                        .finally(() => { loadInstance.close(); e.target.value = ''; });
                };
                const showAttendDetail = (row) => {
                    ElementPlus.ElMessageBox.alert(`
                登记编号：${row.recordNo || '-'}<br>
                月份：${row.yearMonth || '-'}<br>
                工号：${row.empNo || '-'}<br>
                姓名：${row.empName || '-'}<br>
                部门：${row.deptName || '-'}<br>
                出勤天数：${row.attendDays || 0}<br>
                            if (r.data.code === 200) { ElementPlus.ElMessage.success('删除成功'); loadAttendances(); }
                迟到天数：${row.lateTimes || 0}<br>
                请假天数：${row.leaveDays || 0}<br>
                出差时间：${row.businessTripTime || '-'}<br>
                登记日期：${row.registerDate || '-'}<br>
                经理账号：${row.managerNo || '-'}<br>
                经理姓名：${row.managerName || '-'}
            `, '考勤数据详情', { dangerouslyUseHTMLString: true });
                };

                // --- 考勤审批 ---
                const applies = ref([]);
                const applyTotal = ref(0);
                const applyCurrent = ref(1);
                const applyLoading = ref(false);
                const applySearch = reactive({ empName: '', deptId: '', applyType: '', status: '' });
                const applyDisplayRows = computed(() => applies.value.map(row => {
                    const employee = allEmployees.value.find(item => item.id === row.empId) || {};
                    return {
                        ...row,
                        empNo: row.empNo || employee.empNo || '-',
                        empName: row.empName || employee.realName || '-',
                        deptId: row.deptId || employee.deptId || '',
                        deptName: row.deptName || employee.deptName || '-',
                        avatar: row.avatar || employee.avatar || '',
                        managerNo: row.managerNo || employee.managerNo || '-',
                        managerName: row.managerName || employee.managerName || '-',
                        reviewComment: row.reviewComment || row.replyContent || '-'
                    };
                }).filter(row => {
                    const matchName = !applySearch.empName || String(row.empName || '').includes(applySearch.empName);
                    const matchDept = !applySearch.deptId || String(row.deptId || '') === String(applySearch.deptId);
                    const matchType = !applySearch.applyType || String(row.applyType || '') === String(applySearch.applyType);
                    const matchStatus = applySearch.status === '' || String(row.status) === String(applySearch.status);
                    return matchName && matchDept && matchType && matchStatus;
                }));
                const loadApplies = (page = applyCurrent.value) => {
                    applyCurrent.value = page;
                    applyLoading.value = true;
                    axios.get('/api/attendance-apply/page', { params: { current: page, size: 10, status: applySearch.status, applyType: applySearch.applyType, empName: applySearch.empName, realName: applySearch.empName, deptId: applySearch.deptId } }).then(r => {
                        if (r.data.code === 200) { applies.value = r.data.data?.records || []; applyTotal.value = r.data.data?.total || 0; }
                    }).finally(() => applyLoading.value = false);
                };
                const reviewApply = (row, status) => {
                    ElementPlus.ElMessageBox.prompt('请输入审批意见(选填)', status === 1 ? '审批同意' : '驳回申请').then(({ value }) => {
                        axios.put(`/api/attendance-apply/${row.id}/review?status=${status}&comment=${value || ''}`).then(r => {
                            if (r.data.code === 200) { ElementPlus.ElMessage.success('审批完成'); loadApplies(); }
                        });
                    }).catch(() => { });
                };
                const showApplyDetail = (row) => {
                    ElementPlus.ElMessageBox.alert(`
                工号：${row.empNo || '-'}<br>
                姓名：${row.empName || '-'}<br>
                部门：${row.deptName || '-'}<br>
                申请类型：${['', '补签', '销假', '加班', '异常异议'][row.applyType] || '-'}<br>
                申请时间：${row.createTime || '-'}<br>
                申请事由：${row.reason || '-'}<br>
                经理账号：${row.managerNo || '-'}<br>
                经理姓名：${row.managerName || '-'}<br>
                审核回复：${row.reviewComment || '-'}
            `, '考勤申请详情', { dangerouslyUseHTMLString: true });
                };
                const deleteApply = (row) => {
                    if (!row.id) return ElementPlus.ElMessage.warning('当前记录缺少主键，无法删除');
                    ElementPlus.ElMessageBox.confirm('确认删除该条考勤申请吗？', '提示', { type: 'warning' }).then(() => {
                        axios.delete('/api/attendance-apply/' + row.id).then(r => {
                            if (r.data.code === 200) {
                                ElementPlus.ElMessage.success('删除成功');
                                loadApplies(1);
                            } else {
                                ElementPlus.ElMessage.error(r.data.message || '删除失败');
                            }
                        }).catch(() => {
                            ElementPlus.ElMessage.warning('当前后台暂未开放考勤申请删除接口');
                        });
                    }).catch(() => { });
                };

                // --- 异常处理 ---
                const anomalies = ref([]);
                const anomalyTotal = ref(0);
                const anomalyCurrent = ref(1);
                const anomalyLoading = ref(false);
                const anomalySearch = reactive({ status: '' });
                申请类型：${['', '补签', '销假', '加班', '异常异议'][row.applyType] || '-'}<br>
                const anomalyForm = reactive({ id: '', status: 1, processResult: '' });
                const loadAnomalies = (page = anomalyCurrent.value) => {
                    anomalyCurrent.value = page;
                    anomalyLoading.value = true;
                    axios.get('/api/anomaly/page', { params: { current: page, size: 10, status: anomalySearch.status } }).then(r => {
                        if (r.data.code === 200) { anomalies.value = r.data.data?.records || []; anomalyTotal.value = r.data.data?.total || 0; }
                    }).finally(() => anomalyLoading.value = false);
                };
                    if (!row.id) return ElementPlus.ElMessage.warning('当前记录缺少主键，无法删除');
                    anomalyDialogVisible.value = true;
                    anomalyForm.id = row.id;
                    anomalyForm.status = row.status === 0 ? 1 : row.status;
                    anomalyForm.processResult = row.processResult || '';
                };
                const saveAnomaly = () => {
                    if (!anomalyForm.processResult) return ElementPlus.ElMessage.warning('请填写处理进展');
                    axios.put(`/api/anomaly/${anomalyForm.id}/process?status=${anomalyForm.status}&processResult=${anomalyForm.processResult}`).then(r => {
                        if (r.data.code === 200) { ElementPlus.ElMessage.success('记录完毕'); anomalyDialogVisible.value = false; loadAnomalies(); }
                    });
                };

                // --- 投诉反馈处理 ---
                const feedbacks = ref([]);
                const feedbackTotal = ref(0);
                const feedbackCurrent = ref(1);
                const feedbackLoading = ref(false);
                const feedbackSearch = reactive({ status: '' });
                const feedbackDialogVisible = ref(false);
                const feedbackForm = reactive({ id: '', status: 1, replyContent: '' });
                const loadFeedbacks = (page = feedbackCurrent.value) => {
                    feedbackCurrent.value = page;
                    feedbackLoading.value = true;
                    axios.get('/api/feedback/page', { params: { current: page, size: 10, status: feedbackSearch.status } }).then(r => {
                        if (r.data.code === 200) { feedbacks.value = r.data.data?.records || []; feedbackTotal.value = r.data.data?.total || 0; }
                    }).finally(() => feedbackLoading.value = false);
                };
                const openReplyFeedback = (row) => {
                    feedbackDialogVisible.value = true;
                    feedbackForm.id = row.id;
                    feedbackForm.status = row.status === 0 ? 1 : row.status;
                    feedbackForm.replyContent = row.replyContent || '';
                };
                const saveFeedback = () => {
                    if (!feedbackForm.replyContent) return ElementPlus.ElMessage.warning('请填写回复说明');
                    axios.put(`/api/feedback/${feedbackForm.id}/reply?status=${feedbackForm.status}&replyContent=${feedbackForm.replyContent}`).then(r => {
                    if (!anomalyForm.processResult) return ElementPlus.ElMessage.warning('请填写处理结果');
                    });
                };

                // --- 社保配置 ---
                const socConfigs = ref([]);
                const socTotal = ref(0);
                const socCurrent = ref(1);
                const socLoading = ref(false);
                const socSearch = reactive({ configName: '' });
                const socDialogVisible = ref(false);
                const socForm = reactive({ id: '', configName: '', minBase: 0, maxBase: 0, pensionRatio: 8, medicalRatio: 2, unemploymentRatio: 0.5, housingFundRatio: 7, isActive: 0 });
                const loadSocConfigs = (page = socCurrent.value) => {
                    socCurrent.value = page;
                    socLoading.value = true;
                    axios.get('/api/social-config/page', { params: { current: page, size: 10, configName: socSearch.configName } }).then(r => {
                        if (r.data.code === 200) { socConfigs.value = r.data.data?.records || []; socTotal.value = r.data.data?.total || 0; }
                    }).finally(() => socLoading.value = false);
                };
                const openSocForm = (row) => {
                    socDialogVisible.value = true;
                    if (row) Object.assign(socForm, row);
                    else Object.assign(socForm, { id: '', configName: '', minBase: 3000, maxBase: 30000, pensionRatio: 8, medicalRatio: 2, unemploymentRatio: 0.5, housingFundRatio: 7, isActive: 0 });
                };
                const saveSocConfig = () => {
                    const req = socForm.id ? axios.put('/api/social-config', socForm) : axios.post('/api/social-config', socForm);
                    req.then(r => {
                        if (r.data.code === 200) { ElementPlus.ElMessage.success('保存成功'); socDialogVisible.value = false; loadSocConfigs(); }
                    if (!feedbackForm.replyContent) return ElementPlus.ElMessage.warning('请填写回复说明');
                };

                // --- 工资个税与社保 ---
                const taxRecords = ref([]);
                const taxTotal = ref(0);
                const taxCurrent = ref(1);
                const taxLoading = ref(false);
                const taxSearch = reactive({ empName: '', deptId: '' });
                const taxDialogVisible = ref(false);
                const taxForm = reactive({ id: '', empName: '', deptName: '', housingFund: 0, medicalInsurance: 0, pensionInsurance: 0, socialSecurityEmp: 0, incomeTax: 0, deductDate: '', managerNo: '', managerName: '' });
                const taxDisplayRows = computed(() => taxRecords.value.map(row => ({
                    ...row,
                    empName: row.empName || row.realName || '-',
                    deptName: row.deptName || '-',
                    deptId: row.deptId || '',
                    housingFund: row.housingFund ?? row.housingFundDeduct ?? 0,
                    medicalInsurance: row.medicalInsurance ?? 0,
                    pensionInsurance: row.pensionInsurance ?? 0,
                    socialSecurityEmp: row.socialSecurityEmp ?? 0,
                    incomeTax: row.incomeTax ?? 0,
                    deductDate: (row.deductDate || row.createTime || '').toString().slice(0, 10) || '-',
                    managerNo: row.managerNo || '-',
                    managerName: row.managerName || '-'
                })).filter(row => {
                    const matchName = !taxSearch.empName || String(row.empName || '').includes(taxSearch.empName);
                    const matchDept = !taxSearch.deptId || String(row.deptId || '') === String(taxSearch.deptId);
                    return matchName && matchDept;
                }));
                const loadTaxRecords = (page = taxCurrent.value) => {
                    taxCurrent.value = page;
                    taxLoading.value = true;
                    axios.get('/api/salary/page', { params: { current: page, size: 10, realName: taxSearch.empName, empName: taxSearch.empName, deptId: taxSearch.deptId } }).then(r => {
                        if (r.data.code === 200) {
                            taxRecords.value = r.data.data?.records || [];
                            taxTotal.value = r.data.data?.total || 0;
                        }
                    }).finally(() => taxLoading.value = false);
                };
                const openTaxDetail = (row) => {
                    ElementPlus.ElMessageBox.alert(`
                员工姓名：${row.empName || '-'}<br>
                部门：${row.deptName || '-'}<br>
                扣除公积金：${row.housingFund || 0}<br>
                医疗保险：${row.medicalInsurance || 0}<br>
                养老保险：${row.pensionInsurance || 0}<br>
                扣除社保：${row.socialSecurityEmp || 0}<br>
                扣除个税：${row.incomeTax || 0}<br>
                扣除日期：${row.deductDate || '-'}<br>
                经理账号：${row.managerNo || '-'}<br>
                经理姓名：${row.managerName || '-'}
            `, '工资个税与社保详情', { dangerouslyUseHTMLString: true });
                };
                const openTaxForm = (row) => {
                    Object.assign(taxForm, row || { id: '', empName: '', deptName: '', housingFund: 0, medicalInsurance: 0, pensionInsurance: 0, socialSecurityEmp: 0, incomeTax: 0, deductDate: '', managerNo: '', managerName: '' });
                    taxDialogVisible.value = true;
                };
                const saveTaxRecord = () => {
                    taxDialogVisible.value = false;
                    ElementPlus.ElMessage.warning('当前后台暂未开放单条工资个税与社保修改接口，已先完成页面字段与弹窗对齐');
                };
                const deleteTaxRecord = () => {
                    ElementPlus.ElMessage.warning('当前后台暂未开放单条工资个税与社保删除接口');
                };

                // --- 绩效考核 ---
                const performances = ref([]);
                const perfTotal = ref(0);
                const perfCurrent = ref(1);
                const perfLoading = ref(false);
                const perfSearch = reactive({ empName: '', deptId: '', managerName: '' });
                const perfDialogVisible = ref(false);
                const perfForm = reactive({ id: '', yearMonth: '', empId: '', attendanceScore: 20, attitudeScore: 20, skillScore: 20, performanceScore: 20, bonusPenaltyScore: 0, score: 80, evalComment: '' });
                const performanceDisplayRows = computed(() => performances.value.map(row => ({
                    ...row,
                    empName: row.empName || row.realName || '-',
                    deptName: row.deptName || '-',
                    deptId: row.deptId || '',
                    attendanceScore: row.attendanceScore ?? '-',
                    attitudeScore: row.attitudeScore ?? '-',
                    skillScore: row.skillScore ?? '-',
            `, '工资个税与社保详情', { dangerouslyUseHTMLString: true });
                    bonusPenaltyScore: row.bonusPenaltyScore ?? 0,
                    score: row.score ?? 0,
                    grade: row.grade || calcPerfGrade(row.score ?? 0),
                    createTime: (row.createTime || '').toString().slice(0, 10) || '-',
                    managerNo: row.managerNo || '-',
                    managerName: row.managerName || '-'
                })).filter(row => {
                    const matchName = !perfSearch.empName || String(row.empName || '').includes(perfSearch.empName);
                    const matchDept = !perfSearch.deptId || String(row.deptId || '') === String(perfSearch.deptId);
                    const matchManager = !perfSearch.managerName || String(row.managerName || '').includes(perfSearch.managerName);
                    return matchName && matchDept && matchManager;
                }));
                const calcPerfTotal = (form) => {
                    const total = Number(form.attendanceScore || 0) + Number(form.attitudeScore || 0) + Number(form.skillScore || 0) + Number(form.performanceScore || 0) + Number(form.bonusPenaltyScore || 0);
                    return Math.max(0, Math.min(100, total));
                };
                const calcPerfGrade = (score) => {
                    if (score >= 90) return '优秀';
                    if (score >= 80) return '良好';
                    if (score >= 70) return '中等';
                    if (score >= 60) return '及格';
                    return '待提升';
                };
                const loadPerformances = (page = perfCurrent.value) => {
                    perfCurrent.value = page;
                    perfLoading.value = true;
                    axios.get('/api/performance/page', { params: { current: page, size: 10, empName: perfSearch.empName, realName: perfSearch.empName, deptId: perfSearch.deptId, managerName: perfSearch.managerName } }).then(r => {
                        if (r.data.code === 200) { performances.value = r.data.data?.records || []; perfTotal.value = r.data.data?.total || 0; }
                    }).finally(() => perfLoading.value = false);
                };
                const openPerfForm = (row) => {
                    perfDialogVisible.value = true;
                    if (row) Object.assign(perfForm, row);
                    else {
                        const now = new Date();
                        Object.assign(perfForm, { id: '', yearMonth: now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0'), empId: '', attendanceScore: 20, attitudeScore: 20, skillScore: 20, performanceScore: 20, bonusPenaltyScore: 0, score: 80, evalComment: '' });
                    }
                };
                const savePerformance = () => {
                    if (!perfForm.empId || !perfForm.yearMonth) return ElementPlus.ElMessage.warning('员工与月份必填');
                    const totalScore = calcPerfTotal(perfForm);
                    const params = {
                        id: perfForm.id || undefined,
                        empId: perfForm.empId,
                        yearMonth: perfForm.yearMonth,
                        score: totalScore,
                        evalComment: perfForm.evalComment
                    };
                    const req = params.id ? axios.put('/api/performance', params) : axios.post('/api/performance', params);
                    req.then(r => {
                        if (r.data.code === 200) { ElementPlus.ElMessage.success(params.id ? '修改成功' : '录入成功'); perfDialogVisible.value = false; loadPerformances(); }
                    return '待提升';
                    });
                };
                const openPerformanceDetail = (row) => {
                    ElementPlus.ElMessageBox.alert(`
                姓名：${row.empName || '-'}<br>
                部门：${row.deptName || '-'}<br>
                绩效月份：${row.yearMonth || '-'}<br>
                员工考勤：${row.attendanceScore ?? '-'}<br>
                工作态度：${row.attitudeScore ?? '-'}<br>
                业务技能：${row.skillScore ?? '-'}<br>
                工作绩效：${row.performanceScore ?? '-'}<br>
                奖惩加减分：${row.bonusPenaltyScore ?? 0}<br>
                总得分：${row.score ?? 0}<br>
                评价等级：${row.grade || calcPerfGrade(row.score || 0)}<br>
                经理账号：${row.managerNo || '-'}<br>
                经理姓名：${row.managerName || '-'}
            `, '绩效评分详情', { dangerouslyUseHTMLString: true });
                    if (!perfForm.empId || !perfForm.yearMonth) return ElementPlus.ElMessage.warning('员工与月份必填');
                const deletePerformance = (row) => {
                    if (!row.id) return ElementPlus.ElMessage.warning('当前记录缺少主键，无法删除');
                    ElementPlus.ElMessageBox.confirm('确认删除该条绩效评分记录吗？', '提示', { type: 'warning' }).then(() => {
                        axios.delete('/api/performance/' + row.id).then(r => {
                            if (r.data.code === 200) {
                                ElementPlus.ElMessage.success('删除成功');
                                loadPerformances(1);
                            } else {
                                ElementPlus.ElMessage.error(r.data.message || '删除失败');
                            }
                        }).catch(() => {
                            ElementPlus.ElMessage.warning('当前后台暂未开放绩效评分删除接口');
                        });
                    }).catch(() => { });
                };

                // --- 薪资计算 ---
                const salaries = ref([]);
                const salaryTotal = ref(0);
                const salaryCurrent = ref(1);
                const salaryLoading = ref(false);
                const salarySearch = reactive({ yearMonth: '', empNo: '', empName: '', deptId: '' });
                const salarySelections = ref([]);
                const salaryDisplayRows = computed(() => salaries.value.map(row => ({
                    ...row,
                    empNo: row.empNo || '-',
                    empName: row.empName || '-',
                    deptName: row.deptName || '-',
                    baseSalary: row.baseSalary ?? 0,
                    overtimePay: row.overtimePay ?? 0,
                    perfBonus: row.perfBonus ?? 0,
                    if (!row.id) return ElementPlus.ElMessage.warning('当前记录缺少主键，无法删除');
                    deductAmount: Number(row.socialSecurityEmp || 0) + Number(row.incomeTax || 0) + Number(row.attendDeduct || 0),
                    netSalary: row.netSalary ?? 0,
                    recordDate: (row.recordDate || row.createTime || '').toString().slice(0, 10) || '-'
                })));

                const loadSalaries = (page = salaryCurrent.value) => {
                    salaryCurrent.value = page;
                    salaryLoading.value = true;
                    axios.get('/api/salary/page', { params: { current: page, size: 10, yearMonth: salarySearch.yearMonth, empNo: salarySearch.empNo, empName: salarySearch.empName, realName: salarySearch.empName, deptId: salarySearch.deptId } }).then(r => {
                            ElementPlus.ElMessage.warning('当前后台暂未开放绩效评分删除接口');
                    }).finally(() => salaryLoading.value = false);
                };
                const handleSalarySelection = (val) => { salarySelections.value = val; };
                const saveAllowance = (row) => {
                    const payload = { ...row, allowance: Number(row.allowance || 0) };
                    axios.put('/api/salary/manual-update', payload).then(r => {
                        if (r.data.code === 200) {
                            ElementPlus.ElMessage.success(`已保存${row.empName} 的津贴调整`);
                            loadSalaries();
                        } else {
                            ElementPlus.ElMessage.error(r.data.message || '保存失败');
                        }
                    });
                };
                const batchCalcSalary = () => {
                    if (!salarySearch.yearMonth) return ElementPlus.ElMessage.warning('请先在搜索栏选择需要算薪的账期(月份)');
                    const loadInstance = ElementPlus.ElLoading.service({ text: '引擎拼命计算中...' });
                    axios.post('/api/salary/batch-calculate?yearMonth=' + salarySearch.yearMonth).then(r => {
                        if (r.data.code === 200) { ElementPlus.ElMessage.success('批量算薪结束，共生成本月账单记录'); loadSalaries(); }
                        else ElementPlus.ElMessage.error(r.data.message || '算薪失败');
                    }).finally(() => loadInstance.close());
                };
                const batchPublishSalary = () => {
                    let ids = salarySelections.value.map(s => s.id);
                    if (ids.length === 0) {
                        ids = salaries.value.filter(s => s.calcStatus === 1).map(s => s.id);
                    }
                    if (ids.length === 0) return ElementPlus.ElMessage.warning('当前账单中没有可发布的草稿记录');
                    axios.put('/api/salary/batch-publish', ids).then(r => {
                        if (r.data.code === 200) { ElementPlus.ElMessage.success('批量公开完成，员工已能收到账单'); loadSalaries(); }
                    });
                };
                const openSalaryDetail = (row) => {
                    ElementPlus.ElMessageBox.alert(
                        `实发工资 = 基本工资 + 津贴 + 绩效 + 加班奖励 - 社保 - 考勤扣款 - 个税
                <hr>
                基本工资: ${row.baseSalary}<br>
                津贴: ${row.allowance}<br>
                绩效: ${row.perfBonus}<br>
                加班奖励: ${row.overtimePay || 0}<br>
                社保扣除: ${row.socialSecurityEmp}<br>
                考勤扣款: ${row.attendDeduct}<br>
                个税扣除: ${row.incomeTax}<br>
                发薪账户: ${row.bankAccount || '未维护'}`,
                        `员工 ${row.empName} 的薪资核算明细`,
                        { dangerouslyUseHTMLString: true }
                    );
                };
                const deleteSalaryRecord = () => {
                    ElementPlus.ElMessage.warning('当前后台暂未开放薪资核算单条删除接口');
                };

                // --- 薪资支付 ---
                const payments = ref([]);
                const paymentTotal = ref(0);
                const paymentCurrent = ref(1);
                const paymentLoading = ref(false);
                    if (ids.length === 0) return ElementPlus.ElMessage.warning('当前账单中没有可发布的草稿记录');
                const paymentDisplayRows = computed(() => payments.value.map(row => ({
                        if (r.data.code === 200) { ElementPlus.ElMessage.success('批量公开完成，员工已能收到账单'); loadSalaries(); }
                    empNo: row.empNo || '-',
                    empName: row.empName || '-',
                    deptName: row.deptName || '-',
                    baseSalary: row.baseSalary ?? 0,
                    overtimePay: row.overtimePay ?? 0,
                    perfBonus: row.perfBonus ?? 0,
                    allowance: row.allowance ?? 0,
                    deductAmount: Number(row.socialSecurityEmp || 0) + Number(row.incomeTax || 0) + Number(row.attendDeduct || 0),
                    netSalary: row.netSalary ?? 0,
                    payDate: (row.payTime || row.recordDate || row.createTime || '').toString().slice(0, 10) || '-',
                    isPaid: Number(row.calcStatus) === 4,
                    issueFile: row.issueFile || '-'
                })));
                发薪账户: ${row.bankAccount || '未维护'}`,
                const loadPayments = (page = paymentCurrent.value) => {
                    paymentCurrent.value = page;
                    paymentLoading.value = true;
                    axios.get('/api/salary/page', { params: { current: page, size: 10, yearMonth: paymentSearch.yearMonth, empNo: paymentSearch.empNo, empName: paymentSearch.empName, realName: paymentSearch.empName, deptId: paymentSearch.deptId, calcStatus: paymentSearch.calcStatus } }).then(r => {
                        if (r.data.code === 200) { payments.value = r.data.data?.records || []; paymentTotal.value = r.data.data?.total || 0; }
                    ElementPlus.ElMessage.warning('当前后台暂未开放薪资核算单条删除接口');
                };
                const executePayment = (row) => {
                    if (row.calcStatus < 2) return ElementPlus.ElMessage.warning('该账单未公开定表，不可打款');
                    ElementPlus.ElMessageBox.confirm(`准备向账户 ${row.bankAccount || '【未绑定对公账户】'} 汇款 ¥${row.netSalary} 元，是否连通银行接口执行？`, '安全支付网关接入中...').then(() => {
                        const loadInstance = ElementPlus.ElLoading.service({ text: '正在验证网银密钥与通讯...' });
                        setTimeout(() => {
                            axios.put('/api/salary/' + row.id + '/pay').then(r => {
                                if (r.data.code === 200) { ElementPlus.ElMessage.success('支付成功，资金已汇出'); loadPayments(); }
                                else ElementPlus.ElMessage.error(r.data.message || '打款失败');
                            }).finally(() => loadInstance.close());
                        }, 1500);
                    }).catch(() => { });
                };

                // --- 鍛樺伐绠＄悊 ---
                const employees = ref([]);
                const empTotal = ref(0);
                const empCurrent = ref(1);
                const empSize = ref(10);
                const empLoading = ref(false);
                const empSearch = reactive({ empNo: '', realName: '', deptId: '' });
                const empDialogVisible = ref(false);
                const empDialogMode = ref('employee');
                const empForm = reactive({ id: '', empNo: '', realName: '', gender: 1, phone: '', idCard: '', deptId: '', baseSalary: 0, hireDate: '', initialPassword: '', avatar: '', role: '', position: '', positionName: '' });
                const importInput = ref(null);
                const avatarInput = ref(null);
                const avatarUploading = ref(false);
                const empSelections = ref([]);
                const employeeDisplayRows = computed(() => employees.value
                    .filter(row => !isManagerRecord(row))
                    .map(row => ({ ...row, avatar: row.avatar || getStoredAvatar(row.empNo) || '' })));
                const empDialogTitle = computed(() => {
                    if (row.calcStatus < 2) return ElementPlus.ElMessage.warning('该账单未公开定表，不可打款');
                    ElementPlus.ElMessageBox.confirm(`准备向账户 ${row.bankAccount || '【未绑定对公账户】'} 汇款 ￥${row.netSalary} 元，是否继续执行？`, '安全支付网关接入中...').then(() => {
                });
                const empDialogSubmitText = computed(() => empDialogMode.value === 'manager' ? '淇濆瓨缁忕悊淇℃伅' : '淇濆瓨鍛樺伐淇℃伅');
                const empBaseSalaryText = computed(() => empForm.baseSalary == null || empForm.baseSalary === '' ? '' : String(empForm.baseSalary));
                const syncEmpDeptSalary = (deptId) => {
                    const currentDept = departments.value.find(item => String(item.id) === String(deptId || empForm.deptId));
                    if (!currentDept) return;
                    const deptSalary = currentDept.baseSalary ?? currentDept.basicSalary ?? currentDept.defaultSalary;
                    if (deptSalary != null && deptSalary !== '') empForm.baseSalary = deptSalary;
                };

                const loadEmployees = (page = empCurrent.value) => {
                    empCurrent.value = page;
                    empLoading.value = true;
                    axios.get('/api/employee/page', { params: { current: page, size: empSize.value, ...empSearch, status: 1 } }).then(r => {
                        if (r.data.code === 200) {
                            employees.value = r.data.data?.records || r.data.data || [];
                            empTotal.value = r.data.data?.total || 0;
                        }
                    }).finally(() => { empLoading.value = false; });
                };

                const resetEmpSearch = () => {
                    Object.assign(empSearch, { empNo: '', realName: '', deptId: '' });
                    empCurrent.value = 1;
                    loadEmployees(1);
                };
                const handleEmpSelection = (rows) => {
                    empSelections.value = rows || [];
                };
                const openEmpForm = (row, mode = 'employee') => {
                    empDialogMode.value = mode;
                    empDialogVisible.value = true;
                    if (row) {
                        Object.assign(empForm, { role: '', position: '', positionName: '', initialPassword: '', avatar: '' }, row);
                        empForm.avatar = row.avatar || getStoredAvatar(row.empNo) || '';
                        syncEmpDeptSalary(row.deptId);
                    } else {
                        Object.assign(empForm, { id: '', empNo: '', realName: '', gender: 1, phone: '', idCard: '', deptId: '', baseSalary: null, hireDate: '', initialPassword: '', avatar: '', role: mode === 'manager' ? 2 : '', position: mode === 'manager' ? '閮ㄩ棬缁忕悊' : '', positionName: mode === 'manager' ? '閮ㄩ棬缁忕悊' : '' });
                    }
                };
                const triggerAvatarUpload = () => {
                    if (avatarUploading.value) return;
                    avatarInput.value?.click();
                };
                const fallbackLocalAvatar = (file, event) => {
                    const reader = new FileReader();
                    reader.onload = () => {
                        const image = new Image();
                        image.onload = () => {
                            const maxSide = 180;
                            const scale = Math.min(1, maxSide / Math.max(image.width || 1, image.height || 1));
                            const canvas = document.createElement('canvas');
                            canvas.width = Math.max(1, Math.round(image.width * scale));
                            canvas.height = Math.max(1, Math.round(image.height * scale));
                            const ctx = canvas.getContext('2d');
                            if (!ctx) {
                                avatarUploading.value = false;
                                event.target.value = '';
                                ElementPlus.ElMessage.error('当前浏览器无法处理这张图片');
                                return;
                            }
                            ctx.clearRect(0, 0, canvas.width, canvas.height);
                            ctx.drawImage(image, 0, 0, canvas.width, canvas.height);
                            empForm.avatar = canvas.toDataURL('image/jpeg', 0.86);
                            ElementPlus.ElMessage.warning('服务器上传暂不可用，已临时本地保存头像');
                            avatarUploading.value = false;
                            event.target.value = '';
                        };
                        image.onerror = () => {
                            avatarUploading.value = false;
                            event.target.value = '';
                            ElementPlus.ElMessage.error('头像读取失败，请换一张图片再试');
                        };
                        image.src = reader.result;
                    };
                    reader.onerror = () => {
                        avatarUploading.value = false;
                        event.target.value = '';
                        ElementPlus.ElMessage.error('头像读取失败，请换一张图片再试');
                    };
                    reader.readAsDataURL(file);
                };
                const handleAvatarFile = (event) => {
                    const file = event.target.files?.[0];
                    if (!file) return;
                    if (!file.type.startsWith('image/')) {
                        ElementPlus.ElMessage.warning('请选择图片文件');
                        event.target.value = '';
                                ElementPlus.ElMessage.error('当前浏览器无法处理这张图片');
                    }
                    avatarUploading.value = true;
                    const formData = new FormData();
                    formData.append('file', file);
                    axios.post('/api/employee/upload-avatar', formData, {
                            ElementPlus.ElMessage.warning('服务器上传暂不可用，已临时本地保存头像');
                    }).then(r => {
                        if (r.data.code === 200 && r.data.data) {
                            empForm.avatar = r.data.data;
                            ElementPlus.ElMessage.success('头像上传成功');
                            avatarUploading.value = false;
                            event.target.value = '';
                            ElementPlus.ElMessage.error('头像读取失败，请换一张图片再试');
                            fallbackLocalAvatar(file, event);
                        }
                    }).catch(() => {
                        fallbackLocalAvatar(file, event);
                    });
                };
                        ElementPlus.ElMessage.error('头像读取失败，请换一张图片再试');
                    if (!empForm.empNo || !empForm.realName || !empForm.deptId) return ElementPlus.ElMessage.warning('\u8bf7\u586b\u5199\u5fc5\u586b\u9879');
                    const params = { ...empForm };
                    if (!empForm.id && empForm.initialPassword) params.initialPassword = empForm.initialPassword;
                    if (empDialogMode.value === 'manager') {
                        params.role = 2;
                        params.position = params.position || '閮ㄩ棬缁忕悊';
                        params.positionName = params.positionName || '閮ㄩ棬缁忕悊';
                        params.managerNo = params.managerNo || params.empNo;
                        params.managerName = params.managerName || params.realName;
                    }
                    const url = empForm.id ? '/api/employee' : '/api/employee?initialPassword=' + (empForm.initialPassword || '');
                    const req = empForm.id ? axios.put(url, params) : axios.post(url, params);
                    req.then(r => {
                        if (r.data.code === 200) {
                            ElementPlus.ElMessage.success('\u4fdd\u5b58\u6210\u529f');
                            rememberEmployeeAvatar(params.empNo, params.avatar);
                            empDialogVisible.value = false;
                            if (empDialogMode.value === 'manager') {
                                rememberManagerEmpNo(params.empNo);
                                loadDeptManagers();
                                loadEmployees(1);
                            } else {
                                loadEmployees();
                            }
                        } else ElementPlus.ElMessage.error(r.data.message || '\u4fdd\u5b58\u5931\u8d25');
                    });
                };
                const deleteEmp = (id, row) => {
                    ElementPlus.ElMessageBox.confirm('\u786e\u8ba4\u5220\u9664\u8be5\u5458\u5de5\u8bb0\u5f55\uff1f\u5982\u679c\u540e\u7aef\u4ecd\u662f\u505c\u7528\u903b\u8f91\uff0c\u7cfb\u7edf\u4f1a\u81ea\u52a8\u5c06\u5176\u7f6e\u4e3a\u79bb\u804c\u3002', '\u8b66\u544a', { type: 'error' }).then(() => {
                        axios.delete('/api/employee/' + id).then(r => {
                            if (r.data.code === 200) {
                                rememberEmployeeAvatar(row?.empNo, '');
                                ElementPlus.ElMessage.success('\u5220\u9664\u6210\u529f');
                                loadEmployees();
                            } else ElementPlus.ElMessage.error(r.data.message || '\u64cd\u4f5c\u5931\u8d25');
                        });
                    }).catch(() => { });
                };
                const batchDeleteEmp = () => {
                    const ids = empSelections.value.map(item => item.id).filter(Boolean);
                    if (!ids.length) return ElementPlus.ElMessage.warning('\u8bf7\u5148\u52fe\u9009\u8981\u5220\u9664\u7684\u5458\u5de5');
                    ElementPlus.ElMessageBox.confirm('\u786e\u8ba4\u6279\u91cf\u5220\u9664\u5df2\u9009\u4e2d\u7684 ' + ids.length + ' \u540d\u5458\u5de5\u5417\uff1f', '\u6279\u91cf\u5220\u9664', { type: 'warning' }).then(() => {
                        Promise.allSettled(ids.map(id => axios.delete('/api/employee/' + id))).then((results) => {
                            const successCount = results.filter(item => item.status === 'fulfilled' && item.value?.data?.code === 200).length;
                            if (successCount > 0) {
                                ElementPlus.ElMessage.success('\u5df2\u5904\u7406 ' + successCount + ' \u540d\u5458\u5de5');
                                empSelections.value = [];
                                loadEmployees(1);
                            } else {
                                ElementPlus.ElMessage.error('\u6279\u91cf\u5220\u9664\u5931\u8d25');
                            }
                        });
                    }).catch(() => { });
                };
                const triggerImport = () => importInput.value.click();
                const handleImportFile = (e) => {
                    const file = e.target.files[0];
                    if (!file) return;
                    const fd = new FormData();
                    fd.append('file', file);
                    const loadInstance = ElementPlus.ElLoading.service({ text: '姝ｅ湪瑙ｆ瀽Excel瀵煎叆...' });
                    axios.post('/api/employee/import', fd).then(r => {
                        if (r.data.code === 200) { ElementPlus.ElMessage.success('瀵煎叆鎴愬姛'); loadEmployees(1); }
                        else ElementPlus.ElMessage.error(r.data.message || '瀵煎叆澶辫触');
                    }).catch(err => ElementPlus.ElMessage.error(err.response?.data?.message || '缃戠粶閿欒'))
                        .finally(() => { loadInstance.close(); e.target.value = ''; });
                };
                const exportExcel = () => {
                    const query = new URLSearchParams();
                    if (empSearch.empNo) query.append('empNo', empSearch.empNo);
                    if (empSearch.realName) query.append('realName', empSearch.realName);
                    if (empSearch.deptId) query.append('deptId', empSearch.deptId);
                    const url = '/api/employee/export' + (query.toString() ? `?${query.toString()}` : '');
                    window.open(url, '_blank');
                };

                const exportRowsToCsv = (filename, columns, rows) => {
                    const head = columns.map(c => c.label).join(',');
                    const body = rows.map(row => columns.map(c => {
                        const raw = typeof c.value === 'function' ? c.value(row) : row[c.value];
                        const text = raw == null ? '' : String(raw).replace(/"/g, '""');
                        return `"${text}"`;
                    }).join(','));
                    const csv = '\ufeff' + [head, ...body].join('\n');
                    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
                    const url = URL.createObjectURL(blob);
                    const link = document.createElement('a');
                    link.href = url;
                    link.download = filename;
                    link.click();
                    URL.revokeObjectURL(url);
                };

                const exportAttendanceCsv = () => {
                    exportRowsToCsv('\u8003\u52e4\u6570\u636e.csv', [
                        { label: '\u767b\u8bb0\u7f16\u53f7', value: 'recordNo' },
                        { label: '\u6708\u4efd', value: 'yearMonth' },
                        { label: '\u5de5\u53f7', value: 'empNo' },
                        { label: '\u59d3\u540d', value: 'empName' },
                        { label: '\u90e8\u95e8', value: 'deptName' },
                        { label: '\u51fa\u52e4\u5929\u6570', value: 'attendDays' },
                        { label: '\u65f7\u5de5\u5929\u6570', value: 'absentDays' },
                        { label: '\u8fdf\u5230\u5929\u6570', value: 'lateTimes' },
                        { label: '\u8bf7\u5047\u5929\u6570', value: 'leaveDays' },
                        { label: '\u51fa\u5dee\u65f6\u95f4', value: 'remark' },
                        { label: '\u767b\u8bb0\u65e5\u671f', value: 'createTime' },
                        { label: '\u7ecf\u7406\u8d26\u53f7', value: 'managerNo' },
                        { label: '\u7ecf\u7406\u59d3\u540d', value: 'managerName' }
                    ], attendanceDisplayRows.value);
                };
                const exportTaxCsv = () => {
                    exportRowsToCsv('\u5de5\u8d44\u4e2a\u7a0e\u4e0e\u793e\u4fdd.csv', [
                        { label: '\u5458\u5de5\u59d3\u540d', value: 'empName' },
                        { label: '\u90e8\u95e8', value: 'deptName' },
                        { label: '\u6263\u9664\u516c\u79ef\u91d1', value: 'housingFund' },
                        { label: '\u533b\u7597\u4fdd\u9669', value: 'medicalInsurance' },
                        { label: '\u517b\u8001\u4fdd\u9669', value: 'pensionInsurance' },
                        { label: '\u6263\u9664\u793e\u4fdd', value: 'socialSecurityEmp' },
                        { label: '\u6263\u9664\u4e2a\u7a0e', value: 'incomeTax' },
                        { label: '\u6263\u9664\u65e5\u671f', value: 'effectiveDate' },
                        { label: '\u7ecf\u7406\u8d26\u53f7', value: 'managerNo' },
                        { label: '\u7ecf\u7406\u59d3\u540d', value: 'managerName' }
                    ], taxDisplayRows.value);
                };
                const exportPerformanceCsv = () => {
                    exportRowsToCsv('\u7ee9\u6548\u8bc4\u5206.csv', [
                        { label: '\u59d3\u540d', value: 'empName' },
                        { label: '\u90e8\u95e8', value: 'deptName' },
                        { label: '\u7ee9\u6548\u6708\u4efd', value: 'yearMonth' },
                        { label: '\u5458\u5de5\u8003\u52e4', value: 'attendanceScore' },
                        { label: '\u5de5\u4f5c\u6001\u5ea6', value: 'attitudeScore' },
                        { label: '\u4e1a\u52a1\u6280\u80fd', value: 'skillScore' },
                        { label: '\u5de5\u4f5c\u7ee9\u6548', value: 'performanceScore' },
                        { label: '\u5956\u60e9\u52a0\u51cf\u5206', value: 'bonusPenaltyScore' },
                        { label: '\u603b\u5f97\u5206', value: row => row.score },
                        { label: '\u8bc4\u4ef7\u7b49\u7ea7', value: 'grade' },
                        { label: '\u6dfb\u52a0\u65f6\u95f4', value: 'createTime' },
                        { label: '\u7ecf\u7406\u8d26\u53f7', value: 'managerNo' },
                        { label: '\u7ecf\u7406\u59d3\u540d', value: 'managerName' }
                    ], performanceDisplayRows.value);
                };
                const exportDeptManagerCsv = () => {
                    exportRowsToCsv('\u90e8\u95e8\u7ecf\u7406.csv', [
                        { label: '\u7ecf\u7406\u8d26\u53f7', value: row => row.managerNo || row.empNo },
                        { label: '\u7ecf\u7406\u59d3\u540d', value: 'realName' },
                        { label: '\u6027\u522b', value: row => row.gender === 1 ? '\u7537' : '\u5973' },
                        { label: '\u624b\u673a', value: 'phone' },
                        { label: '\u90e8\u95e8', value: 'deptName' }
                    ], managerDisplayRows.value);
                };
                const exportSalaryCsv = () => {
                    exportRowsToCsv('\u85aa\u8d44\u6838\u7b97.csv', [
                        { label: '\u6708\u4efd', value: 'yearMonth' },
                        { label: '\u5de5\u53f7', value: 'empNo' },
                        { label: '\u59d3\u540d', value: 'empName' },
                        { label: '\u90e8\u95e8', value: 'deptName' },
                        { label: '\u57fa\u672c\u5de5\u8d44', value: 'baseSalary' },
                        { label: '\u52a0\u73ed\u5de5\u8d44', value: 'overtimePay' },
                        { label: '\u7ee9\u6548\u5956\u91d1', value: 'perfBonus' },
                        { label: '\u5176\u4ed6\u8865\u52a9', value: 'allowance' },
                        { label: '\u6263\u6b3e\u91d1\u989d', value: 'deductAmount' },
                        { label: '\u5b9e\u53d1\u5de5\u8d44', value: 'netSalary' },
                        { label: '\u767b\u8bb0\u65e5\u671f', value: 'recordDate' }
                    ], salaryDisplayRows.value);
                };
                const exportPaymentCsv = () => {
                    exportRowsToCsv('\u85aa\u8d44\u53d1\u653e.csv', [
                        { label: '\u6708\u4efd', value: 'yearMonth' },
                        { label: '\u5de5\u53f7', value: 'empNo' },
                        { label: '\u59d3\u540d', value: 'empName' },
                        { label: '\u90e8\u95e8', value: 'deptName' },
                        { label: '\u57fa\u672c\u5de5\u8d44', value: 'baseSalary' },
                        { label: '\u52a0\u73ed\u5de5\u8d44', value: 'overtimePay' },
                        { label: '\u7ee9\u6548\u5956\u91d1', value: 'perfBonus' },
                        { label: '\u5176\u4ed6\u8865\u52a9', value: 'allowance' },
                        { label: '\u6263\u6b3e\u91d1\u989d', value: 'deductAmount' },
                        { label: '\u5b9e\u53d1\u5de5\u8d44', value: 'netSalary' },
                        { label: '\u53d1\u653e\u65e5\u671f', value: 'payDate' },
                        { label: '\u662f\u5426\u652f\u4ed8', value: row => row.isPaid ? '\u5df2\u652f\u4ed8' : '\u672a\u652f\u4ed8' },
                        { label: '\u53d1\u653e\u6587\u4ef6', value: 'issueFile' }
                    ], paymentDisplayRows.value);
                };
                const goWebsiteHome = () => {
                    window.location.href = '../home.html';
                };

                const backupDatabase = () => {
                    ElementPlus.ElMessageBox.confirm('\u786e\u8ba4\u7acb\u5373\u5907\u4efd\u6570\u636e\u5e93\u5417\uff1f', '\u7cfb\u7edf\u63d0\u793a', { type: 'warning' }).then(() => {
                        axios.post('/api/system/backup').then(r => {
                            if (r.data?.code === 200) ElementPlus.ElMessage.success('\u6570\u636e\u5e93\u5907\u4efd\u6210\u529f');
                            else ElementPlus.ElMessage.warning(r.data?.message || '\u5f53\u524d\u540e\u7aef\u672a\u914d\u7f6e\u81ea\u52a8\u5907\u4efd\u63a5\u53e3');
                        }).catch(() => {
                            ElementPlus.ElMessage.warning('\u5f53\u524d\u540e\u7aef\u672a\u914d\u7f6e\u81ea\u52a8\u5907\u4efd\u63a5\u53e3\uff0c\u8bf7\u5148\u624b\u52a8\u5907\u4efd\u6570\u636e\u5e93');
                        });
                    }).catch(() => { });
                };
                const handleUserMenuCommand = (command) => {
                    if (command === 'site') goWebsiteHome();
                    if (command === 'backup') backupDatabase();
                    if (command === 'logout') logout();
                };

                const logout = () => {
                    localStorage.clear();
                    window.location.href = '../login.html';
                };

                onMounted(() => {
                    if (!localStorage.getItem('token')) { window.location.href = '../login.html'; return; }
                    loadDashboard();
                });

                return {
                    userInfo, menus, currentMenu, currentMenuLabel, stats, notices,
                    selfInfo, selfEditForm, loadSelfInfo, saveSelfInfo,
                    pwdForm, updatePassword,
                    departments, deptLoading, deptDialogVisible, deptForm, loadDepartments, openDeptForm, saveDept, deleteDept,
                    managerSearch, deptManagers, deptManagerLoading, managerDisplayRows, loadDeptManagers, openDeptManagerForm, deleteDeptManager, exportDeptManagerCsv,
                    employees, empTotal, empCurrent, empLoading, empSearch, empDialogVisible, empDialogMode, empDialogTitle, empDialogSubmitText, empBaseSalaryText, empForm, importInput, empSelections, employeeDisplayRows,
                    loadEmployees, resetEmpSearch, handleEmpSelection, openEmpForm, syncEmpDeptSalary, saveEmployee, deleteEmp, batchDeleteEmp, triggerImport, handleImportFile, exportExcel,
                    avatarInput, avatarUploading, triggerAvatarUpload, handleAvatarFile,
                    // --- 鑰冨嫟 ---
                    attendances, attendTotal, attendCurrent, attendLoading, attendSearch, attendImportMonth, attendDialogVisible, attendForm, attendImportInput, allEmployees, attendanceDisplayRows, loadAttendances, openAttendForm, saveAttend, deleteAttend, triggerAttendImport, handleAttendImport, loadAllEmployeesForSelect, showAttendDetail, exportAttendanceCsv,
                    // --- 鍏憡 ---
                    noticeList, noticeTotal, noticeCurrent, noticeSearch, noticeLoading, noticeDialogVisible, noticeForm, loadNotices, openNoticeForm, saveNotice, deleteNotice,
                    // --- 瀹℃壒/寮傚父/鎶曡瘔 ---
                    applies, applyTotal, applyCurrent, applyLoading, applySearch, applyDisplayRows, loadApplies, reviewApply, showApplyDetail, deleteApply,
                    anomalies, anomalyTotal, anomalyCurrent, anomalyLoading, anomalySearch, anomalyDialogVisible, anomalyForm, loadAnomalies, openProcessAnomaly, saveAnomaly,
                    feedbacks, feedbackTotal, feedbackCurrent, feedbackLoading, feedbackSearch, feedbackDialogVisible, feedbackForm, loadFeedbacks, openReplyFeedback, saveFeedback,
                    // --- 绀句繚/缁╂晥 ---
                    socConfigs, socTotal, socCurrent, socLoading, socSearch, socDialogVisible, socForm, loadSocConfigs, openSocForm, saveSocConfig,
                    taxRecords, taxTotal, taxCurrent, taxLoading, taxSearch, taxDialogVisible, taxForm, taxDisplayRows, loadTaxRecords, openTaxDetail, openTaxForm, saveTaxRecord, deleteTaxRecord, exportTaxCsv,
                    performances, perfTotal, perfCurrent, perfLoading, perfSearch, perfDialogVisible, perfForm, performanceDisplayRows, calcPerfTotal, calcPerfGrade, loadPerformances, openPerfForm, savePerformance, openPerformanceDetail, deletePerformance, exportPerformanceCsv,
                    // --- 钖祫璁＄畻鍙戣柂 ---
                    salaries, salaryTotal, salaryCurrent, salaryLoading, salarySearch, salaryDisplayRows, handleSalarySelection, loadSalaries, saveAllowance, batchCalcSalary, batchPublishSalary, openSalaryDetail, deleteSalaryRecord, exportSalaryCsv,
                    payments, paymentTotal, paymentCurrent, paymentLoading, paymentSearch, paymentDisplayRows, loadPayments, executePayment, exportPaymentCsv,
                    switchMenu, logout, handleUserMenuCommand
                };
            }
        });
        Object.entries(ElementPlusIconsVue).forEach(([name, component]) => {
            app.component(name, component);
        });
        app.use(ElementPlus, {
            locale: window.ElementPlusLocaleZhCn || window.ElementPlusLocaleZhCN
        }).mount('#app');
    