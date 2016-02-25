package cf.usercf;

import DataProcess.DataSort;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Created by sghipr on 2016/2/23.
 */
public class UserCF {
    private String originFile;//原始文件数据.
    private static String sortedTempDir = "F:/Coupon_Purchase_Predict/sortedTemp";//默认排序的中间目录文件.
    private static String SIMILIRY = "similiary.csv";//为了避免内存不够,将计算出的部分相似矩阵存入到文件中,然后再通过文件的方式来获得完整的用户相似矩阵.
   private static String RECOMMEND = "recommendResult.csv";
    private static int N = 10;//默认寻找与其最相似的10个用户.

    public UserCF(String originFile){
        this.originFile = originFile;
    }
    public void setN(int N){
        this.N = N;
    }
    public void setSortedTempDir(String dir){
        sortedTempDir = dir;
    }
    public void setOriginFile(String file){
        originFile = file;
    }
    public String getOriginFile(){
        return originFile;
    }
    /**
     * 对原始数据进行预处理
     * @param asec
     * @param indexs
     * @param dir
     * @return
     */
    public File prepare(boolean asec,LinkedHashMap<Integer,Boolean> indexs,String dir){
        //排序.
        DataSort dataSort = new DataSort(asec,indexs,originFile);
        return dataSort.run(dir);
    }
    /**
     * 获得其用户评分的训练集,并将数据写入到文件中.
     * @param file
     * @return
     */
    public File getTrainData(String file){
        BuildTrainData bTrainData = new BuildTrainData();
        return bTrainData.evaluateScores(file);
    }

