package DataProcess;

/**
 * Created by sghipr on 2016/2/23.
 */
public class test {

    public static void main(String[] args){
        //1.���ݵ��뵽���ݿ���.
        String originFile = "F:\\Coupon_Purchase_Predict\\coupon_visit_train.csv\\coupon_visit_train.csv";
        String tableName = "coupon_visit_train";
        DataLoad.load(originFile,tableName,",");
    }
}
