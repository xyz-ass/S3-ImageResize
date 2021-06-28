package dego;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author YuNingbo
 * @description: TODO
 * @date 2021-05-0912:26
 **/
public class Handler implements
        RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final String JPG_MIME =  "image/jpg";
    private final String PNG_MIME =  "image/png";
    private final Pattern resizedPattern = Pattern.compile(".*_\\d+x\\d+.*");
    private final String bucket = "nft-bsc";
    private final String resFormat = "{\"key\":\"%s\"}";

    @Override
    public APIGatewayProxyResponseEvent  handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setIsBase64Encoded(false);
        response.setStatusCode(200);
        HashMap<String, String> headers = new HashMap<>(3);
        headers.put("Content-Type", "application/json");
        response.setHeaders(headers);
        try {
            Map<String,String> params = event.getQueryStringParameters();
            String srcKey = params.get("key");
            System.out.println("srcKey:"+srcKey);
            // 先使用原始路径
            response.setBody(String.format(resFormat, srcKey));

            // 如果原始路径是已经压缩过的，则返回原始路径
            if(resizedPattern.matcher(srcKey).matches()){
                return response;
            }

            String size = params.getOrDefault("size","");
            System.out.println("size:"+size);
            if("".equals(size)){
                return response;
            }
            size = size.toLowerCase();
            String dstKey;
            int dotIndex = srcKey.lastIndexOf(".");
            if(dotIndex>0){
                String suffix = srcKey.substring(dotIndex);
                dstKey = srcKey.substring(0,dotIndex)+"_"+size+suffix;
            }else{
                dstKey = srcKey+"_"+size;
            }

            // Download the image from S3 into a stream
            AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
            S3Object s3Object = s3Client.getObject(new GetObjectRequest(bucket, srcKey));
            String imageMemi = Util.getFileType(s3Object.getObjectContent());
            String imageType;
            if(JPG_MIME.equals(imageMemi)){
                imageType = "jpg";
            }else if(PNG_MIME.equals(imageMemi)){
                imageType= "png";
            }else{
                System.out.println("Wrong ImageType:"+imageMemi);
                return response;
            }
            InputStream objectData = s3Object.getObjectContent();

            // Read the source image
            BufferedImage srcImage = ImageIO.read(objectData);
            int srcHeight = srcImage.getHeight();
            int srcWidth = srcImage.getWidth();
            // Infer the scaling factor to avoid stretching the image
            // unnaturally
            String[] sizeArr = size.split("x");
            float maxWidth = Integer.parseInt(sizeArr[0]);
            float maxHeight = Integer.parseInt(sizeArr[1]);
            float scalingFactor;
            if(maxWidth==0 && maxHeight==0){
                return response;
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
            ImageIO.write(resizedImage, imageType, os);
            InputStream is = new ByteArrayInputStream(os.toByteArray());
            // Set Content-Length and Content-Type
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(os.size());
            meta.setContentType(imageMemi);

            try {
                s3Client.putObject(bucket, dstKey, is, meta);
            }catch(Exception e){
                return response;
            }

            response.setBody(String.format(resFormat, dstKey));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return response;
    }
}
