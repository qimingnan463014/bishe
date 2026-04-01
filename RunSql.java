import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class RunSql {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/bishe?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false";
        String user = "root";
        String password = "123456";
        String sqlFile = "salary-system/reset_data.sql";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            
            BufferedReader reader = new BufferedReader(new FileReader(sqlFile));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("--") || line.trim().isEmpty()) continue;
                sb.append(line);
                if (line.trim().endsWith(";")) {
                    String sql = sb.toString();
                    System.out.println("Executing: " + sql.substring(0, Math.min(sql.length(), 50)) + "...");
                    stmt.execute(sql);
                    sb.setLength(0);
                }
            }
            System.out.println("SQL execution completed successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
