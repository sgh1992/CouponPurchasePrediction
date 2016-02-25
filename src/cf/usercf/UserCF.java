package cf.usercf;

import DataProcess.DataSort;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Created by sghipr on 2016/2/23.
 */
public class UserCF {
    private String originFile;//ԭʼ�ļ�����.
    private static String sortedTempDir = "F:/Coupon_Purchase_Predict/sortedTemp";//Ĭ��������м�Ŀ¼�ļ�.
    private static String SIMILIRY = "similiary.csv";//Ϊ�˱����ڴ治��,��������Ĳ������ƾ�����뵽�ļ���,Ȼ����ͨ���ļ��ķ�ʽ������������û����ƾ���.
   private static String RECOMMEND = "recommendResult.csv";
    private static int N = 10;//Ĭ��Ѱ�����������Ƶ�10���û�.

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
     * ��ԭʼ���ݽ���Ԥ����
     * @param asec
     * @param indexs
     * @param dir
     * @return
     */
    public File prepare(boolean asec,LinkedHashMap<Integer,Boolean> indexs,String dir){
        //����.
        DataSort dataSort = new DataSort(asec,indexs,originFile);
        return dataSort.run(dir);
    }
    /**
     * ������û����ֵ�ѵ����,��������д�뵽�ļ���.
     * @param file
     * @return
     */
    public File getTrainData(String file){
        BuildTrainData bTrainData = new BuildTrainData();
        return bTrainData.evaluateScores(file);
    }

    /**
     * �����ļ��е����ݣ��õ�������ѵ����.
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
     * ��ѵ�������������ܴ�,�޷�ȫ�������ڴ�ʱ,Ӧ����μ���������ƥ���� ???
     * ������Ʒ���ű�.
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
     * �����û�������ƾ���.
     * ����������ڴ�����;
     * ������Ҫ�������н�����Ϊ�û���ȫ���ҳ���,���û������ܴ�ʱ,�ڴ��޷�װ��;
     * �������:(Map-Reduce ˼��)
     * 1.�����ؽ��û�����������д�뵽�ļ���.д��ظ�ʽ:user1+user2:similarityScore...
     * Ȼ����������ķ����������ݰ����û��Խ���������ʱ��Ҫ�õ��鲢����(�ļ�̫��,��Ҫ�ָ��ļ�);
     * ����Ľ�����Ǿ�����ͬ�û��Ե�user Pair�����ڵ�,Ȼ�����ÿ��user pair�������������;
     * ʱ�临�Ӷ�ȡ������Ʒ���ű��е��û�����.
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
                    double score = similiary(trainMap.get(user).get(item),trainMap.get(otherUser).get(item));//����������.
                    //ע��,A��B�������Եȼ���B��A��������;���,ֻ��Ҫ�洢һ�Լ���;���洢��A��B�������ԾͲ��ô洢B��A����������.
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
        //��������������������.
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
                scores /= (sqrt(trainMap.get(user).values()) * sqrt(trainMap.get(otherUser).values()));//���������Եļ���.
                scoreEntry.setValue(scores);
            }
        }
        trainMap.clear();
        return similiaryMap;
    }
    /**
     * ���������û������Ծ���д�뵽�ļ���.
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
     * �����������û������Ծ���.
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
                //ע��ÿ�����Ƽ�¼�ĸ�ʽ!!!
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
     * �������û��������Ƽ�.
     * @param similiaryFile
     * @param n ��ȡ�������û��ĸ���.
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
     *   �Ե������û������Ƽ�.
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
        HashMap<String,Double> neighborMap = neighbors(userSimiliartyRecord,n);//���Ƶ��û���.
        HashMap<String,Tuple> recommendMap = new HashMap<>();//�Ƽ����.
        HashMap<String,Double> userRecordMap = trainMap.get(userSimiliartyRecord.entrySet().iterator().next().getKey());//��ǰ�û�����Ϊ��¼.
        for(String neighbor : neighborMap.keySet()){
            HashMap<String,Double> itemEvaluates = trainMap.get(neighbor);
            double similiarty = neighborMap.get(neighbor);
            //�����������û�����Ʒ��,�����м��㵱ǰ�û�û�����ֹ�����Ʒ���Ƽ�����.
            for(Map.Entry<String,Double> entry : itemEvaluates.entrySet()){
                String item = entry.getKey();
                double score = entry.getValue();
                if(!userRecordMap.containsKey(item)){//���ֿ�����Ҫͨ��һ����ֵ�����м���.
                    if(recommendMap.containsKey(entry.getKey())) {
                        recommendMap.get(entry.getKey()).addVW(score * similiarty,similiarty);
                    }
                    else
                        recommendMap.put(entry.getKey(),new Tuple(score * similiarty,similiarty));
                }
            }
        }
        //���Ƽ���Ʒ�������ɸߵ��͵�˳�򷵻�.
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
     * ÿ���û����Ƶļ�¼;���ݸ�ʽΪ:userId   simiUser1:simiScore,simiUser2:simiScore,...
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
        HashMap<String,HashMap<String,Double>> similiaryMap = userSimiliary(train);//���ֵ��û������Ծ���.
        File simiFile = writeSimiliarity(similiaryMap);
        similiaryMap.clear();

        HashMap<String,HashMap<String,Double>> allSimiliaryMap = allSimiliarity(simiFile);//�������û������Ծ���.
        simiFile = writeSimiliarity(allSimiliaryMap);
        allSimiliaryMap.clear();

        HashMap<String,HashMap<String,Double>> trainMap = getTrainMap(train);
        return recommend(simiFile,N,trainMap);
    }
    public void run(boolean asec,LinkedHashMap<Integer,Boolean> indexs){
        //File processedFile = prepare(asec,indexs,sortedTempDir);
        //File trainFile = getTrainData(processedFile.getPath());
        //����
//        File train = new File("F:/Coupon_Purchase_Predict/sortedTemp/scores-mergeSorted-coupon_visit_train.csv");
//        File recommendResult = recommendRun(train);

    }

    /**
     * ����!!!
     * @param args
     */
    public static void main(String[] args){
        //����.
        File similiarityFile = new File("F:/Coupon_Purchase_Predict/sortedTemp/usersSimilarity-scores-mergeSorted-coupon_visit_train.csv");
        File trainFile = new File("F:/Coupon_Purchase_Predict/sortedTemp/scores-mergeSorted-coupon_visit_train.csv");
        int N = 10;//�����û�.
        UserCF userCF = new UserCF(trainFile.getAbsolutePath());
        userCF.recommend(similiarityFile,N,trainFile);
    }
}
