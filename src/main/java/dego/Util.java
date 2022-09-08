package dego;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

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

    public static String getFileType(byte[] src){
        try{

            byte[] b = new byte[3];
            System.arraycopy(src,0, b, 0, 3);
            String xxx = bytesToHexString(b);
            xxx = xxx.toUpperCase();
            return checkType(xxx);
        }catch (Exception e){
            return "";
        }
    }

    public static ByteArrayOutputStream cloneInputStream(InputStream input) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = input.read(buffer)) > -1) {
                baos.write(buffer, 0, len);
            }
            baos.flush();
            return baos;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ByteArrayOutputStream resizeImage(InputStream is, String imageType, String size) throws IOException {
        BufferedImage srcImage = ImageIO.read(is);
        int srcHeight = srcImage.getHeight();
        int srcWidth = srcImage.getWidth();
        // Infer the scaling factor to avoid stretching the image
        // unnaturally
        String[] sizeArr = size.split("x");
        float maxWidth = Integer.parseInt(sizeArr[0]);
        float maxHeight = Integer.parseInt(sizeArr[1]);
        float scalingFactor;
        if(maxWidth==0 && maxHeight==0){
            return null;
        }if(maxWidth==0 && maxHeight!=0){
            scalingFactor = maxHeight/srcHeight;
        }else if(maxWidth!=0 && maxHeight==0){
            scalingFactor = maxWidth/srcWidth;
        }else{
            scalingFactor = Math.min(maxWidth / srcWidth, maxHeight / srcHeight);
        }

        int width = (int) (scalingFactor * srcWidth);
        int height = (int) (scalingFactor * srcHeight);

        BufferedImage resizedImage = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resizedImage.createGraphics();
        // Fill with white before applying semi-transparent (alpha) images
        g.setPaint(Color.white);
        g.fillRect(0, 0, width, height);
        // Simple bilinear resize
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(srcImage, 0, 0, width, height, null);
        g.dispose();

        // Re-encode image to target format
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, imageType, os);
        return os;
    }

    public static void main(String[] args) throws Exception {
        FileInputStream is = new FileInputStream("C:\\Users\\yunin\\Desktop\\5e8566ef7bdf361d59dce92810b4b480.jpg");
        ByteArrayOutputStream os = resizeImage(is, "png","50x0");
        File f = new File("C:\\Users\\yunin\\Desktop\\5e8566ef7bdf361d59dce92810b4b480——1.jpg");
        OutputStream oos = new FileOutputStream(f);
        oos.write(os.toByteArray());
        oos.flush();
        oos.close();
        /*byte[] b = new byte[3];
        is.read(b, 0, b.length);
        String xxx = bytesToHexString(b);
        xxx = xxx.toUpperCase();
        System.out.println("头文件是：" + xxx);
        String ooo = checkType(xxx);
        System.out.println("后缀名是：" + ooo);*/
    }

}
