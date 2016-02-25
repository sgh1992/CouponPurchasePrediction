package cf.usercf;

import java.io.*;
import java.util.*;

/**
 * Created by sghipr on 2016/2/25.
 * ����ÿ�����û�����Ϊ��¼������ԭʼ��������ƥ��
 * ����ʱ�临�Ӷ�ΪO(mn^2)������mΪ��Ʒ����,nΪ�û���.
 */
public class OriginalSimilarity {

    private File train;
    private static String SIMILARITY = "usersSimilarity-";
    public OriginalSimilarity(String trainName){
        this(new File(trainName));
    }
    public OriginalSimilarity(File train){
        this.train = train;
    }
    public void setTrain(File train){
        this.train = train;
    }

    /**
     * ÿ���û�����Ϊ��¼;���ݸ�ʽΪ:userId   simiUser1:simiScore,simiUser2:simiScore,...
     * @param behaviorRecord
     * @return
     */
    public HashMap<String,HashMap<String,Double>> userBehaviorRecord(String behaviorRecord){
        HashMap<String,HashMap<String,Double>> record = new HashMap<>();
        String[] userAndScorePairs = behaviorRecord.split("\t", -1);
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

    /**
     * ����û���Ϊ��¼��.
     * @param train
     * @return
     */
    public HashMap<String,HashMap<String,Double>> getTrain(File train){
        HashMap<String,HashMap<String,Double>> dataMap = new HashMap<>();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(train)));
            String str = null;
            reader.readLine();
            while((str = reader.readLine()) != null){
                HashMap<String,HashMap<String,Double>> userRecord = userBehaviorRecord(str);
                dataMap.put(userRecord.entrySet().iterator().next().getKey(),userRecord.entrySet().iterator().next().getValue());
            }
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dataMap;
    }

    public File similarity(){
        return similarity(train);
    }

    /**
     * ��������û���������.
     * @param train
     * @return
     */
    public File similarity(File train){
        HashMap<String,HashMap<String,Double>> trainData = getTrain(train);
        File simiFile = new File(train.getParent(),SIMILARITY + train.getName());
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(simiFile));
            for(Map.Entry<String,HashMap<String,Double>> entry : trainData.entrySet()){
                String user = entry.getKey();
                HashMap<String,Double> items = entry.getValue();
                HashMap<String,Double> similary = similarity(items,trainData);
                if(similary.size() == 0)
                    continue;//�������������û����Ƶ������û�,�򽫸��û�ɾ��.
                List<Map.Entry<String,Double>> sortSimilary = sortUserSimilarity(similary);
                StringBuilder similaryBuilder = new StringBuilder();
                similaryBuilder.append(user).append("\t");
                for(Map.Entry<String,Double> pair : sortSimilary){
                    similaryBuilder.append(pair.getKey()).append(":").append(pair.getValue()).append(",");
                }
                writer.write(similaryBuilder.toString().substring(0,similaryBuilder.toString().length() - 1));
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
        return simiFile;
    }
    /**
     * �����û���������.
     * ������¼��.
     * @param userItemRecord
     * @param train
     * @return
     */
    public HashMap<String,Double> similarity(HashMap<String,Double> userItemRecord,HashMap<String,HashMap<String,Double>> train){
        HashMap<String,Double> similary = new HashMap<>();
        for(Map.Entry<String,HashMap<String,Double>> entry : train.entrySet()){
            String otherUser = entry.getKey();
            HashMap<String,Double> otherItems = entry.getValue();
            double simi = cosine(userItemRecord,otherItems);
            if(simi == 0) //������߼����ƶ�Ϊ0�����ü�¼.
                continue;
            similary.put(otherUser,simi);
        }
        //ע��,similaryΪ�յ����.
        return similary;
    }

    /**
     * ���û��������ԣ��ɸߵ��͵Ľ�������.
     * @param similarity
     * @return
     */
    public List<Map.Entry<String,Double>> sortUserSimilarity(HashMap<String,Double> similarity){
        List<Map.Entry<String,Double>> sortList = new ArrayList<>(similarity.entrySet());
        Collections.sort(sortList, new Comparator<Map.Entry<String, Double>>() {
            @Override
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                return o1.getValue() - o2.getValue() == 0 ? 0 : o1.getValue() - o2.getValue() > 0 ? -1 : 1;//����.
            }
        });
        return sortList;
    }

    /**
     * ����������.
     * @param item1
     * @param item2
     * @return
     */
    public double cosine(HashMap<String,Double> item1,HashMap<String,Double> item2){
        double similiarity = 0;
        double square1 = 0;
        for(Map.Entry<String,Double> entry : item1.entrySet()){
            String item = entry.getKey();
            double score = entry.getValue();
            square1 += Math.pow(score,2);
            if(item2.containsKey(item))
                similiarity += score * item2.get(item);
        }
        double square2 = 0;
        for(double value : item2.values())
            square2 += Math.pow(value,2);

        if(square2 * square1 != 0)
            return similiarity/Math.sqrt(square1 * square2);
        return 0;
    }

    public static void main(String[] args){
        String trainName = "F:/Coupon_Purchase_Predict/sortedTemp/scores-mergeSorted-coupon_visit_train.csv";
        OriginalSimilarity originalSimilarity = new OriginalSimilarity(trainName);
        originalSimilarity.similarity();
    }







}
