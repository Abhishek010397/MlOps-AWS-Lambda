package handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.gson.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class HandlerJsonData {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public String handleRequest( Map<String,Object> event, Context context) {
        String bucket;
        try {
            JSONObject myjson = new JSONObject(event);
            bucket = myjson.getString("Bucketname");
            JSONArray myarray = myjson.getJSONArray("splitfiles");
            List<String> pdfs = new ArrayList<>();
            int size = myarray.length();
            for(int i =0;   i < size ; i++){
                pdfs.add(myarray.getString(i));
            }
            for( int i =0 ; i < pdfs.size(); i++){
                String key = pdfs.get(i);
                System.out.println(key);
                String OUTPUT_DIR = "/tmp/";
                String file = ("/tmp/"+key);
                System.out.println(file);
                try (final PDDocument document = PDDocument.load(new File(file))){
                    PDFRenderer pdfRenderer = new PDFRenderer(document);
                    for (int page = 0; page < document.getNumberOfPages(); ++page) {
                        BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
                        String fileName = OUTPUT_DIR + "image-" + page + ".png";
                        ImageIOUtil.writeImage(bim, fileName, 300);
                    }
                    document.close();
                } catch (IOException e) {
                    System.err.println("Exception while trying to create pdf document - " + e);
                }
            }
        } catch (org.json.JSONException e) {
            throw new JSONException(e);
        }
        return null;
    }
}


