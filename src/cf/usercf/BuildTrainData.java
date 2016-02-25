package cf.usercf;

import dataBase.DB;

import java.io.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by sghipr on 2016/2/23.
 * �������־���.
 * ��ÿһ���û��й����ֵ�������Ʒ���γ�һ��ѵ����.
 * ���ֵĲ��Կ��ܲ�ͬ.
 *����  +3
 * ����� +1
 *
 */
public class BuildTrainData {

    public File evaluateScores(String sourceFileName){
        File source = new File(sourceFileName);
        HashMap<String,HashMap<String,Integer>> scoresMap = new HashMap<>();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(source)));
            String str = null;
            List<String> title = Arrays.asList(reader.readLine().split(",", -1));
            while((str = reader.readLine()) != null){
                String[] array = str.split(",", -1);
                //�û�����Ʒ�Ĵ�ִ���.
                updateScores(array[(title.indexOf("PURCHASE_FLG"))],array[title.indexOf("USER_ID_hash")],array[title.indexOf("VIEW_COUPON_ID_hash")], scoresMap);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //������.
        File result = new File(source.getParent(),"scores-" + source.getName());
        dataWrite(scoresMap,result);
        return result;
    }
    public void dataWrite(HashMap<String,HashMap<String,Integer>> scoresMap,File file) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            Set<String> userIdSet = scoresMap.keySet();
            for(String userId : userIdSet){
                Set<String> itemSet = scoresMap.get(userId).keySet();
                StringBuilder recordBuild = new StringBuilder();
                recordBuild.append(userId).append("\t");
                for(String item : itemSet){
                    recordBuild.append(item).append(":").append(scoresMap.get(userId).get(item)).append(",");
                }
                writer.write(recordBuild.toString().substring(0,recordBuild.toString().length() - 1));
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void updateScores(String flag,String userId,String itemId,HashMap<String,HashMap<String,Integer>> scoresMap){
        int score = flag.equals("1") ? 1 : 3;
        if(scoresMap.containsKey(userId)){
            if(scoresMap.get(userId).containsKey(itemId))
                score += scoresMap.get(userId).get(itemId);
            scoresMap.get(userId).put(itemId,score);
        }
        else{
            HashMap<String,Integer> itemMap = new HashMap<>();
            itemMap.put(itemId,score);
            scoresMap.put(userId,itemMap);
        }
    }
}
