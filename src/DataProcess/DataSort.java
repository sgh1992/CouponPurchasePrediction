package DataProcess;

import java.io.*;
import java.util.*;
/**
 * Created by sghipr on 2016/2/23.
 * 由于文件数据很大;无法完全存储在内存中,从而没法完全地进行排序.
 * 归并排序算法:
 * 1.将文件分割成许多小文件;
 * 2.对每个小文件进行排序；
 * 3.对所有小文件每次提取出一条记录，总计有n条,然后取出这n条记录中的最小记录值(这里可以运用优先队列的数据结构来实现) 并将它插入到磁盘文件上.
 * 算法总的时间复杂度为O(NlgN)
 */
public class DataSort {
    private String fileName;//需要排序的目标文件.
    private int perFileMemory = 80; //每个文件所占用的存储空间的大小.以M为单位!默认为80M.
    private final boolean asec;//文件记录排序的顺序，升序还是降序.
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
                    if (asec) //如果为升序.
                        return diff > 0 ? 1 : -1;
                    else
                        return diff > 0 ? -1 : 1;//降序.
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
        File file = new File(tempDir);//注意，可以将File理解为一个文件目录.
        mkdir(file);
        //file.mkdir();//新建这个指定的文件目录.
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

    //递归删除文件及目录.
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
     * 对source文件进行排序.
     * 文件中的每行记录都是以逗号分隔的；
     * 排序的标准依赖于所给出的indexs.
     * 排序是依照其map中严格的给出顺序来进行的;而且排序的属性可能是数值型的,由map中的value来确定.
     *
     * @param source 需要排序的源文件.
     * @param indexs 排序的下标索引,严格遵守其所给定的顺序.Boolen若为true，则说明其属性为数值型属性.
     * @return 排好序的文件.
     */
    public File sorted(File source, final LinkedHashMap<Integer, Boolean> indexs) {
        List<String> linesList = new ArrayList<>();
        //读取记录.
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
        //针对记录进行排序
        Collections.sort(linesList, new RecordComparator());
        //将排序好的记录写入文件.
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
        //函数内部类.
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


    //运行时的主程序.
    public File run(String tempDir){
        List<File> splits = splitFiles(tempDir);
        List<File> sorteFiles = sorted(splits,indexs);
        return mergeSort(sorteFiles);
    }

    //测试.
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