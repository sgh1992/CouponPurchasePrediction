package DataProcess;

import java.io.*;
import java.util.*;
/**
 * Created by sghipr on 2016/2/23.
 * �����ļ����ݺܴ�;�޷���ȫ�洢���ڴ���,�Ӷ�û����ȫ�ؽ�������.
 * �鲢�����㷨:
 * 1.���ļ��ָ�����С�ļ�;
 * 2.��ÿ��С�ļ���������
 * 3.������С�ļ�ÿ����ȡ��һ����¼���ܼ���n��,Ȼ��ȡ����n����¼�е���С��¼ֵ(��������������ȶ��е����ݽṹ��ʵ��) ���������뵽�����ļ���.
 * �㷨�ܵ�ʱ�临�Ӷ�ΪO(NlgN)
 */
public class DataSort {
    private String fileName;//��Ҫ�����Ŀ���ļ�.
    private int perFileMemory = 80; //ÿ���ļ���ռ�õĴ洢�ռ�Ĵ�С.��MΪ��λ!Ĭ��Ϊ80M.
    private final boolean asec;//�ļ���¼�����˳�������ǽ���.
    private final LinkedHashMap<Integer, Boolean> indexs;
    private String title;

    class RecordComparator implements Comparator<String> {
        @Override
        public int compare(String o1, String o2) {
            if (indexs == null) {
                return o1.compareTo(o2);
            } else {
                String[] array1 = o1.split(",", -1);
                String[] array2 = o2.split(",", -1);
                Iterator<Map.Entry<Integer, Boolean>> iterator = indexs.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Integer, Boolean> entry = iterator.next();
                    int index = entry.getKey();
                    boolean isNumeric = entry.getValue();
                    double diff;
                    if (isNumeric) {
                        diff = Double.parseDouble(array1[index]) - Double.parseDouble(array2[index]);
                    } else {
                        diff = array1[index].compareTo(array2[index]);
                    }
                    if (diff == 0)
                        continue;
                    if (asec) //���Ϊ����.
                        return diff > 0 ? 1 : -1;
                    else
                        return diff > 0 ? -1 : 1;//����.
                }
                return 0;
            }
        }
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setPerMemory(int memory) {
        perFileMemory = memory;
    }

    public int getPerFileMemory() {
        return perFileMemory;
    }

    public DataSort(String fileName) {
        asec = true;
        indexs = null;
        this.fileName = fileName;
    }

    public DataSort(boolean asec, LinkedHashMap<Integer, Boolean> indexs,String fileName) {
        this.asec = asec;
        this.indexs = indexs;
        this.fileName = fileName;
    }

    public List<File> splitFiles(String tempDir) {
        long perSpace = perFileMemory * 1024 * 1024;
        File file = new File(tempDir);//ע�⣬���Խ�File���Ϊһ���ļ�Ŀ¼.
        mkdir(file);
        //file.mkdir();//�½����ָ�����ļ�Ŀ¼.
        int index = 0;
        List<File> splitList = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
            File curFile = new File(tempDir, String.valueOf(index++));
            BufferedWriter writer = new BufferedWriter(new FileWriter(curFile));
            String str = null;
            int space = 0;
            splitList.add(curFile);
            boolean isTitle = true;
            while ((str = reader.readLine()) != null) {
                if (isTitle) {
                    title = str;
                    isTitle = false;
                    continue;
                }
                if (space > perSpace) {
                    writer.close();
                    curFile = new File(tempDir, String.valueOf(index++));
                    splitList.add(curFile);
                    writer = new BufferedWriter(new FileWriter(curFile));
                    space = 0;
                }
                writer.write(str);
                writer.newLine();
                space += str.getBytes().length;
            }
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return splitList;
    }

    public void mkdir(File file){
        if(file.exists())
            delete(file);
        file.mkdir();
    }

    //�ݹ�ɾ���ļ���Ŀ¼.
    public void delete(File target){
        if(target.isFile())
            target.delete();
        else{
            File[] files = target.listFiles();
            for(File file : files){
                delete(file);
            }
            target.delete();
        }
    }
    /**
     * ��source�ļ���������.
     * �ļ��е�ÿ�м�¼�����Զ��ŷָ��ģ�
     * ����ı�׼��������������indexs.
     * ������������map���ϸ�ĸ���˳�������е�;������������Կ�������ֵ�͵�,��map�е�value��ȷ��.
     *
     * @param source ��Ҫ�����Դ�ļ�.
     * @param indexs ������±�����,�ϸ���������������˳��.Boolen��Ϊtrue����˵��������Ϊ��ֵ������.
     * @return �ź�����ļ�.
     */
    public File sorted(File source, final LinkedHashMap<Integer, Boolean> indexs) {
        List<String> linesList = new ArrayList<>();
        //��ȡ��¼.
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(source)));
            String str = null;
            while ((str = reader.readLine()) != null) {
                linesList.add(str);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //��Լ�¼��������
        Collections.sort(linesList, new RecordComparator());
        //������õļ�¼д���ļ�.
        File sortedFile = new File(source.getParent(), "sorted-" + source.getName());
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(sortedFile));
            for (String line : linesList) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sortedFile;
    }

    public List<File> sorted(List<File> sourceList, LinkedHashMap<Integer, Boolean> indexs) {
        List<File> sortedList = new ArrayList<>();
        for (File source : sourceList) {
            sortedList.add(sorted(source, indexs));
        }
        return sortedList;
    }

    public File mergeSort(List<File> sortedList) {
        //�����ڲ���.
        class Tuple {
            private String record;
            private int index;

            public Tuple(String record, int index) {
                this.record = record;
                this.index = index;
            }
        }
        PriorityQueue<Tuple> priorityQueue = new PriorityQueue<>(sortedList.size(), new Comparator<Tuple>() {
            @Override
            public int compare(Tuple o1, Tuple o2) {
                return new RecordComparator().compare(o1.record, o2.record);
            }
        });
        File mergedSortedFile = new File(sortedList.get(0).getParent(), "mergeSorted-" + new File(fileName).getName());
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(mergedSortedFile));
            writer.write(title);
            writer.newLine();
            List<BufferedReader> readerList = new ArrayList<>();
            BufferedReader reader = null;
            for (File file : sortedList) {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                readerList.add(reader);
            }
            for (int i = 0; i < readerList.size(); i++) {
                priorityQueue.add(new Tuple(readerList.get(i).readLine(), i));
            }
            while (!priorityQueue.isEmpty()) {
                Tuple head = priorityQueue.poll();
                writer.write(head.record);
                writer.newLine();
                String line = readerList.get(head.index).readLine();
                if (line != null)
                    priorityQueue.add(new Tuple(line, head.index));
                else
                    readerList.get(head.index).close();
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mergedSortedFile;
    }


    //����ʱ��������.
    public File run(String tempDir){
        List<File> splits = splitFiles(tempDir);
        List<File> sorteFiles = sorted(splits,indexs);
        return mergeSort(sorteFiles);
    }

    //����.
    public static void main(String[] args){

        String fileName = "F:\\Coupon_Purchase_Predict\\coupon_visit_train.csv\\coupon_visit_train.csv";
        boolean asec = true;
        LinkedHashMap<Integer,Boolean> indexs = new LinkedHashMap<>();
        indexs.put(6,false);
        indexs.put(2,false);
        indexs.put(5,false);
        DataSort dataSort = new DataSort(asec,indexs,fileName);
        File merged = dataSort.run("F:\\Coupon_Purchase_Predict\\sortDirs1");
    }


}