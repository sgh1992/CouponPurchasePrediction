package dataBase;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Created by sghipr on 2016/2/22.
 * 注意理解管理Connection,Statement,ResultSet这三者之间的关系
 * 每个Connection下可以有多个Statement；每个Statement可以执行多条不同的sql命令,但是每个statement中只能有一个活动的ResultSet结果集.
 * 每次用完Connection或者Statement,ResultSet之后都需要close，因为connection,statement,resultSet等数据结构占用了很大的内存空间.
 */
public class DB {
    public static Statement getStatement(){
        Connection conn = getConnection();
        Statement stat = null;
        try {
            stat = conn.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stat;
    }
    public static Connection getConnection(){
        Properties properties = new Properties();
        try {
            FileInputStream inputStream = new FileInputStream("db.properties");//将这个属性配置文件固定下来.
            properties.load(inputStream);
        } catch (FileNotFoundException e) {
           System.err.println("This db.properties not found!");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String userName = properties.getProperty("user");
        String password = properties.getProperty("password");
        String url = properties.getProperty("url");
        String driver = properties.getProperty("driver");
        Connection connection = null;
        try {
            Class.forName(driver);
            connection = DriverManager.getConnection(url,userName,password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }
}
