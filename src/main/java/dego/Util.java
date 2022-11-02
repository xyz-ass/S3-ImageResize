package dego;

import com.madgag.gif.fmsware.AnimatedGifEncoder;
import com.madgag.gif.fmsware.GifDecoder;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.resizers.Resizer;

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
            case "474946": return "image/gif";
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

    public static ByteArrayOutputStream resizeImage(byte[] imgByteArray, String size) throws IOException {
        BufferedImage srcImage = ImageIO.read(new ByteArrayInputStream(imgByteArray));
        int srcHeight = srcImage.getHeight();
        int srcWidth = srcImage.getWidth();

        int[] arr = getZoomSize(srcWidth,srcHeight,size);
        int width = arr[0];
        int height = arr[1];

        /*BufferedImage resizedImage = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();
        // 保持png背景透明
        resizedImage = g.getDeviceConfiguration().createCompatibleImage(width,height,Transparency.TRANSLUCENT);
        g = resizedImage.createGraphics();

        // Simple bilinear resize
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(srcImage, 0, 0, width, height, null);
        g.dispose();*/

        // Re-encode image to target format
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Thumbnails.of(new ByteArrayInputStream(imgByteArray)).height(height).width(width).outputQuality(0.9).toOutputStream(os);
        //ImageIO.write(resizedImage, "png", os);
        return os;
    }

    public static ByteArrayOutputStream resizeGif(InputStream is, String size) throws IOException {
        // GIF需要特殊处理
        GifDecoder decoder = new GifDecoder();
        int status = decoder.read(is);
        if (status != GifDecoder.STATUS_OK) {
            throw new IOException("read gif image error!");
        }
        int srcWidth = decoder.getFrameSize().width;
        int srcHeight = decoder.getFrameSize().height;
        int[] arr = getZoomSize(srcWidth,srcHeight,size);
        int width = arr[0];
        int height = arr[1];

        // 拆分一帧一帧的压缩之后合成
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        AnimatedGifEncoder encoder = new AnimatedGifEncoder();
        encoder.start(os);
        encoder.setRepeat(decoder.getLoopCount());
        for (int i = 0; i < decoder.getFrameCount(); i++) {
            encoder.setDelay(decoder.getDelay(i));// 设置播放延迟时间
            BufferedImage bufferedImage = decoder.getFrame(i);// 获取每帧BufferedImage流
            BufferedImage zoomImage = new BufferedImage(width, height, bufferedImage.getType());
            Image image = bufferedImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            Graphics gc = zoomImage.getGraphics();
            gc.setColor(Color.WHITE);
            gc.drawImage(image, 0, 0, null);
            encoder.addFrame(zoomImage);
        }
        encoder.finish();
        return os;
    }

    private static int[] getZoomSize(int srcWidth,int srcHeight,String targetSize){
        String[] sizeArr = targetSize.split("x");
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
        return new int[]{width,height};
    }

    public static void main(String[] args) throws Exception {
        FileInputStream is = new FileInputStream("C:\\Users\\yunin\\Desktop\\sss.png");
        ByteArrayOutputStream os = resizeImage(cloneInputStream(is).toByteArray(), "200x200");
        File f = new File("C:\\Users\\yunin\\Desktop\\s.png");
        OutputStream oos = new FileOutputStream(f);
        oos.write(os.toByteArray());
        oos.flush();
        oos.close();

        //resizeGif("C:\\Users\\yunin\\Desktop\\a8e145b1d15a618ad5e3a9ad42de0155.gif",200,300,"C:\\Users\\yunin\\Desktop\\aaa.gif");
        /*byte[] b = new byte[3];
        is.read(b, 0, b.length);
        String xxx = bytesToHexString(b);
        xxx = xxx.toUpperCase();
        System.out.println("头文件是：" + xxx);
        String ooo = checkType(xxx);
        System.out.println("后缀名是：" + ooo);*/
    }

}
