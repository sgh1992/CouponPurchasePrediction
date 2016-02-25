package DataProcess;

import dataBase.DB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by sghipr on 2016/2/23.
 * ���ļ����ݵ��뵽���ݿ���.
 */
public class DataLoad {
    /**
     *
     * @param fileName
     * @param tableName
     * @param splitLetter ÿ��֮��ķָ���.
     */
    public static void load(String fileName,String tableName,String splitLetter){
        Connection conn = DB.getConnection();
        try {
            //ע��,��ͬ����ϵͳ�е�lines terminated�ǲ�ͬ��.
            String loadDataSql = "load data local infile ? into table $tableName fields terminated by ? lines terminated by '\\n' IGNORE 1 LINES";
            loadDataSql = loadDataSql.replace("$tableName",tableName);
            //Ϊ�˱���ע�빥��,ʹ��Ԥ�����.
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
