import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author YuNingbo
 * @description: TODO
 * @date 2021-05-1017:11
 **/
public class Test {
    private final static Pattern resizedPattern = Pattern.compile(".*_\\d+x\\d+.*");
    private final static Pattern pattern = Pattern.compile(".*\\.([^\\.]*)");
    public static void main(String[] args) {
        ImageResize(200,0);
    }

    public static void ImageResize(int height,int width){
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(new File("C:\\Users\\yunin\\Desktop\\3e0a6a59fa4fb540466350a83e73c04b.jpg"));
            BufferedImage originalImage = ImageIO.read(fileInputStream);
           Thumbnails.of(originalImage).width(height).height(height).outputQuality(0.8).toFile("C:\\Users\\yunin\\Desktop\\aaa2.jpg");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