    /**
     * 根据文件中的数据，得到其评分训练集.
     * @param train
     * @return
     */
    public HashMap<String,HashMap<String,Double>> getTrainMap(File train){
        HashMap<String,HashMap<String,Double>> trainMap = new HashMap<>();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(train)));
            String str = null;
            while((str = reader.readLine()) != null){
                String[] userIdAndItemPair = str.split("\t", -1);
                String[] pairs = userIdAndItemPair[1].split(",", -1);
                if(!trainMap.containsKey(userIdAndItemPair[0])){
                    trainMap.put(userIdAndItemPair[0],new HashMap<String, Double>());
                }
                for(String pair : pairs){
                    String[] itemAndScores = pair.split(":", -1);
                    trainMap.get(userIdAndItemPair[0]).put(itemAndScores[0],Double.parseDouble(itemAndScores[1]));
                }
            }
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return trainMap;
    }
    /**
     * 当训练集的数据量很大,无法全部放入内存时,应该如何计算相似性匹配呢 ???
     * 建立物品倒排表.
     * @param train
     * @return
     */
    public HashMap<String,Set<String>> itemInverted(File train){
        HashMap<String,Set<String>> invertedMap = new HashMap<>();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(train)));
            String str = null;
            while((str = reader.readLine()) != null){
                String[] userIdAndItemPair = str.split("\t", -1);
                String userId = userIdAndItemPair[0];
                String[] itemPair = userIdAndItemPair[1].split(",", -1);
                for(String pair : itemPair){
                    String[] itemAndScores = pair.split(":", -1);
                    String item = itemAndScores[0];
                    if(invertedMap.containsKey(item)){
                        if(!invertedMap.get(item).contains(userId))
                            invertedMap.get(item).add(userId);
                    }
                    else{
                        HashSet<String> set = new HashSet<>();
                        set.add(userId);
                        invertedMap.put(item, set);
                    }
                }
            }
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return invertedMap;
    }
    /**
     * 计算用户间的相似矩阵.
     * 这个方法有内存限制;
     * 它必须要将所有有交互行为用户对全部找出来,当用户对数很大时,内存无法装下;
     * 解决方法:(Map-Reduce 思想)
     * 1.批量地将用户相似性数据写入到文件中.写入地格式:user1+user2:similarityScore...
     * 然后再用排序的方法，将数据按照用户对进行排序，这时需要用到归并排序(文件太大,需要分隔文件);
     * 排序的结果，是具有相同用户对的user Pair是相邻的,然后针对每个user pair来获得其相似性;
     * 时间复杂度取决于物品倒排表中的用户对数.
     * @param train
     * @return
     */
    public HashMap<String,HashMap<String,Double>> userSimiliary(File train){
        HashMap<String,HashMap<String,Double>> trainMap = getTrainMap(train);
        HashMap<String,Set<String>> itemInvertedMap = itemInverted(train);
        Iterator<Map.Entry<String,Set<String>>> iterator = itemInvertedMap.entrySet().iterator();
        HashMap<String,HashMap<String,Double>> similiaryMap = new HashMap<>();
        int debug = 0;
        while(iterator.hasNext()){
            Map.Entry<String,Set<String>> entry = iterator.next();
            String item = entry.getKey();
            Set<String> userSet = entry.getValue();
            for(String user : userSet){
                for(String otherUser : userSet){
                    if(user.equals(otherUser))
                        continue;
                    double score = similiary(trainMap.get(user).get(item),trainMap.get(otherUser).get(item));//余弦相似性.
                    //注意,A与B的相似性等价于B与A的相似性;因此,只需要存储一对即可;即存储了A与B的相似性就不用存储B与A的相似性了.
                    if(similiaryMap.containsKey(user) && similiaryMap.get(user).containsKey(otherUser)){
                        score += similiaryMap.get(user).get(otherUser);
                        similiaryMap.get(user).put(otherUser,score);
                    }
                    else if(similiaryMap.containsKey(otherUser) && similiaryMap.get(otherUser).containsKey(user)){
                        score += similiaryMap.get(otherUser).get(user);
                        similiaryMap.get(otherUser).put(user,score);
                    }
                    else if(similiaryMap.containsKey(user)){
                        similiaryMap.get(user).put(otherUser,score);
                        debug++;
                    }
                    else if(similiaryMap.containsKey(otherUser)) {
                        similiaryMap.get(otherUser).put(user, score);
                        debug++;
                    }
                    else{
                        HashMap<String,Double> scoreMap = new HashMap<>();
                        scoreMap.put(otherUser,score);
                        similiaryMap.put(user, scoreMap);
                        debug++;
                    }
                    if(debug % 10000 == 0)
                        System.out.println(debug);
                }
            }
        }
        itemInvertedMap.clear();
        //计算完整的余弦相似性.
        Iterator<Map.Entry<String,HashMap<String,Double>>> userIterator = similiaryMap.entrySet().iterator();
        while(userIterator.hasNext()){
            Map.Entry<String,HashMap<String,Double>> userEntry = userIterator.next();
            String user = userEntry.getKey();
            HashMap<String,Double> scoreMap = userEntry.getValue();
            Iterator<Map.Entry<String,Double>> scoreIterator = scoreMap.entrySet().iterator();
            while(scoreIterator.hasNext()){
                Map.Entry<String,Double> scoreEntry = scoreIterator.next();
                String otherUser = scoreEntry.getKey();
                double scores = scoreEntry.getValue();
                scores /= (sqrt(trainMap.get(user).values()) * sqrt(trainMap.get(otherUser).values()));//余弦相似性的计算.
                scoreEntry.setValue(scores);
            }
        }
        trainMap.clear();
        return similiaryMap;
    }
    /**
     * 将半量的用户相似性矩阵写入到文件中.
     * @param similiaryMap
     * @return
     */
    public File writeSimiliarity(HashMap<String,HashMap<String,Double>> similiaryMap){
        File writeFile = new File(new File(originFile).getParent(),SIMILIRY);
        DecimalFormat df = new DecimalFormat("0.00000");
        BufferedWriter writer = null;
        try {
             writer = new BufferedWriter(new FileWriter(writeFile));
            Iterator<Map.Entry<String,HashMap<String,Double>>> userIterator = similiaryMap.entrySet().iterator();
            while(userIterator.hasNext()){
                Map.Entry<String,HashMap<String,Double>> userEntry = userIterator.next();
                String user = userEntry.getKey();
                HashMap<String,Double> scoresMap = userEntry.getValue();
                StringBuilder recordBuilder = new StringBuilder();
                recordBuilder.append(user).append("\t");
                Iterator<Map.Entry<String,Double>> scoresIterator = scoresMap.entrySet().iterator();
                while(scoresIterator.hasNext()){
                    Map.Entry<String,Double> scoreEntry = scoresIterator.next();
                    String otherUser = scoreEntry.getKey();
                    double score = scoreEntry.getValue();
                    recordBuilder.append(otherUser).append(":").append(df.format(score)).append(",");
                }
                writer.write(recordBuilder.toString().substring(0,recordBuilder.toString().length() - 1));
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
        return writeFile;
    }
    /**
     * 返回完整的用户相似性矩阵.
     * @param similiaryFile
     * @return
     */
    public HashMap<String,HashMap<String,Double>> allSimiliarity(File similiaryFile){
        HashMap<String,HashMap<String,Double>> allSimiliartyMap = new HashMap<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(similiaryFile)));
            String str = null;
            while((str = reader.readLine()) != null){
                //注意每条相似记录的格式!!!
                HashMap<String,HashMap<String,Double>> simiRecord = similiartyRecord(str);
                String user = simiRecord.entrySet().iterator().next().getKey();
                if(!allSimiliartyMap.containsKey(user))
                    allSimiliartyMap.put(user,new HashMap<String, Double>());

                for(Map.Entry<String,Double> entry : simiRecord.get(user).entrySet()){
                    String simiUser = entry.getKey();
                    double simiScore = entry.getValue();
                    allSimiliartyMap.get(user).put(simiUser,simiScore);
                    if(!allSimiliartyMap.containsKey(simiUser))
                        allSimiliartyMap.put(simiUser,new HashMap<String, Double>());
                    allSimiliartyMap.get(simiUser).put(user,simiScore);
                }
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
        return allSimiliartyMap;
    }
    public double similiary(double value1,double value2){
        return value1 * value2;
    }
    public double sqrt(Collection<Double> values){
        double squareValue = 0;
        for(double value : values)
            squareValue += Math.pow(value,2);
        return Math.sqrt(squareValue);
    }
    public File recommend(File similiaryFile,int n,File train){
        return recommend(similiaryFile,n,getTrainMap(train));
    }
    /**
     * 对所有用户都进行推荐.
     * @param similiaryFile
     * @param n 所取的相似用户的个数.
     * @param trainMap
     * @return
     */
    public File recommend(File similiaryFile, int n, HashMap<String,HashMap<String,Double>> trainMap){
        BufferedWriter writer = null;
        File recommendResult = new File(similiaryFile.getParent(),RECOMMEND);
        try {
            writer = new BufferedWriter(new FileWriter(recommendResult));
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(similiaryFile)));
            String str = null;
            while((str = reader.readLine()) != null){
                HashMap<String,HashMap<String,Double>> recordMap = similiartyRecord(str);
                LinkedHashMap<String,Double> recommendList = recommend(recordMap,n,trainMap);
                if(recommendList.size() == 0)
                    continue;
                StringBuilder recommendBuild = new StringBuilder();
                recommendBuild.append(recordMap.entrySet().iterator().next().getKey()).append("\t");
                for(Map.Entry<String,Double> entry : recommendList.entrySet()){
                    recommendBuild.append(entry.getKey()).append(":").append(entry.getValue()).append(",");
                }
                writer.write(recommendBuild.toString().substring(0,recommendBuild.toString().length() - 1));
                writer.newLine();
            }
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return recommendResult;
    }
    /**
     *   对单个的用户进行推荐.
     */
    public LinkedHashMap<String,Double> recommend(HashMap<String,HashMap<String,Double>> userSimiliartyRecord,int n,HashMap<String,HashMap<String,Double>> trainMap){
        class Tuple{
            double value;
            double weight;
            public Tuple(double value, double weight){
                this.value = value;
                this.weight = weight;
            }
            public void addValue(double value){
                this.value += value;
            }
            public void addWeight(double weight){
                this.weight += weight;
            }
            public void addVW(double value,double weight){
                addValue(value);
                addWeight(weight);
            }
            public double average(){
                if(weight != 0)
                    return value/weight;
                return 0;
            }
        }
        HashMap<String,Double> neighborMap = neighbors(userSimiliartyRecord,n);//相似的用户集.
        HashMap<String,Tuple> recommendMap = new HashMap<>();//推荐结果.
        HashMap<String,Double> userRecordMap = trainMap.get(userSimiliartyRecord.entrySet().iterator().next().getKey());//当前用户的行为记录.
        for(String neighbor : neighborMap.keySet()){
            HashMap<String,Double> itemEvaluates = trainMap.get(neighbor);
            double similiarty = neighborMap.get(neighbor);
            //遍历其相似用户的物品集,在其中计算当前用户没有评分过的物品的推荐评分.
            for(Map.Entry<String,Double> entry : itemEvaluates.entrySet()){
                String item = entry.getKey();
                double score = entry.getValue();
                if(!userRecordMap.containsKey(item)){//评分可能需要通过一个阈值来进行计算.
                    if(recommendMap.containsKey(entry.getKey())) {
                        recommendMap.get(entry.getKey()).addVW(score * similiarty,similiarty);
                    }
                    else
                        recommendMap.put(entry.getKey(),new Tuple(score * similiarty,similiarty));
                }
            }
        }
        //将推荐物品的评分由高到低的顺序返回.
        LinkedHashMap<String,Double> sortedRecommendMap = new LinkedHashMap<>();
        List<Map.Entry<String,Tuple>> sortList = new ArrayList<>(recommendMap.entrySet());
        Collections.sort(sortList, new Comparator<Map.Entry<String, Tuple>>() {
            @Override
            public int compare(Map.Entry<String, Tuple> o1, Map.Entry<String, Tuple> o2) {
                return o1.getValue().average() - o2.getValue().average() == 0 ? 0 : o1.getValue().average() - o2.getValue().average() > 0 ? -1 : 1;
            }
        });
        for(Map.Entry<String,Tuple> entry : sortList){
            sortedRecommendMap.put(entry.getKey(),entry.getValue().average());
        }
        return sortedRecommendMap;
    }

    /**
     * 每个用户相似的记录;数据格式为:userId   simiUser1:simiScore,simiUser2:simiScore,...
     * @param userSimiliaryRecord
     * @return
     */
    public HashMap<String,HashMap<String,Double>> similiartyRecord(String userSimiliaryRecord){
        HashMap<String,HashMap<String,Double>> record = new HashMap<>();
        String[] userAndScorePairs = userSimiliaryRecord.split("\t", -1);
        String user = userAndScorePairs[0];
        String[] scorePairs = userAndScorePairs[1].split(",", -1);
        HashMap<String,Double> scoreMap = new HashMap<>();
        for(String pair : scorePairs){
            String[] simiUserAndScore = pair.split(":", -1);
            scoreMap.put(simiUserAndScore[0],Double.parseDouble(simiUserAndScore[1]));
        }
        record.put(user,scoreMap);
        return record;
    }

    public HashMap<String,Double> neighbors(HashMap<String,HashMap<String,Double>> signalSimiliartyMap,int n){
        List<Map.Entry<String,Double>> userSimiliartys = new ArrayList<>(signalSimiliartyMap.get(signalSimiliartyMap.entrySet().iterator().next().getKey()).entrySet());
        Collections.sort(userSimiliartys, new Comparator<Map.Entry<String, Double>>() {
            @Override
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                return o1.getValue() - o2.getValue() == 0 ? 0 : o1.getValue() - o2.getValue() > 0 ? -1 : 1;
            }
        });
        HashMap<String,Double> neighborMap = new HashMap<>();
        for(int i = 0; i < userSimiliartys.size() && i < n; i++){
            neighborMap.put(userSimiliartys.get(i).getKey(),userSimiliartys.get(i).getValue());
        }
        return neighborMap;
    }
    /**
     * @param train
     * @return
     */
    public File recommendRun(File train){
        HashMap<String,HashMap<String,Double>> similiaryMap = userSimiliary(train);//部分的用户相似性矩阵.
        File simiFile = writeSimiliarity(similiaryMap);
        similiaryMap.clear();

        HashMap<String,HashMap<String,Double>> allSimiliaryMap = allSimiliarity(simiFile);//完整的用户相似性矩阵.
        simiFile = writeSimiliarity(allSimiliaryMap);
        allSimiliaryMap.clear();

        HashMap<String,HashMap<String,Double>> trainMap = getTrainMap(train);
        return recommend(simiFile,N,trainMap);
    }
    public void run(boolean asec,LinkedHashMap<Integer,Boolean> indexs){
        //File processedFile = prepare(asec,indexs,sortedTempDir);
        //File trainFile = getTrainData(processedFile.getPath());
        //测试
//        File train = new File("F:/Coupon_Purchase_Predict/sortedTemp/scores-mergeSorted-coupon_visit_train.csv");
//        File recommendResult = recommendRun(train);

    }

    /**
     * 测试!!!
     * @param args
     */
    public static void main(String[] args){
        //测试.
        File similiarityFile = new File("F:/Coupon_Purchase_Predict/sortedTemp/usersSimilarity-scores-mergeSorted-coupon_visit_train.csv");
        File trainFile = new File("F:/Coupon_Purchase_Predict/sortedTemp/scores-mergeSorted-coupon_visit_train.csv");
        int N = 10;//相似用户.
        UserCF userCF = new UserCF(trainFile.getAbsolutePath());
        userCF.recommend(similiarityFile,N,trainFile);
    }
}
