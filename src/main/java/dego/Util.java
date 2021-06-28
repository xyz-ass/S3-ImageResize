package dego;

import com.amazonaws.services.s3.model.S3ObjectInputStream;

import java.io.FileInputStream;

/**
 * @author YuNingbo
 * @description: TODO
 * @date 2021-05-1018:14
 **/
public class Util {

    private static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    private static String checkType(String xxxx) {
        switch (xxxx) {
            case "FFD8FF": return "image/jpg";
            case "89504E": return "image/png";
            //case "474946": return "image/gif";
            default: return "";
        }
    }

    public static String getFileType(S3ObjectInputStream is){
        try{
            byte[] b = new byte[3];
            is.read(b, 0, b.length);
            String xxx = bytesToHexString(b);
            xxx = xxx.toUpperCase();
            return checkType(xxx);
        }catch (Exception e){
            return "";
        }
    }

    public static void main(String[] args) throws Exception {
        FileInputStream is = new FileInputStream("F:\\桌面\\a.jpg");
        byte[] b = new byte[3];
        is.read(b, 0, b.length);
        String xxx = bytesToHexString(b);
        xxx = xxx.toUpperCase();
        System.out.println("头文件是：" + xxx);
        String ooo = checkType(xxx);
        System.out.println("后缀名是：" + ooo);
    }

}
