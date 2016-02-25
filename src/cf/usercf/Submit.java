package cf.usercf;

import java.io.*;
import java.util.*;
/**
 * Created by sghipr on 2016/2/25.
 * 提交结果.
 */
public class Submit {
    private File recommendFile;
    private File itemFile = new File("F:/Coupon_Purchase_Predict/coupon_list_test.csv/coupon_list_test.csv");//实际需要推荐的物品集.
    private double threshold = 3.0;//推荐物品得分的阈值.只有当推荐物品的得分超过这个得分时,才会被推荐.默认评分为3.0
    private int N = 10; //每个用户推荐的物品数.默认为10个物品.
    private static String BESTRECOMMEND = "bestRecommend-";
    private String userFileName = "F:/Coupon_Purchase_Predict/sample_submission.csv (1)/sample_submission.csv";//需要提交的用户集.

    public Submit(){
    }
    public Submit(File recommendFile,double threshold){
        this.recommendFile = recommendFile;
        this.threshold = threshold;
    }
    public Submit(String recommendName,double threshold,int N){
        this.recommendFile = new File(recommendName);
        this.threshold = threshold;
        this.N = N;
    }
    public void setUserFileName(String fileName){
        this.userFileName = fileName;
    }
    public void setRecommendFile(File recommend){
        this.recommendFile = recommend;
    }
    public void setItemFile(File itemFile){
        this.itemFile = itemFile;
    }
    public void setThreshold(double threshold){
        this.threshold = threshold;
    }
    public void setN(int N){
        this.N = N;
    }

    /**
     * 获得物品集.
     * @param itemFile
     * @return
     */
    public Set<String> itemSet(File itemFile){
        Set<String> itemSets = new HashSet<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(itemFile),"utf-8"));
            String str = null;
            String[] titles = reader.readLine().split(",", -1);
            int itemIndex = Arrays.asList(titles).indexOf("COUPON_ID_hash");
            while((str = reader.readLine()) != null){
                String[] columns = str.split(",", -1);
                itemSets.add(columns[itemIndex]);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return itemSets;
    }

    public Set<String> usersSet(){
        return usersSet(userFileName);
    }
    /**
     * 返回用户的集合.
     * @param userFile
     * @return
     */
    public Set<String> usersSet(String userFile){
        BufferedReader reader = null;
        Set<String> set = new HashSet<>();
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(userFile),"utf-8"));
            String str = null;
            reader.readLine();
            while((str = reader.readLine()) != null){
                set.add(str.split(",", -1)[0]);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return set;
    }
    public File bestRecommend(){
        return bestRecommend(recommendFile, threshold,N,itemFile,userFileName);
    }
    /**
     * 获得所有用户的推荐列表.
     * @param recommendFile
     * @param threshold
     * @param N
     * @return
     */
    public File bestRecommend(File recommendFile,double threshold,int N,File itemFile,String userFileName){
        Set<String> userSet = usersSet(userFileName);
        BufferedReader reader = null;
        File bestFile = new File(recommendFile.getParent(),BESTRECOMMEND + recommendFile.getName());
        BufferedWriter writer = null;
        Set<String> itemSets = itemSet(itemFile);
        try {
            writer = new BufferedWriter(new FileWriter(bestFile));
            writer.write("USER_ID_hash,PURCHASED_COUPONS");
            writer.newLine();
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(recommendFile)));
            String str = null;
            while((str = reader.readLine()) != null){
                HashMap<String,HashMap<String,Double>> recommendRecord = recommendRecord(str);
                if(recommendRecord == null)
                    continue;
                String user = recommendRecord.entrySet().iterator().next().getKey();
                List<String> best = bestRecommend(recommendRecord.get(user),threshold,N,itemSets);
                if(best.size() == 0)//当不存在可以推荐的物品时,则过滤此用户.
                    continue;
                userSet.remove(user);
                StringBuilder builder = new StringBuilder();
                builder.append(user).append(",");
                for(String item : best)
                    builder.append(item).append(" ");
                writer.write(builder.toString().trim());
                writer.newLine();
            }
            //将没有可推荐的用户物品写到文件末尾.
            for(String userId : userSet) {
                writer.write(userId + ",");
                writer.newLine();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                reader.close();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bestFile;
    }
    /**
     * 推荐结果的记录格式
     * user     item1:score,item2:score,...
     * @param line
     * @return
     */
    public HashMap<String,HashMap<String,Double>> recommendRecord(String line){
        HashMap<String,HashMap<String,Double>> record = new HashMap<>();
        String[] userAndRecommendItems = line.split("\t", -1);
        String user = userAndRecommendItems[0];
        HashMap<String,Double> items = new HashMap<>();
        if(userAndRecommendItems.length < 2)
            return null;
        for(String pair : userAndRecommendItems[1].split(",", -1)){
            String item = pair.split(":", -1)[0];
            double value = Double.parseDouble(pair.split(":", -1)[1]);
            items.put(item,value);
        }
        record.put(user,items);
        return record;
    }
    /**
     * 获得每个用户的最佳推荐列表.
     * @param recommendItems
     * @param threshold
     * @param N
     * @return
     */
    public List<String> bestRecommend(HashMap<String,Double> recommendItems,double threshold,int N,Set<String> itemSet){
        List<String> bestItems = new ArrayList<>();
        List<Map.Entry<String,Double>> entryList = new ArrayList<>(recommendItems.entrySet());
        //按每个物品的推荐得分进行排序.
        Collections.sort(entryList, new Comparator<Map.Entry<String, Double>>() {
            @Override
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                return o1.getValue() - o2.getValue() == 0 ? 0 : o1.getValue() - o2.getValue() > 0 ? -1 : 1;
            }
        });

        for(Map.Entry<String,Double> entry : entryList){
            if(bestItems.size() == N || entry.getValue() < threshold)
                break;
            if(itemSet.contains(entry.getKey()))
                bestItems.add(entry.getKey());
        }
        //注意，存在推荐列表为空的情况.
        return bestItems;
    }
    /**
     * 测试.
     * @param args
     */
    public static void main(String[] args){
        int N = 10;
        String recommend = "F:/Coupon_Purchase_Predict/sortedTemp/recommendResult.csv";
        double threshold = 3;
        Submit submit = new Submit(recommend,threshold,N);
        submit.bestRecommend();
    }
}
