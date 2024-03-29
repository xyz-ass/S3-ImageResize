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
import java.util.regex.Pattern;

/**
 * @author YuNingbo
 * @description: TODO
 * @date 2021-05-0912:26
 **/
public class Handler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final Pattern resizedPattern = Pattern.compile(".*_\\d+x\\d+.*");
    private final String bucket = "metaverses";
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
            String onlyPng = params.getOrDefault("png","false");
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
            InputStream objectData = s3Object.getObjectContent();
            ByteArrayOutputStream bs = Util.cloneInputStream(objectData);
            byte[] byteArr = bs.toByteArray();
            String imageMemi = Util.getFileType(byteArr);
            if("".equals(imageMemi)){
                return response;
            }
            ByteArrayOutputStream os;
            if(imageMemi.contains("gif") && "false".equals(onlyPng)){
                //os = Util.resizeGif(new ByteArrayInputStream(byteArr),size);  gif如果不转成png的话返回原图
                return response;
            }else{
                os = Util.resizeImage(byteArr, size);
            }

            InputStream is = new ByteArrayInputStream(os.toByteArray());
            // Set Content-Length and Content-Type
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(os.size());
            meta.setContentType(imageMemi);

            try {
                s3Client.putObject(bucket, dstKey, is, meta);
            }catch(Exception e){
                e.printStackTrace();
                return response;
            }
            response.setBody(String.format(resFormat, dstKey));
        } catch (IOException e) {
            e.printStackTrace();
            return response;
        }
        return response;
    }
}
