package evaluate;

import java.io.*;
import java.util.*;

/**
 * Created by sghipr on 2016/2/25.
 */
public class Evaluation {
    private File predict; //Ԥ��Ľ��.
    private File actual;//��ʵ�Ľ��.

    public Evaluation(){

    }
    public Evaluation(File predict,File actual){
        this.predict = predict;
        this.actual = actual;
    }
    public void setPredict(File predict){
        this.predict = predict;
    }
    public void setActual(File actual){
        this.actual = actual;
    }

    /**
     * ������ݼ�.
     * @param file
     * @return
     */
    public HashMap<String,List<String>> dataSet(File file) {
        BufferedReader reader = null;
        HashMap<String,List<String>> data = new HashMap<>();
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String str = null;
            while((str = reader.readLine()) != null){
                HashMap<String,List<String>> items = record(str);
                data.put(items.entrySet().iterator().next().getKey(),items.entrySet().iterator().next().getValue());
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
        return data;
    }

    public double evaluatePrecision(){
        return evaluatePrecision(predict, actual);
    }

    /**
     * ���������Ƽ�������Ƽ�����.
     * �����Ƽ��������ʵ������жԱȣ�����ó��Ƽ�����.
     * @param predict
     * @param actual
     * @return
     */
    public double evaluatePrecision(File predict,File actual){
        HashMap<String,List<String>> actualData = dataSet(actual);
        double precision = 0.0;
        int size = 0;
        try {
            BufferedReader predictReader = new BufferedReader(new InputStreamReader(new FileInputStream(predict)));
            String str = null;
            while((str = predictReader.readLine()) != null){
                HashMap<String,List<String>> recommendMap = record(str);
                String user = recommendMap.entrySet().iterator().next().getKey();
                if(actualData.containsKey(user)){
                    precision += evaluatePrecision(recommendMap.get(user),actualData.get(user));
                }
                size++;
            }
            predictReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(size != 0)
            precision /= size;
        return precision;
    }

    /**
     *���㵥���û����Ƽ�����.
     * @param recommendItems
     * @param actualItems
     * @return
     */
    public double evaluatePrecision(List<String> recommendItems, List<String> actualItems){
        double precsion = 0;
        int right = 0;
        int itemSize = Math.min(recommendItems.size(),actualItems.size());
        if(itemSize == 0)
            return 0;
        for(int i = 0; i < recommendItems.size(); i++){
            if(actualItems.contains(recommendItems.get(i))) {
                precsion += 1.0/itemSize * (++right/(i + 1.0));//ע�⣬���ּ��㷽ʽ.
            }
        }
        return precsion;
    }

    /**
     * ÿ����¼�ĸ�ʽ.
     * ���ҽ�֮����Ϊuser      recommendItem1,recommendItem2,...
     * @param line
     * @return
     */
    public HashMap<String,List<String>> record(String line){
        String[] userAndRecommends = line.split("\t", -1);
        String user = userAndRecommends[0];
        String[] recommendItems = userAndRecommends[1].split(",");
        HashMap<String,List<String>> recordMap = new HashMap<>();
        List<String> recommendList = new ArrayList<>();
        for(String recommenditem : recommendItems)
            recommendList.add(recommenditem);
        recordMap.put(user,recommendList);
        return  recordMap;
    }


}
