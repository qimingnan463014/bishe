-- 测试查询：从数据库验证经理数据
SELECT e.id, e.emp_no, e.real_name, u.username, u.role, e.dept_id, e.user_id
FROM t_employee e
LEFT JOIN t_user u ON e.user_id = u.id
WHERE u.role = 2
ORDER BY e.id;
