package handler;
import com.amazonaws.regions.Regions;
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
import java.util.*;

public class HandlerJsonData {
    public String handleRequest(Map<String, Object> event, Context context) throws IOException {
        //Java Code takes Json with List, Then it converts the json list to ArrayList,
        //It also writing to /tmp/
        //It calls Pdfbox to convert .pdf to .png
        //it also uploading .png to s3
        //it will sent chunks to downstream
        String bucket;
        String folder;
        try {
            final AmazonS3 s3client = AmazonS3ClientBuilder
                    .standard()
                    .withRegion(Regions.US_EAST_1)
                    .build();
            LambdaLogger logger = context.getLogger();
            JSONObject myjson = new JSONObject(event);
            bucket = myjson.getString("Bucketname");
            folder = myjson.getString("FolderName");
            JSONArray myarray = myjson.getJSONArray("splitfiles");
            List<String> pdfs = new ArrayList<>();
            int size = myarray.length();
            for (int i = 0; i < size; i++) {
                pdfs.add(myarray.getString(i));
            }
            for (int i = 0; i < pdfs.size(); i++) {
                String key = pdfs.get(i);
                System.out.println("Key is: "+key);
                String newKey = key.replaceAll("[^\\d]", " ");
                newKey = newKey.trim();
                Integer index = Integer.parseInt(newKey);
                //System.out.println(index);
                String key_Name = folder + key; //extracted-pdf/document-page0.pdf
                System.out.println("Folder is: "+key_Name);
                 //* key information is coming from json
                 //* Iam appending that key with folder and asking s3 to give me the content of that
                 GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, key_Name);
                 S3Object s3Object = s3client.getObject(getObjectRequest);
                 InputStream fileInputStream = s3Object.getObjectContent();
                 File newFile = new File("/tmp/" + key);//key = document-page0.pdf
                 FileUtils.copyInputStreamToFile(fileInputStream, newFile);
                 /**
                 BufferedReader br = new BufferedReader(new FileReader(newFile));
                 int j =0;
                 String content;
                 while((content = br.readLine())!= null)
                     System.out.println(j++);
                 try {
                  **/
                 try (final PDDocument document = PDDocument.load(newFile)) {
                     //String fileName = "document-page-" + "page" + ".png";
                     String[] keyName = key.split(".pdf");//{"document-page-0"}
                     for(String KeyValue : keyName) {
                         PDFRenderer pdfRenderer = new PDFRenderer(document);
                         BufferedImage img = pdfRenderer.renderImageWithDPI(index, 300, ImageType.RGB);
                         System.out.println("Index is: "+index);
                         System.out.println("Splitted Key is: "+KeyValue);
                         ImageIOUtil.writeImage(img, "/tmp/" + KeyValue + ".png", 300);
                         System.out.println("Output KeyValue is: "+KeyValue+".png");
                         document.close();
                         s3client.putObject(
                                 bucket,
                                 "extracted-image/" + KeyValue + ".png",
                                 new File("/tmp/" + KeyValue + ".png")
                         );
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

