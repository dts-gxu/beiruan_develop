import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestDatabaseConnection {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/ruoyi?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8";
        String username = "root";
        String password = "root";
        
        try {
            // 加载MySQL驱动
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("✓ MySQL驱动加载成功");
            
            // 建立连接
            Connection conn = DriverManager.getConnection(url, username, password);
            System.out.println("✓ 数据库连接成功！");
            
            // 测试查询
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM sys_user");
            
            if (rs.next()) {
                int count = rs.getInt("count");
                System.out.println("✓ sys_user表中有 " + count + " 条用户记录");
            }
            
            // 查询用户信息
            rs = stmt.executeQuery("SELECT user_id, login_name, user_name FROM sys_user");
            System.out.println("\n用户信息：");
            while (rs.next()) {
                System.out.println("  - ID: " + rs.getLong("user_id") + 
                                 ", 账号: " + rs.getString("login_name") + 
                                 ", 昵称: " + rs.getString("user_name"));
            }
            
            // 关闭资源
            rs.close();
            stmt.close();
            conn.close();
            System.out.println("\n✓ 所有测试通过！数据库连接正常");
            
        } catch (ClassNotFoundException e) {
            System.out.println("✗ MySQL驱动未找到: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ 数据库连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
