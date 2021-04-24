package handler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
public class Handler implements RequestHandler<Map<String,String>, String> {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public String handleRequest(Map<String, String> event, Context context) {
        try {
            LambdaLogger logger = context.getLogger();
            logger.log("EVENT: " + gson.toJson(event));
            String key = event.get("inputKey");
            String OUTPUT_DIR = "/tmp/";
            try (final PDDocument document = PDDocument.load(new File(key)))
                {
                    PDFRenderer pdfRenderer = new PDFRenderer(document);
                    for (int page = 0; page < document.getNumberOfPages(); ++page) {
                        BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
                        String fileName = OUTPUT_DIR + "image-" + page + ".png";
                        ImageIOUtil.writeImage(bim, fileName, 300);
                    }
                    document.close();
            }catch (IOException e){
                System.err.println("Exception while trying to create pdf document - " + e);
            }
        }
            catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
