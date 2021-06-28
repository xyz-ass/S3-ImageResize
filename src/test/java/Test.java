import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
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
        String srcKey = "images/bsc/0x577012bb4caec8b06084352f9b78e9404f0e8c46/004cd68c13ed76a03154414168792043_76x23.jpg";
        Matcher matcher = pattern.matcher(srcKey);
        if (!matcher.matches()) {
            System.out.println("Unable to infer image type for key " + srcKey);
        }
        String imageType = matcher.group(1);
        System.out.println(imageType);
    }

    public static void ImageResize(File file, String target){
        // Read the source image
        try{
            BufferedImage srcImage = ImageIO.read(file);
            int srcHeight = srcImage.getHeight();
            int srcWidth = srcImage.getWidth();
            // Infer the scaling factor to avoid stretching the image
            // unnaturally
            float maxWidth = 300;
            float maxHeight = 300;
            float scalingFactor;
            if(maxWidth==0 && maxHeight==0){
                return;
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
                    BufferedImage.TYPE_INT_RGB);
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
            ImageIO.write(resizedImage, "jpg", os);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}

