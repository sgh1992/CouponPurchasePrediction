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
 * 建立评分矩阵.
 * 即每一个用户有过评分的所有物品，形成一个训练集.
 * 评分的策略可能不同.
 *购买：  +3
 * 浏览： +1
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
                //用户对物品的打分处理.
                updateScores(array[(title.indexOf("PURCHASE_FLG"))],array[title.indexOf("USER_ID_hash")],array[title.indexOf("VIEW_COUPON_ID_hash")], scoresMap);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //输出结果.
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
