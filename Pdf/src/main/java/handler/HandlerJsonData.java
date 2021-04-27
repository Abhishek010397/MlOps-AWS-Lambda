package handler;
import com.amazonaws.AmazonServiceException;
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
    public String handleRequest(Map<String, Object> event, Context context) {
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
                System.out.println(key);
                String key_Name = folder + key;
                System.out.println(key_Name);
                GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, key_Name);
                S3Object s3Object = s3client.getObject(getObjectRequest);
                InputStream fileInputStream = s3Object.getObjectContent();
                File newFile = new File("/tmp/"+key);
                FileUtils.copyInputStreamToFile(fileInputStream, newFile);
                try {
                    try (final PDDocument document = PDDocument.load(newFile)) {
                        PDFRenderer pdfRenderer = new PDFRenderer(document);
                        for (int page = 0; page < document.getNumberOfPages(); ++page) {
                            BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
                            String fileName = "document-page-" + page + ".png";
                            ImageIOUtil.writeImage(bim, "/tmp/document-page-" + page + ".png", 300);
                            s3client.putObject(
                                    bucket,
                                    "extracted-image/"+fileName,
                                    new File("/tmp/" + fileName)
                            );
                        }
                        document.close();
                    } catch (IOException e) {
                        System.err.println("Exception while trying to create pdf document - " + e);
                    }
                } catch (AmazonServiceException e) {
                    System.err.println(e.getErrorMessage());
                    System.exit(1);
                }
            }
        }catch (JSONException | IOException e) {
            throw new JSONException(e);
        }
        return null;
    }
}