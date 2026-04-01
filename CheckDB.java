import java.sql.*;

public class CheckDB {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/bishe?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false";
        String user = "root";
        String password = "123456";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("Connected to the database!");
            
            System.out.println("\n--- Users ---");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, username, role FROM user")) {
                while (rs.next()) {
                    System.out.printf("ID: %d, Username: %s, Role: %d%n", rs.getInt("id"), rs.getString("username"), rs.getInt("role"));
                }
            }

            System.out.println("\n--- Employee Count ---");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM employee")) {
                if (rs.next()) {
                    System.out.println("Total Employees: " + rs.getInt(1));
                }
            }
            
            System.out.println("\n--- Department Count ---");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM department")) {
                if (rs.next()) {
                    System.out.println("Total Departments: " + rs.getInt(1));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
