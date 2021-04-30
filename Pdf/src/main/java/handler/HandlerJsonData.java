package handler;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.model.ServiceException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HandlerJsonData {
    public String handleRequest(Map<String, Object> event, Context context) throws IOException {
        String bucket;
        String folder;
        String newFolder;
        try {
            final AmazonS3 s3client = AmazonS3ClientBuilder
                    .standard()
                    .withRegion(Regions.US_EAST_1)
                    .build();
            LambdaLogger logger = context.getLogger();
            JSONObject myjson = new JSONObject(event);
            bucket = myjson.getString("Bucketname");
            folder = myjson.getString("FolderName");
            newFolder = myjson.getString("newfolder");
            JSONArray myarray = myjson.getJSONArray("splitfiles");
            List<String> pdfs = new ArrayList<>();
            int size = myarray.length();
            for (int i = 0; i < size; i++) {
                pdfs.add(myarray.getString(i));
            }
            for (int i = 0; i < pdfs.size(); i++) {
                String key = pdfs.get(i);
                System.out.println("Key is: "+key);
                String key_Name = folder + key;
                System.out.println("Folder is: "+key_Name);
                 GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, key_Name);
                 S3Object s3Object = s3client.getObject(getObjectRequest);
                 InputStream fileInputStream = s3Object.getObjectContent();
                 File newFile = new File("/tmp/" + key);
                 FileUtils.copyInputStreamToFile(fileInputStream, newFile);
                 try (final PDDocument document = PDDocument.load(newFile)) {
                     String[] keyName = key.split(".pdf");
                     for(String KeyValue : keyName) {
                         PDFRenderer pdfRenderer = new PDFRenderer(document);
                         BufferedImage img = pdfRenderer.renderImageWithDPI(0, 300, ImageType.RGB);
                         System.out.println("Splitted Key is: "+KeyValue);
                         ImageIOUtil.writeImage(img, "/tmp/" + KeyValue + ".png", 300);
                         System.out.println("Output KeyValue is: "+KeyValue+".png");
                         document.close();
                         String key1 = newFolder+"-img/";
                         System.out.println("S3KeyValue: "+key1);
                         s3client.putObject(
                                 bucket,
                                 key1+ KeyValue + ".png",
                                 new File("/tmp/" + KeyValue + ".png")
                         );
                         String fileName  = KeyValue + ".png";
                         String functionName = "Lambda-Worker";
                         String ffolder = newFolder+"-img";
                         System.out.println("New Folder Value: "+ffolder);
                         InvokeRequest invokeRequest = new InvokeRequest()
                                 .withFunctionName(functionName)
                                 .withPayload("{\n" +
                                         " \"Bucketname\": \""+bucket+"\",\n" +
                                         " \"FolderName\": \""+ffolder+"\",\n" +
                                         " \"FileName\": \""+fileName+"\",\n"+
                                         " \"NewFolder\": \""+newFolder+"\"\n"+
                                         "}");
                         InvokeResult invokeResult = null;
                         try {
                             AWSLambda awsLambda = AWSLambdaClientBuilder
                                     .standard()
                                     .withRegion(Regions.US_EAST_1)
                                     .build();
                             invokeResult = awsLambda.invoke(invokeRequest);
                             String res = new String(invokeResult.getPayload().array(), StandardCharsets.UTF_8);
                             //write out the response value
                             System.out.println(res);
                         } catch (ServiceException e) {
                             System.out.println(e);
                         }
                     }
                 } catch (IOException e) {
                 System.err.println("Exception while trying to create pdf document - " + e);
                 }
            }
            }catch (JSONException e) {
            throw new JSONException(e);
        }
        return null;
    }
}


Input Json:-
{
  "Bucketname": "lambdatextractbucket",
  "FolderName": "extracted-pdf/",
  "splitfiles": [
    "document-page14.pdf",
    "document-page15.pdf",
    "document-page16.pdf",
    .....................,
    .....................,
    .....................,
    .....................,
    .....................,
    "document-page...n.pdf"
  ],
 "newfolder": "next_234"
}
           
          


         

