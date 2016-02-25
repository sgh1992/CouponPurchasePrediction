package DataProcess;

import dataBase.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by sghipr on 2016/2/23.
 * 将文件数据导入到数据库中.
 */
public class DataLoad {
    /**
     *
     * @param fileName
     * @param tableName
     * @param splitLetter 每列之间的分隔符.
     */
    public static void load(String fileName,String tableName,String splitLetter){
        Connection conn = DB.getConnection();
        try {
            //注意,不同操作系统中的lines terminated是不同的.
            String loadDataSql = "load data local infile ? into table $tableName fields terminated by ? lines terminated by '\\n' IGNORE 1 LINES";
            loadDataSql = loadDataSql.replace("$tableName",tableName);
            //为了避免注入攻击,使用预备语句.
            PreparedStatement preStat = conn.prepareStatement(loadDataSql);
            preStat.setString(1,fileName);
            preStat.setString(2,splitLetter);
            preStat.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
