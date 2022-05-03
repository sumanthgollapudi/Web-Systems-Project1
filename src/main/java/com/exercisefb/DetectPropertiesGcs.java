package com.exercisefb;


import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.ColorInfo;
import com.google.cloud.vision.v1.DominantColorsAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class DetectPropertiesGcs {
	/*
  public static void detectPropertiesGcs() throws IOException {
	  // TODO(developer): Replace these variables before running the sample.
	  String filePath = "https://storage.cloud.google.com/project14628/1.jpg?authuser=0";
	  detectPropertiesGcs(filePath);
  }
  */
  // Detects image properties such as color frequency from the specified remote image on Google
  // Cloud Storage.
  public static void detectPropertiesGcs(String gcsPath,LinkedHashMap<String, Float> map) throws IOException {
    List<AnnotateImageRequest> requests = new ArrayList<>();

    ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(gcsPath).build();
    Image img = Image.newBuilder().setSource(imgSource).build();
    Feature feat = Feature.newBuilder().setType(Feature.Type.IMAGE_PROPERTIES).build();
    AnnotateImageRequest request =
        AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
    requests.add(request);

    // Initialize client that will be used to send requests. This client only needs to be created
    // once, and can be reused for multiple requests. After completing all of your requests, call
    // the "close" method on the client to safely clean up any remaining background resources.
    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
      BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
      List<AnnotateImageResponse> responses = response.getResponsesList();

      for (AnnotateImageResponse res : responses) {
        if (res.hasError()) {
          System.out.format("Error: %s%n", res.getError().getMessage());
          return;
        }

        // For full list of available annotations, see http://g.co/cloud/vision/docs
        DominantColorsAnnotation colors = res.getImagePropertiesAnnotation().getDominantColors();
        float red=0,green=0,blue=0;
        for (ColorInfo color : colors.getColorsList()) {
        	red += color.getColor().getRed() * color.getPixelFraction();
            green += color.getColor().getGreen() * color.getPixelFraction();
            blue += color.getColor().getBlue() * color.getPixelFraction();        
            }
        map.put("red", red);
        map.put("green", green);
        map.put("blue", blue);
      }
    }
  }
}