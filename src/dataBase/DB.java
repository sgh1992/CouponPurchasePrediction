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
 * ע��������Connection,Statement,ResultSet������֮��Ĺ�ϵ
 * ÿ��Connection�¿����ж��Statement��ÿ��Statement����ִ�ж�����ͬ��sql����,����ÿ��statement��ֻ����һ�����ResultSet�����.
 * ÿ������Connection����Statement,ResultSet֮����Ҫclose����Ϊconnection,statement,resultSet�����ݽṹռ���˺ܴ���ڴ�ռ�.
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
            FileInputStream inputStream = new FileInputStream("db.properties");//��������������ļ��̶�����.
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
