
const { createApp, ref, reactive, onMounted, computed } = Vue;

// Element Plus 鏈湴鍖栵細寮哄埗鏈堜唤鏄剧ず涓烘暟瀛楋紙1鏈?12鏈堬級
const __elZhLocale = window.ElementPlusLocaleZhCn || window.ElementPlusLocaleZhCN;
if (__elZhLocale && __elZhLocale.el) {
    const monthMap = {
        jan: '1鏈?, feb: '2鏈?, mar: '3鏈?, apr: '4鏈?,
        may: '5鏈?, jun: '6鏈?, jul: '7鏈?, aug: '8鏈?,
        sep: '9鏈?, oct: '10鏈?, nov: '11鏈?, dec: '12鏈?
    };
    if (__elZhLocale.el.datepicker) __elZhLocale.el.datepicker.months = monthMap;
    if (__elZhLocale.el.datePicker) __elZhLocale.el.datePicker.months = monthMap;
}

// Axios 鎷︽埅鍣ㄦ坊鍔?token
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

        // --- 涓汉涓績鏁版嵁 ---
        const selfInfo = reactive({ empNo:'', realName:'', gender:'', phone:'', deptName:'', baseSalary:'', bankCard:'', positionName:'', hireDate:'', managerNo:'', managerName:'', avatar:'', id:'' });
        const selfEditForm = reactive({ phone:'', bankCard:'' });
        const loadSelfInfo = () => {
            const username = userInfo.value.username;
            if (!username) return;
            // 鍏堝皾璇曢€氳繃 /api/auth/info 鎷垮綋鍓嶇櫥褰曞憳宸ヤ俊鎭?            axios.get('/api/auth/info').then(r => {
                if (r.data.code === 200 && r.data.data) {
                    Object.assign(selfInfo, r.data.data);
                    selfEditForm.phone = selfInfo.phone || '';
                    selfEditForm.bankCard = selfInfo.bankCard || '';
                    return;
                }
                // fallback: 閫氳繃鍛樺伐鍒嗛〉鎼滅储鎸夎处鍙?                axios.get('/api/employee/page', { params: { username, size: 5 } }).then(r2 => {
                    const emp = r2.data.data?.records?.[0] || r2.data.data?.[0];
                    if (emp) {
                        Object.assign(selfInfo, emp);
                        selfEditForm.phone = selfInfo.phone || '';
                        selfEditForm.bankCard = selfInfo.bankCard || '';
                    }
                }).catch(()=>{});
            }).catch(()=> {
                // fallback
                axios.get('/api/employee/page', { params: { username, size: 5 } }).then(r2 => {
                    const emp = r2.data.data?.records?.[0] || r2.data.data?.[0];
                    if (emp) {
                        Object.assign(selfInfo, emp);
                        selfEditForm.phone = selfInfo.phone || '';
                        selfEditForm.bankCard = selfInfo.bankCard || '';
                    }
                }).catch(()=>{});
            });
        };
        const saveSelfInfo = () => {
            if (!selfInfo.id) return ElementPlus.ElMessage.warning('灏氭湭鍏宠仈鍒板憳宸ヤ俊鎭紝鏃犳硶淇敼');
            axios.put('/api/employee', { id: selfInfo.id, phone: selfEditForm.phone, bankCard: selfEditForm.bankCard }).then(r => {
                if (r.data.code === 200) {
                    ElementPlus.ElMessage.success('淇℃伅淇濆瓨鎴愬姛');
                    selfInfo.phone = selfEditForm.phone;
                    selfInfo.bankCard = selfEditForm.bankCard;
                } else ElementPlus.ElMessage.error(r.data.message || '淇濆瓨澶辫触');
            });
        };

        const menus = ref([
            { key: 'home', label: '\u7cfb\u7edf\u9996\u9875', icon: 'House' },
            { key: 'personal', label: '\u4e2a\u4eba\u4e2d\u5fc3', icon: 'User' },
            { key: 'employee', label: '\u5458\u5de5', icon: 'UserFilled' },
            { key: 'dept-manager', label: '\u90e8\u95e8\u7ecf\u7406', icon: 'Avatar' },
            { key: 'department', label: '\u90e8\u95e8\u4fe1\u606f', icon: 'OfficeBuilding' },
            { key: 'attendance-apply', label: '\u8003\u52e4\u7533\u8bf7', icon: 'Document' },
            { key: 'attendance', label: '\u8003\u52e4\u6570\u636e', icon: 'Calendar' },
            { key: 'salary', label: '\u85aa\u8d44\u6838\u7b97', icon: 'Money' },
            { key: 'salary-feedback', label: '\u85aa\u8d44\u53cd\u9988', icon: 'ChatDotRound' },
            { key: 'salary-payment', label: '\u85aa\u8d44\u53d1\u653e', icon: 'Wallet' },
            { key: 'tax', label: '\u5de5\u8d44\u4e2a\u7a0e\u4e0e\u793e\u4fdd', icon: 'Tickets' },
            { key: 'performance', label: '\u7ee9\u6548\u8bc4\u5206', icon: 'Histogram' },
            { key: 'complaint', label: '\u6295\u8bc9\u53cd\u9988', icon: 'ChatLineRound' },
            { key: 'anomaly', label: '\u5f02\u5e38\u4e0a\u62a5', icon: 'Warning' },
            { key: 'notice', label: '\u901a\u77e5\u516c\u544a', icon: 'Bell' }
        ]);
        const currentMenuLabel = computed(() => menus.value.find(m => m.key === currentMenu.value)?.label || '\u7cfb\u7edf\u9996\u9875');
        if (userInfo.value.role === 2) {
            menus.value = menus.value.filter(m => ['home','personal','employee','attendance-apply','attendance','performance','salary','salary-feedback','anomaly','notice'].includes(m.key));
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
            if (menu.key === 'tax') {
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

        // --- 棣栭〉鏁版嵁 ---
        const stats = reactive({ employeeCount: 0, salaryCount: 0, paymentCount: 0 });
        const notices = ref([]);
        const loadDashboard = () => {
            axios.get('/api/employee/count').then(r => { if (r.data.code === 200) stats.employeeCount = r.data.data; }).catch(()=>{});
            axios.get('/api/salary/count').then(r => { if (r.data.code === 200) stats.salaryCount = r.data.data; }).catch(()=>{});
            axios.get('/api/salary/payment/count').then(r => { if (r.data.code === 200) stats.paymentCount = r.data.data; }).catch(()=>{});
            setTimeout(initCharts, 200);
        };
        const initCharts = () => {
            const getEl = id => document.getElementById(id);
            if (!getEl('echart-top1')) return;

            // 1. 閮ㄩ棬浜烘暟鍒嗗竷 (鏌辩姸鍥?
            const c1 = echarts.init(getEl('echart-top1'));
            c1.setOption({
                title: { text: '閮ㄩ棬浜烘暟鍒嗗竷', top: 15, left: 'center', textStyle: { fontSize: 16, color: '#333' } },
                tooltip: { trigger: 'axis' },
                xAxis: { type: 'category', data: ['閿€鍞儴','鎶€鏈儴','璐㈠姟閮?,'琛屾斂閮?,'浜у搧閮?] },
                yAxis: { type: 'value' },
                series: [{ type: 'bar', data: [15, 23, 8, 5, 12], itemStyle: { color: '#0956FF', borderRadius: [4,4,0,0] } }],
                grid: { left: 40, right: 30, top: 60, bottom: 30 }
            });

            // 2. 钖祫缁撴瀯鍒嗗竷 (楗煎浘)
            const c2 = echarts.init(getEl('echart-top2'));
            c2.setOption({
                title: { text: '褰撴湀钖祫缁撴瀯鍗犳瘮', top: 15, left: 'center', textStyle: { fontSize: 16, color: '#333' } },
                tooltip: { trigger: 'item' },
                legend: { bottom: 10, left: 'center' },
                series: [{
                    name: '钖祫绫诲瀷', type: 'pie', radius: ['40%', '70%'], center: ['50%', '50%'],
                    data: [
                        { value: 124500, name: '鍩烘湰宸ヨ祫' }, { value: 34000, name: '缁╂晥濂栭噾' },
                        { value: 12000, name: '鍔犵彮琛ヨ创' }, { value: 8500, name: '鍏朵粬琛ュ姪' }
                    ],
                    itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 }
                }]
            });

            // 3. 鍏ㄥ勾钖祫鏍稿彂璧板娍 (鎶樼嚎闈㈢Н鍥?
            const c3 = echarts.init(getEl('echart-mid1'));
            c3.setOption({
                title: { text: '鏈堝疄鍙戝伐璧勮蛋鍔?鍏?', top: 15, left: 'center', textStyle: { fontSize: 16, color: '#333' } },
                tooltip: { trigger: 'axis' },
                xAxis: { type: 'category', boundaryGap: false, data: ['1鏈?,'2鏈?,'3鏈?,'4鏈?,'5鏈?,'6鏈?,'7鏈?,'8鏈?,'9鏈?] },
                yAxis: { type: 'value' },
                series: [{
                    type: 'line', data: [81453, 79000, 83000, 86500, 80000, 89000, 92000, 88500, 91000],
                    smooth: true, itemStyle: { color: '#00D1B2' },
                    areaStyle: {
                        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                            { offset: 0, color: 'rgba(0, 209, 178, 0.4)' }, { offset: 1, color: 'rgba(0, 209, 178, 0.05)' }
                        ])
                    }
                }],
                grid: { left: 60, right: 40, top: 60, bottom: 30 }
            });

            // 4. 鍛樺伐鑰冨嫟鐘舵€佸垎鏋?(鐜舰鍥?
            const c4 = echarts.init(getEl('echart-bot1'));
            c4.setOption({
                title: { text: '褰撴湀鏁翠綋鑰冨嫟鐘舵€?, top: 15, left: 'center', textStyle: { fontSize: 16, color: '#333' } },
                tooltip: { trigger: 'item' },
                series: [{
                    type: 'pie', radius: '65%', center: ['50%', '55%'],
                    data: [ { value: 92, name: '鍏ㄥ嫟姝ｅ父' }, { value: 3, name: '杩熷埌/鏃╅€€' }, { value: 2, name: '鏃峰伐' }, { value: 5, name: '璇峰亣' } ],
                    itemStyle: { borderRadius: 6 }
                }],
                color: ['#0956FF', '#E6A23C', '#F56C6C', '#909399']
            });

            // 5. 鍚勯儴闂ㄤ汉鍧囪柂璧勫姣?(闆疯揪鍥?妯悜鏌卞浘)
            const c5 = echarts.init(getEl('echart-bot2'));
            c5.setOption({
                title: { text: '鍚勯儴闂ㄤ汉鍧囪柂璧?鍏?', top: 15, left: 'center', textStyle: { fontSize: 16, color: '#333' } },
                tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
                xAxis: { type: 'value' },
                yAxis: { type: 'category', data: ['閿€鍞儴','鎶€鏈儴','璐㈠姟閮?,'琛屾斂閮?,'浜у搧閮?] },
                series: [{ type: 'bar', data: [9800, 14200, 8500, 7200, 12500], itemStyle: { color: '#8884d8' }, label: { show: true, position: 'right' } }],
                grid: { left: 80, right: 40, top: 60, bottom: 30 }
            });

            window.addEventListener('resize', () => { c1.resize(); c2.resize(); c3.resize(); c4.resize(); c5.resize(); });
        };

        // --- 涓汉涓績 ---
        const pwdFormRef = ref(null);
        const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' });
        const validatePass2 = (rule, value, callback) => {
            if (value === '') callback(new Error('璇峰啀娆¤緭鍏ュ瘑鐮?));
            else if (value !== pwdForm.newPassword) callback(new Error('涓ゆ杈撳叆瀵嗙爜涓嶄竴鑷?'));
            else callback();
        };
        const pwdRules = reactive({
            oldPassword: [{ required: true, message: '璇疯緭鍏ュ綋鍓嶅瘑鐮?, trigger: 'blur' }],
            newPassword: [
                { required: true, message: '璇疯緭鍏ユ柊瀵嗙爜', trigger: 'blur' },
                { min: 6, message: '瀵嗙爜闀垮害鑷冲皯6浣?, trigger: 'blur' }
            ],
            confirmPassword: [
                { required: true, message: '璇风‘璁ゆ柊瀵嗙爜', trigger: 'blur' },
                { validator: validatePass2, trigger: 'blur' }
            ]
        });
        const updatePassword = () => {
            if(!pwdFormRef.value) return;
            pwdFormRef.value.validate((valid) => {
                if(valid) {
                    axios.put('/api/auth/password', null, { params: { oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword } }).then(r => {
                        if(r.data.code === 200) {
                            ElementPlus.ElMessage.success('瀵嗙爜淇敼鎴愬姛锛岃閲嶆柊鐧诲綍');
                            setTimeout(logout, 1500);
                        } else {
                            ElementPlus.ElMessage.error(r.data.message || '淇敼澶辫触');
                        }
                    });
                }
            });
        };

        // --- 閮ㄩ棬绠＄悊 ---
        const departments = ref([]);
        const deptLoading = ref(false);
        const deptDialogVisible = ref(false);
        const deptForm = reactive({ id: '', deptName: '', description: '' });

        const loadDepartments = () => {
            deptLoading.value = true;
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
            if(!deptForm.deptName) return ElementPlus.ElMessage.warning('閮ㄩ棬鍚嶇О蹇呭～');
            const req = deptForm.id ? axios.put('/api/department', deptForm) : axios.post('/api/department', deptForm);
            req.then(r => {
                if(r.data.code === 200) {
                    ElementPlus.ElMessage.success('淇濆瓨鎴愬姛');
                    deptDialogVisible.value = false;
                    loadDepartments();
                } else ElementPlus.ElMessage.error(r.data.message || '澶辫触');
            });
        };
        const deleteDept = (id) => {
            ElementPlus.ElMessageBox.confirm('纭瑙ｆ暎璇ラ儴闂紵', '璀﹀憡', { type: 'warning' }).then(() => {
                axios.delete(`/api/department/${id}`).then(r => {
                    if(r.data.code === 200) { ElementPlus.ElMessage.success('宸插垹闄?); loadDepartments(); }
                    else ElementPlus.ElMessage.error(r.data.message || '澶辫触');
                });
            }).catch(()=>{});
        };

        // --- 閫氱煡鍏憡绠＄悊 ---
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
            localStorage.setItem('deptManagerEmpNos', JSON.stringify([...new Set(managerEmpNos.value.filter(Boolean))]));
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
            return positionText.includes('缁忕悊')
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
            empForm.positionName = '閮ㄩ棬缁忕悊';
            empForm.position = '閮ㄩ棬缁忕悊';
            empForm.role = 2;
            if (empForm.empNo) rememberManagerEmpNo(empForm.empNo);
            syncEmpDeptSalary(empForm.deptId);
        };
        const deleteDeptManager = (row) => {
            ElementPlus.ElMessageBox.confirm('纭鍒犻櫎璇ラ儴闂ㄧ粡鐞嗗悧锛?, '鎻愮ず', { type: 'warning' }).then(() => {
                axios.delete('/api/employee/' + row.id).then(r => {
                    if (r.data.code === 200) {
                        forgetManagerEmpNo(row.empNo);
                        rememberEmployeeAvatar(row.empNo, '');
                        ElementPlus.ElMessage.success('鍒犻櫎鎴愬姛');
                        loadDeptManagers();
                        loadEmployees(1);
                    } else {
                        ElementPlus.ElMessage.error(r.data.message || '鍒犻櫎澶辫触');
                    }
                });
            }).catch(() => {});
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
                if(r.data.code === 200) {
                    noticeList.value = r.data.data?.records || r.data.data || [];
                    noticeTotal.value = r.data.data?.total || 0;
                }
            }).finally(()=> noticeLoading.value = false);
        };
        const openNoticeForm = (row) => {
            noticeDialogVisible.value = true;
            if(row) Object.assign(noticeForm, row);
            else Object.assign(noticeForm, { id:'', title:'', content:'' });
        };
        const saveNotice = () => {
            if(!noticeForm.title || !noticeForm.content) return ElementPlus.ElMessage.warning('鍐呭涓嶈兘涓虹┖');
            const req = noticeForm.id ? axios.put('/api/announcement', noticeForm) : axios.post('/api/announcement', noticeForm);
            req.then(r => {
                if(r.data.code === 200) { ElementPlus.ElMessage.success('鍙戝竷鎴愬姛'); noticeDialogVisible.value=false; loadNotices(); loadDashboard(); }
                else ElementPlus.ElMessage.error(r.data.message||'澶辫触');
            });
        };
        const deleteNotice = (id) => {
            ElementPlus.ElMessageBox.confirm('纭鎾ゅ洖鍒犻櫎璇ュ叕鍛婏紵', '鎻愮ず').then(()=>{
                axios.delete('/api/announcement/'+id).then(r=>{ if(r.data.code===200){ ElementPlus.ElMessage.success('宸插垹闄?); loadNotices(); loadDashboard(); } });
            }).catch(()=>{});
        };

        // --- 鑰冨嫟鏁版嵁绠＄悊 ---
        const attendances = ref([]);
        const attendTotal = ref(0);
        const attendCurrent = ref(1);
        const attendLoading = ref(false);
        const attendSearch = reactive({ empNo: '', empName: '', deptId: '' });
        const attendImportMonth = ref(new Date().getFullYear() + '-' + String(new Date().getMonth() + 1).padStart(2, '0'));
        const attendDialogVisible = ref(false);
        const attendForm = reactive({ id:'', yearMonth:'', empId:'', attendDays:0, absentDays:0, lateTimes:0, leaveDays:0, businessTripTime:'', registerDate:'', remark:'' });
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
            const matchDept = !attendSearch.deptId || String(row.deptId || '') === String(attendSearch.deptId);
            return matchEmpNo && matchEmpName && matchDept;
        }));

        const loadAllEmployeesForSelect = () => {
            if(allEmployees.value.length > 0) return;
            axios.get('/api/employee/page?size=1000').then(r => {
                if(r.data.code===200) allEmployees.value = r.data.data?.records || [];
            });
        };
        const loadAttendances = (page = attendCurrent.value) => {
            attendCurrent.value = page;
            attendLoading.value = true;
            axios.get('/api/attendance/page', { params: { current: page, size: 10, empNo: attendSearch.empNo, empName: attendSearch.empName, realName: attendSearch.empName, deptId: attendSearch.deptId } }).then(r => {
                if(r.data.code === 200) {
                    attendances.value = r.data.data?.records || r.data.data || [];
                    attendTotal.value = r.data.data?.total || 0;
                }
            }).finally(()=> attendLoading.value = false);
        };
        const openAttendForm = (row) => {
            attendDialogVisible.value = true;
            if(row) Object.assign(attendForm, {
                ...row,
                businessTripTime: row.businessTripTime || row.remark || '',
                registerDate: (row.registerDate || row.createTime || '').toString().slice(0, 10)
            });
            else {
                const now = new Date();
                const ym = now.getFullYear() + '-' + String(now.getMonth()+1).padStart(2,'0');
                const dateText = now.toISOString().slice(0, 10);
                Object.assign(attendForm, { id:'', yearMonth:ym, empId:'', attendDays:22, absentDays:0, lateTimes:0, leaveDays:0, businessTripTime:'', registerDate:dateText, remark:'' });
            }
        };
        const saveAttend = () => {
            if(!attendForm.empId || !attendForm.yearMonth) return ElementPlus.ElMessage.warning('鏈堜唤鍜屽憳宸ュ繀閫?);
            const payload = {
                ...attendForm,
                remark: attendForm.businessTripTime || attendForm.remark || '',
                registerDate: attendForm.registerDate || new Date().toISOString().slice(0, 10)
            };
            const req = attendForm.id ? axios.put('/api/attendance', payload) : axios.post('/api/attendance', payload);
            req.then(r => {
                if(r.data.code === 200) { ElementPlus.ElMessage.success('淇濆瓨鎴愬姛'); attendDialogVisible.value=false; loadAttendances(); }
                else ElementPlus.ElMessage.error(r.data.message||'澶辫触');
            });
        };
        const deleteAttend = (id) => {
            ElementPlus.ElMessageBox.confirm('纭鍒犻櫎璇ユ潯鑰冨嫟璁板綍锛?, '鎻愮ず', {type:'warning'}).then(()=>{
                axios.delete('/api/attendance/'+id).then(r=>{ 
                    if(r.data.code===200) { ElementPlus.ElMessage.success('宸插垹闄?); loadAttendances(); }
                    else ElementPlus.ElMessage.error(r.data.message);
                });
            }).catch(()=>{});
        };
        const triggerAttendImport = () => {
            if(!attendImportMonth.value) return ElementPlus.ElMessage.warning('瀵煎叆鑰冨嫟鍓嶏紝璇峰厛閫夋嫨瀵煎叆鏈堜唤');
            attendImportInput.value.click();
        };
        const handleAttendImport = (e) => {
            const file = e.target.files[0];
            if(!file) return;
            const fd = new FormData();
            fd.append('file', file);
            fd.append('yearMonth', attendImportMonth.value);
            const loadInstance = ElementPlus.ElLoading.service({ text: '姝ｅ湪瑙ｆ瀽鑰冨嫟鏈烘暟鎹苟璁＄畻鑰冨嫟...' });
            axios.post('/api/attendance/import/clock', fd, {headers: {'Content-Type': 'multipart/form-data'}}).then(r => {
                if(r.data.code === 200) { ElementPlus.ElMessage.success('瀵煎叆缁撶畻瀹屾垚锛?); loadAttendances(1); }
                else ElementPlus.ElMessage.error(r.data.message || '瀵煎叆澶辫触');
            }).catch(err => ElementPlus.ElMessage.error(err.response?.data?.message || '缃戠粶閿欒'))
              .finally(() => { loadInstance.close(); e.target.value = ''; });
        };
        const showAttendDetail = (row) => {
            ElementPlus.ElMessageBox.alert(`
                鐧昏缂栧彿锛?{row.recordNo || '-'}<br>
                鏈堜唤锛?{row.yearMonth || '-'}<br>
                宸ュ彿锛?{row.empNo || '-'}<br>
                濮撳悕锛?{row.empName || '-'}<br>
                閮ㄩ棬锛?{row.deptName || '-'}<br>
                鍑哄嫟澶╂暟锛?{row.attendDays || 0}<br>
                鏃峰伐澶╂暟锛?{row.absentDays || 0}<br>
                杩熷埌澶╂暟锛?{row.lateTimes || 0}<br>
                璇峰亣澶╂暟锛?{row.leaveDays || 0}<br>
                鍑哄樊鏃堕棿锛?{row.businessTripTime || '-'}<br>
                鐧昏鏃ユ湡锛?{row.registerDate || '-'}<br>
                缁忕悊璐﹀彿锛?{row.managerNo || '-'}<br>
                缁忕悊濮撳悕锛?{row.managerName || '-'}
            `, '鑰冨嫟鏁版嵁璇︽儏', { dangerouslyUseHTMLString: true });
        };

        // --- 鑰冨嫟瀹℃壒 ---
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
                if(r.data.code===200) { applies.value = r.data.data?.records||[]; applyTotal.value = r.data.data?.total||0; }
            }).finally(()=> applyLoading.value=false);
        };
        const reviewApply = (row, status) => {
            ElementPlus.ElMessageBox.prompt('璇疯緭鍏ュ鎵规剰瑙?閫夊～)', status===1?'瀹℃壒鍚屾剰':'椹冲洖鐢宠').then(({value}) => {
                axios.put(`/api/attendance-apply/${row.id}/review?status=${status}&comment=${value||''}`).then(r=>{
                    if(r.data.code===200) { ElementPlus.ElMessage.success('瀹℃壒瀹屾垚'); loadApplies(); }
                });
            }).catch(()=>{});
        };
        const showApplyDetail = (row) => {
            ElementPlus.ElMessageBox.alert(`
                宸ュ彿锛?{row.empNo || '-'}<br>
                濮撳悕锛?{row.empName || '-'}<br>
                閮ㄩ棬锛?{row.deptName || '-'}<br>
                鐢宠绫诲瀷锛?{['','琛ョ','閿€鍋?,'鍔犵彮','寮傚父寮傝'][row.applyType] || '-'}<br>
                鐢宠鏃堕棿锛?{row.createTime || '-'}<br>
                鐢宠浜嬬敱锛?{row.reason || '-'}<br>
                缁忕悊璐﹀彿锛?{row.managerNo || '-'}<br>
                缁忕悊濮撳悕锛?{row.managerName || '-'}<br>
                瀹℃牳鍥炲锛?{row.reviewComment || '-'}
            `, '鑰冨嫟鐢宠璇︽儏', { dangerouslyUseHTMLString: true });
        };
        const deleteApply = (row) => {
            if (!row.id) return ElementPlus.ElMessage.warning('褰撳墠璁板綍缂哄皯涓婚敭锛屾棤娉曞垹闄?);
            ElementPlus.ElMessageBox.confirm('纭鍒犻櫎璇ユ潯鑰冨嫟鐢宠鍚楋紵', '鎻愮ず', { type: 'warning' }).then(() => {
                axios.delete('/api/attendance-apply/' + row.id).then(r => {
                    if (r.data.code === 200) {
                        ElementPlus.ElMessage.success('鍒犻櫎鎴愬姛');
                        loadApplies(1);
                    } else {
                        ElementPlus.ElMessage.error(r.data.message || '鍒犻櫎澶辫触');
                    }
                }).catch(() => {
                    ElementPlus.ElMessage.warning('褰撳墠鍚庡彴鏆傛湭寮€鏀捐€冨嫟鐢宠鍒犻櫎鎺ュ彛');
                });
            }).catch(() => {});
        };

        // --- 寮傚父澶勭悊 ---
        const anomalies = ref([]);
        const anomalyTotal = ref(0);
        const anomalyCurrent = ref(1);
        const anomalyLoading = ref(false);
        const anomalySearch = reactive({ status: '' });
        const anomalyDialogVisible = ref(false);
        const anomalyForm = reactive({ id:'', status:1, processResult:'' });
        const loadAnomalies = (page = anomalyCurrent.value) => {
            anomalyCurrent.value = page;
            anomalyLoading.value = true;
            axios.get('/api/anomaly/page', { params: { current: page, size: 10, status: anomalySearch.status } }).then(r => {
                if(r.data.code===200) { anomalies.value = r.data.data?.records||[]; anomalyTotal.value = r.data.data?.total||0; }
            }).finally(()=> anomalyLoading.value=false);
        };
        const openProcessAnomaly = (row) => {
            anomalyDialogVisible.value = true;
            anomalyForm.id = row.id;
            anomalyForm.status = row.status === 0 ? 1 : row.status;
            anomalyForm.processResult = row.processResult || '';
        };
        const saveAnomaly = () => {
            if(!anomalyForm.processResult) return ElementPlus.ElMessage.warning('璇峰～鍐欏鐞嗚繘灞?);
            axios.put(`/api/anomaly/${anomalyForm.id}/process?status=${anomalyForm.status}&processResult=${anomalyForm.processResult}`).then(r=>{
                if(r.data.code===200) { ElementPlus.ElMessage.success('璁板綍瀹屾瘯'); anomalyDialogVisible.value=false; loadAnomalies(); }
            });
        };

        // --- 鎶曡瘔鍙嶉澶勭悊 ---
        const feedbacks = ref([]);
        const feedbackTotal = ref(0);
        const feedbackCurrent = ref(1);
        const feedbackLoading = ref(false);
        const feedbackSearch = reactive({ status: '' });
        const feedbackDialogVisible = ref(false);
        const feedbackForm = reactive({ id:'', status:1, replyContent:'' });
        const loadFeedbacks = (page = feedbackCurrent.value) => {
            feedbackCurrent.value = page;
            feedbackLoading.value = true;
            axios.get('/api/feedback/page', { params: { current: page, size: 10, status: feedbackSearch.status } }).then(r => {
                if(r.data.code===200) { feedbacks.value = r.data.data?.records||[]; feedbackTotal.value = r.data.data?.total||0; }
            }).finally(()=> feedbackLoading.value=false);
        };
        const openReplyFeedback = (row) => {
            feedbackDialogVisible.value = true;
            feedbackForm.id = row.id;
            feedbackForm.status = row.status === 0 ? 1 : row.status;
            feedbackForm.replyContent = row.replyContent || '';
        };
        const saveFeedback = () => {
            if(!feedbackForm.replyContent) return ElementPlus.ElMessage.warning('璇峰～鍐欏洖澶嶈鏄?);
            axios.put(`/api/feedback/${feedbackForm.id}/reply?status=${feedbackForm.status}&replyContent=${feedbackForm.replyContent}`).then(r=>{
                if(r.data.code===200) { ElementPlus.ElMessage.success('鍥炲瀹屾瘯'); feedbackDialogVisible.value=false; loadFeedbacks(); }
            });
        };

        // --- 绀句繚閰嶇疆 ---
        const socConfigs = ref([]);
        const socTotal = ref(0);
        const socCurrent = ref(1);
        const socLoading = ref(false);
        const socSearch = reactive({ configName: '' });
        const socDialogVisible = ref(false);
        const socForm = reactive({ id:'', configName:'', minBase:0, maxBase:0, pensionRatio:8, medicalRatio:2, unemploymentRatio:0.5, housingFundRatio:7, isActive:0 });
        const loadSocConfigs = (page = socCurrent.value) => {
            socCurrent.value = page;
            socLoading.value = true;
            axios.get('/api/social-config/page', { params: { current: page, size: 10, configName: socSearch.configName } }).then(r => {
                if(r.data.code===200) { socConfigs.value = r.data.data?.records||[]; socTotal.value = r.data.data?.total||0; }
            }).finally(()=>socLoading.value=false);
        };
        const openSocForm = (row) => {
            socDialogVisible.value = true;
            if(row) Object.assign(socForm, row);
            else Object.assign(socForm, { id:'', configName:'', minBase:3000, maxBase:30000, pensionRatio:8, medicalRatio:2, unemploymentRatio:0.5, housingFundRatio:7, isActive:0 });
        };
        const saveSocConfig = () => {
            const req = socForm.id ? axios.put('/api/social-config', socForm) : axios.post('/api/social-config', socForm);
            req.then(r=>{
                if(r.data.code===200) { ElementPlus.ElMessage.success('淇濆瓨鎴愬姛'); socDialogVisible.value=false; loadSocConfigs(); }
            });
        };

        // --- 宸ヨ祫涓◣涓庣ぞ淇?---
        const taxRecords = ref([]);
        const taxTotal = ref(0);
        const taxCurrent = ref(1);
        const taxLoading = ref(false);
        const taxSearch = reactive({ empName: '', deptId: '' });
        const taxDialogVisible = ref(false);
        const taxForm = reactive({ id:'', empName:'', deptName:'', housingFund:0, medicalInsurance:0, pensionInsurance:0, socialSecurityEmp:0, incomeTax:0, deductDate:'', managerNo:'', managerName:'' });
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
                if(r.data.code===200) {
                    taxRecords.value = r.data.data?.records || [];
                    taxTotal.value = r.data.data?.total || 0;
                }
            }).finally(() => taxLoading.value = false);
        };
        const openTaxDetail = (row) => {
            ElementPlus.ElMessageBox.alert(`
                鍛樺伐濮撳悕锛?{row.empName || '-'}<br>
                閮ㄩ棬锛?{row.deptName || '-'}<br>
                鎵ｉ櫎鍏Н閲戯細${row.housingFund || 0}<br>
                鍖荤枟淇濋櫓锛?{row.medicalInsurance || 0}<br>
                鍏昏€佷繚闄╋細${row.pensionInsurance || 0}<br>
                鎵ｉ櫎绀句繚锛?{row.socialSecurityEmp || 0}<br>
                鎵ｉ櫎涓◣锛?{row.incomeTax || 0}<br>
                鎵ｉ櫎鏃ユ湡锛?{row.deductDate || '-'}<br>
                缁忕悊璐﹀彿锛?{row.managerNo || '-'}<br>
                缁忕悊濮撳悕锛?{row.managerName || '-'}
            `, '宸ヨ祫涓◣涓庣ぞ淇濊鎯?, { dangerouslyUseHTMLString: true });
        };
        const openTaxForm = (row) => {
            Object.assign(taxForm, row || { id:'', empName:'', deptName:'', housingFund:0, medicalInsurance:0, pensionInsurance:0, socialSecurityEmp:0, incomeTax:0, deductDate:'', managerNo:'', managerName:'' });
            taxDialogVisible.value = true;
        };
        const saveTaxRecord = () => {
            taxDialogVisible.value = false;
            ElementPlus.ElMessage.warning('褰撳墠鍚庡彴鏆傛湭寮€鏀惧崟鏉″伐璧勪釜绋庝笌绀句繚淇敼鎺ュ彛锛屽凡鍏堝畬鎴愰〉闈㈠瓧娈典笌寮圭獥瀵归綈');
        };
        const deleteTaxRecord = () => {
            ElementPlus.ElMessage.warning('褰撳墠鍚庡彴鏆傛湭寮€鏀惧崟鏉″伐璧勪釜绋庝笌绀句繚鍒犻櫎鎺ュ彛');
        };

        // --- 缁╂晥鑰冩牳 ---
        const performances = ref([]);
        const perfTotal = ref(0);
        const perfCurrent = ref(1);
        const perfLoading = ref(false);
        const perfSearch = reactive({ empName: '', deptId: '', managerName: '' });
        const perfDialogVisible = ref(false);
        const perfForm = reactive({ id:'', yearMonth:'', empId:'', attendanceScore:20, attitudeScore:20, skillScore:20, performanceScore:20, bonusPenaltyScore:0, score:80, evalComment:'' });
        const performanceDisplayRows = computed(() => performances.value.map(row => ({
            ...row,
            empName: row.empName || row.realName || '-',
            deptName: row.deptName || '-',
            deptId: row.deptId || '',
            attendanceScore: row.attendanceScore ?? '-',
            attitudeScore: row.attitudeScore ?? '-',
            skillScore: row.skillScore ?? '-',
            performanceScore: row.performanceScore ?? '-',
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
            if (score >= 90) return '浼樼';
            if (score >= 80) return '鑹ソ';
            if (score >= 70) return '涓瓑';
            if (score >= 60) return '鍙婃牸';
            return '寰呮彁鍗?;
        };
        const loadPerformances = (page = perfCurrent.value) => {
            perfCurrent.value = page;
            perfLoading.value = true;
            axios.get('/api/performance/page', { params: { current: page, size: 10, empName: perfSearch.empName, realName: perfSearch.empName, deptId: perfSearch.deptId, managerName: perfSearch.managerName } }).then(r => {
                if(r.data.code===200) { performances.value = r.data.data?.records||[]; perfTotal.value = r.data.data?.total||0; }
            }).finally(()=>perfLoading.value=false);
        };
        const openPerfForm = (row) => {
            perfDialogVisible.value = true;
            if(row) Object.assign(perfForm, row);
            else {
                const now = new Date();
                Object.assign(perfForm, { id:'', yearMonth: now.getFullYear() + '-' + String(now.getMonth()+1).padStart(2,'0'), empId:'', attendanceScore:20, attitudeScore:20, skillScore:20, performanceScore:20, bonusPenaltyScore:0, score:80, evalComment:'' });
            }
        };
        const savePerformance = () => {
            if(!perfForm.empId || !perfForm.yearMonth) return ElementPlus.ElMessage.warning('鍛樺伐涓庢湀浠藉繀濉?);
            const totalScore = calcPerfTotal(perfForm);
            const params = {
                id: perfForm.id || undefined,
                empId: perfForm.empId,
                yearMonth: perfForm.yearMonth,
                score: totalScore,
                evalComment: perfForm.evalComment
            };
            const req = params.id ? axios.put('/api/performance', params) : axios.post('/api/performance', params);
            req.then(r=>{
                if(r.data.code===200) { ElementPlus.ElMessage.success(params.id ? '淇敼鎴愬姛' : '褰曞叆鎴愬姛'); perfDialogVisible.value=false; loadPerformances(); }
                else ElementPlus.ElMessage.error(r.data.message||'淇濆瓨澶辫触');
            });
        };
        const openPerformanceDetail = (row) => {
            ElementPlus.ElMessageBox.alert(`
                濮撳悕锛?{row.empName || '-'}<br>
                閮ㄩ棬锛?{row.deptName || '-'}<br>
                缁╂晥鏈堜唤锛?{row.yearMonth || '-'}<br>
                鍛樺伐鑰冨嫟锛?{row.attendanceScore ?? '-'}<br>
                宸ヤ綔鎬佸害锛?{row.attitudeScore ?? '-'}<br>
                涓氬姟鎶€鑳斤細${row.skillScore ?? '-'}<br>
                宸ヤ綔缁╂晥锛?{row.performanceScore ?? '-'}<br>
                濂栨儵鍔犲噺鍒嗭細${row.bonusPenaltyScore ?? 0}<br>
                鎬诲緱鍒嗭細${row.score ?? 0}<br>
                璇勪环绛夌骇锛?{row.grade || calcPerfGrade(row.score || 0)}<br>
                缁忕悊璐﹀彿锛?{row.managerNo || '-'}<br>
                缁忕悊濮撳悕锛?{row.managerName || '-'}
            `, '缁╂晥璇勫垎璇︽儏', { dangerouslyUseHTMLString: true });
        };
        const deletePerformance = (row) => {
            if (!row.id) return ElementPlus.ElMessage.warning('褰撳墠璁板綍缂哄皯涓婚敭锛屾棤娉曞垹闄?);
            ElementPlus.ElMessageBox.confirm('纭鍒犻櫎璇ユ潯缁╂晥璇勫垎璁板綍鍚楋紵', '鎻愮ず', { type: 'warning' }).then(() => {
                axios.delete('/api/performance/' + row.id).then(r => {
                    if (r.data.code === 200) {
                        ElementPlus.ElMessage.success('鍒犻櫎鎴愬姛');
                        loadPerformances(1);
                    } else {
                        ElementPlus.ElMessage.error(r.data.message || '鍒犻櫎澶辫触');
                    }
                }).catch(() => {
                    ElementPlus.ElMessage.warning('褰撳墠鍚庡彴鏆傛湭寮€鏀剧哗鏁堣瘎鍒嗗垹闄ゆ帴鍙?);
                });
            }).catch(() => {});
        };

        // --- 钖祫璁＄畻 ---
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
            allowance: row.allowance ?? 0,
            deductAmount: Number(row.socialSecurityEmp || 0) + Number(row.incomeTax || 0) + Number(row.attendDeduct || 0),
            netSalary: row.netSalary ?? 0,
            recordDate: (row.recordDate || row.createTime || '').toString().slice(0, 10) || '-'
        })));
        
        const loadSalaries = (page = salaryCurrent.value) => {
            salaryCurrent.value = page;
            salaryLoading.value = true;
            axios.get('/api/salary/page', { params: { current: page, size: 10, yearMonth: salarySearch.yearMonth, empNo: salarySearch.empNo, empName: salarySearch.empName, realName: salarySearch.empName, deptId: salarySearch.deptId } }).then(r => {
                if(r.data.code===200) { salaries.value = r.data.data?.records||[]; salaryTotal.value = r.data.data?.total||0; }
            }).finally(()=>salaryLoading.value=false);
        };
        const handleSalarySelection = (val) => { salarySelections.value = val; };
        const saveAllowance = (row) => {
            const payload = { ...row, allowance: Number(row.allowance || 0) };
            axios.put('/api/salary/manual-update', payload).then(r => {
                if (r.data.code === 200) {
                    ElementPlus.ElMessage.success(`宸蹭繚瀛?${row.empName} 鐨勬触璐磋皟鏁碻);
                    loadSalaries();
                } else {
                    ElementPlus.ElMessage.error(r.data.message || '淇濆瓨澶辫触');
                }
            });
        };
        const batchCalcSalary = () => {
            if(!salarySearch.yearMonth) return ElementPlus.ElMessage.warning('璇峰厛鍦ㄦ悳绱㈡爮閫夋嫨闇€瑕佺畻钖殑璐︽湡(鏈堜唤)锛?);
            const loadInstance = ElementPlus.ElLoading.service({ text: '寮曟搸鎷煎懡璁＄畻涓?..' });
            axios.post('/api/salary/batch-calculate?yearMonth=' + salarySearch.yearMonth).then(r=>{
                if(r.data.code===200) { ElementPlus.ElMessage.success('鎵归噺绠楄柂缁撴潫锛屽叡鐢熸垚鏈湀璐﹀崟璁板綍'); loadSalaries(); }
                else ElementPlus.ElMessage.error(r.data.message||'绠楄柂澶辫触');
            }).finally(()=>loadInstance.close());
        };
        const batchPublishSalary = () => {
            let ids = salarySelections.value.map(s => s.id);
            if(ids.length === 0) {
                ids = salaries.value.filter(s => s.calcStatus === 1).map(s => s.id);
            }
            if(ids.length === 0) return ElementPlus.ElMessage.warning('褰撳墠璐﹀崟涓病鏈夊彲鍙戝竷鐨勮崏绋胯褰?);
            axios.put('/api/salary/batch-publish', ids).then(r=>{
                if(r.data.code===200) { ElementPlus.ElMessage.success('鎵归噺鍏紑瀹屾垚锛屽憳宸ュ凡鑳芥敹鍒拌处鍗?); loadSalaries(); }
            });
        };
        const openSalaryDetail = (row) => {
            ElementPlus.ElMessageBox.alert(
                `瀹炲彂宸ヨ祫 = 鍩烘湰宸ヨ祫 + 娲ヨ创 + 缁╂晥 + 鍔犵彮濂栧姳 - 绀句繚 - 鑰冨嫟鎵ｆ - 涓◣
                <hr>
                鍩烘湰宸ヨ祫: ${row.baseSalary}<br>
                娲ヨ创: ${row.allowance}<br>
                缁╂晥: ${row.perfBonus}<br>
                鍔犵彮濂栧姳: ${row.overtimePay || 0}<br>
                绀句繚鎵ｉ櫎: ${row.socialSecurityEmp}<br>
                鑰冨嫟鎵ｆ: ${row.attendDeduct}<br>
                涓◣鎵ｉ櫎: ${row.incomeTax}<br>
                鍙戣柂璐︽埛: ${row.bankAccount || '鏈淮鎶?}`,
                `鍛樺伐 ${row.empName} 鐨勮柂璧勬牳绠楁槑缁哷,
                {dangerouslyUseHTMLString: true}
            );
        };
        const deleteSalaryRecord = (row) => {
            ElementPlus.ElMessage.warning('褰撳墠鍚庡彴鏆傛湭寮€鏀捐柂璧勬牳绠楀崟鏉″垹闄ゆ帴鍙?);
        };

        // --- 钖祫鏀粯 ---
        const payments = ref([]);
        const paymentTotal = ref(0);
        const paymentCurrent = ref(1);
        const paymentLoading = ref(false);
        const paymentSearch = reactive({ yearMonth: '', empNo: '', empName: '', deptId: '', calcStatus: '' });
        const paymentDisplayRows = computed(() => payments.value.map(row => ({
            ...row,
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
        
        const loadPayments = (page = paymentCurrent.value) => {
            paymentCurrent.value = page;
            paymentLoading.value = true;
            axios.get('/api/salary/page', { params: { current: page, size: 10, yearMonth: paymentSearch.yearMonth, empNo: paymentSearch.empNo, empName: paymentSearch.empName, realName: paymentSearch.empName, deptId: paymentSearch.deptId, calcStatus: paymentSearch.calcStatus } }).then(r => {
                if(r.data.code===200) { payments.value = r.data.data?.records||[]; paymentTotal.value = r.data.data?.total||0; }
            }).finally(()=>paymentLoading.value=false);
        };
        const executePayment = (row) => {
            if(row.calcStatus < 2) return ElementPlus.ElMessage.warning('璇ヨ处鍗曟湭鍏紑瀹氳〃锛屼笉鍙墦娆?);
            ElementPlus.ElMessageBox.confirm(`鍑嗗鍚戣处鎴?${row.bankAccount||'銆愭湭缁戝畾瀵瑰叕璐︽埛銆?} 姹囨 楼${row.netSalary} 鍏冿紝鏄惁杩為€氶摱琛屾帴鍙ｆ墽琛岋紵`, '瀹夊叏鏀粯缃戝叧鎺ュ叆涓?..').then(()=>{
                const loadInstance = ElementPlus.ElLoading.service({ text: '姝ｅ湪楠岃瘉缃戦摱瀵嗛挜涓庨€氳...' });
                setTimeout(()=>{
                    axios.put('/api/salary/' + row.id + '/pay').then(r=>{
                        if(r.data.code===200) { ElementPlus.ElMessage.success('鏀粯鎴愬姛锛岃祫閲戝凡姹囧嚭锛?); loadPayments(); }
                        else ElementPlus.ElMessage.error(r.data.message||'鎵撴澶辫触');
                    }).finally(()=>loadInstance.close());
                }, 1500);
            }).catch(()=>{});
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
        const empForm = reactive({ id:'', empNo:'', realName:'', gender:1, phone:'', idCard:'', deptId:'', baseSalary:0, hireDate:'', initialPassword:'', avatar:'', role:'', position:'', positionName:'' });
        const importInput = ref(null);
        const avatarInput = ref(null);
        const avatarUploading = ref(false);
        const empSelections = ref([]);
        const employeeDisplayRows = computed(() => employees.value
            .filter(row => !isManagerRecord(row))
            .map(row => ({ ...row, avatar: row.avatar || getStoredAvatar(row.empNo) || '' })));
        const empDialogTitle = computed(() => {
            if (empDialogMode.value === 'manager') return empForm.id ? '淇敼閮ㄩ棬缁忕悊' : '鏂板閮ㄩ棬缁忕悊';
            return empForm.id ? '淇敼鍛樺伐' : '鏂板鍛樺伐';
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
            Object.assign(empSearch, { empNo:'', realName:'', deptId:'' });
            empCurrent.value = 1;
            loadEmployees(1);
        };
        const handleEmpSelection = (rows) => {
            empSelections.value = rows || [];
        };
        const openEmpForm = (row, mode = 'employee') => {
            empDialogMode.value = mode;
            empDialogVisible.value = true;
            if(row) {
                Object.assign(empForm, { role:'', position:'', positionName:'', initialPassword:'', avatar:'' }, row);
                empForm.avatar = row.avatar || getStoredAvatar(row.empNo) || '';
                syncEmpDeptSalary(row.deptId);
            } else {
                Object.assign(empForm, { id:'', empNo:'', realName:'', gender:1, phone:'', idCard:'', deptId:'', baseSalary:null, hireDate:'', initialPassword:'', avatar:'', role: mode === 'manager' ? 2 : '', position: mode === 'manager' ? '閮ㄩ棬缁忕悊' : '', positionName: mode === 'manager' ? '閮ㄩ棬缁忕悊' : '' });
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
                return;
            }
            avatarUploading.value = true;
            const formData = new FormData();
            formData.append('file', file);
            axios.post('/api/employee/upload-avatar', formData, {
                headers: { 'Content-Type': 'multipart/form-data' }
            }).then(r => {
                if (r.data.code === 200 && r.data.data) {
                    empForm.avatar = r.data.data;
                    ElementPlus.ElMessage.success('头像上传成功');
                    avatarUploading.value = false;
                    event.target.value = '';
                } else {
                    fallbackLocalAvatar(file, event);
                }
            }).catch(() => {
                fallbackLocalAvatar(file, event);
            });
        };
        const saveEmployee = () => {
            if(!empForm.empNo || !empForm.realName || !empForm.deptId) return ElementPlus.ElMessage.warning('\u8bf7\u586b\u5199\u5fc5\u586b\u9879');
            const params = { ...empForm };
            if(!empForm.id && empForm.initialPassword) params.initialPassword = empForm.initialPassword;
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
                if(r.data.code === 200) {
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
                    if(r.data.code === 200) {
                        rememberEmployeeAvatar(row?.empNo, '');
                        ElementPlus.ElMessage.success('\u5220\u9664\u6210\u529f');
                        loadEmployees();
                    } else ElementPlus.ElMessage.error(r.data.message || '\u64cd\u4f5c\u5931\u8d25');
                });
            }).catch(()=>{});
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
            }).catch(()=>{});
        };
        const triggerImport = () => importInput.value.click();
        const handleImportFile = (e) => {
            const file = e.target.files[0];
            if(!file) return;
            const fd = new FormData();
            fd.append('file', file);
            const loadInstance = ElementPlus.ElLoading.service({ text: '姝ｅ湪瑙ｆ瀽Excel瀵煎叆...' });
            axios.post('/api/employee/import', fd).then(r => {
                if(r.data.code === 200) { ElementPlus.ElMessage.success('瀵煎叆鎴愬姛'); loadEmployees(1); }
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
            }).catch(() => {});
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

