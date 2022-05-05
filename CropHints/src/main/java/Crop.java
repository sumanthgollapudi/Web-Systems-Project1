import com.google.appengine.api.datastore.*;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.CropHintsAnnotation;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.protobuf.ByteString;
import org.apache.commons.io.IOUtils;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This class is used to fetch the images from facebook and crop them
 * @author STSC
 * 
 */
@MultipartConfig
@WebServlet(
        name = "Crop",
        urlPatterns = {"/Crop"}
)

public class Crop extends HttpServlet {
    public Crop() {
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        String url = request.getParameter("hiddenField");
        String FbPhotoId = request.getParameter("Fb_image_id");
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        if (checkIfImageExists(datastore, FbPhotoId) == false) {
            List<EntityAnnotation> imageLabels = getImageLabels(url);
            CropHintsAnnotation image = detectCropHints(url);
            if (imageLabels != null) {
                List<String> lables = imageLabels.stream().filter(label -> label.getScore() * 100 > 75)
                        .map(EntityAnnotation::getDescription).collect(Collectors.toList());
                if (null != lables && !lables.isEmpty()) {
                			int a=lables.indexOf("person");
                			List<String> arr = new ArrayList<String>(1);
                			arr.add("");
                    addImageDetailsToDataStore(url, lables, image, FbPhotoId, datastore);
                    getImageFromStore(request, response, datastore, FbPhotoId);
                }
            }
        }else{
            getImageFromStore(request, response, datastore, FbPhotoId);
        }
    }
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws
            ServletException, IOException {
    }

    /**
     * This method adds an image aand its corresponding label to the datastore
     * @param url image url
     * @param labels list of labels 
     * @param image image fetched from facebook
     * @param imageId id of image
     * @param datastore a datastore object
     */
    public static void addImageDetailsToDataStore(String url, List<String> labels, CropHintsAnnotation image, String imageId, DatastoreService
            datastore) {
        Entity User_Images = new Entity("User_Images");
        User_Images.setProperty("image_id", imageId);
        User_Images.setProperty("image_url", url);
        User_Images.setProperty("image", image);
        StringBuffer result = new StringBuffer();
        for(String label: labels) {
        	if(label=="human") {
            result.append(",").append(label).append(" ");
        	}
        }
        User_Images.setProperty("labels", result.toString());
        datastore.put(User_Images);
    }

    private void getImageFromStore(HttpServletRequest request, HttpServletResponse response, DatastoreService datastore, String imageId) {

        Query query =
                new Query("User_Images")
                        .setFilter(new Query.FilterPredicate("image_id", Query.FilterOperator.EQUAL, imageId));
        PreparedQuery pq = datastore.prepare(query);
        List<Entity> results = pq.asList(FetchOptions.Builder.withDefaults());
        if(null != results) {
            results.forEach(user_Photo -> {
                String labelsFromStore = (String) user_Photo.getProperty("labels");
                String image_url=user_Photo.getProperty("image_url").toString();
                request.setAttribute("imageUrl",image_url );
                request.setAttribute("imageLabels", labelsFromStore);
                RequestDispatcher dispatcher = getServletContext()
                        .getRequestDispatcher("/labels.jsp");
                try {
                    dispatcher.forward(request, response);
                } catch (ServletException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

    }

    /**
     * This method checks if images exists in the datastore
     * @param datastore a datastore object
     * @param imageId id of image
     * @return returns true if image exists
     */
    public static boolean checkIfImageExists(DatastoreService datastore, String imageId) {
        Query q =
                new Query("User_Images")
                        .setFilter(new Query.FilterPredicate("image_id", Query.FilterOperator.EQUAL, imageId));
        PreparedQuery pq = datastore.prepare(q);
        Entity result = pq.asSingleEntity();
        if (result == null) {
            return false;
        }
        return true;
    }

    private static byte[] downloadFile(URL url) throws Exception {
        try (InputStream in = url.openStream()) {
            byte[] bytes = IOUtils.toByteArray(in);
            return bytes;
        }
    }
    private List<EntityAnnotation> getImageLabels(String imageUrl) {
        try {
            byte[] imgBytes = downloadFile(new URL(imageUrl));
            ByteString byteString = ByteString.copyFrom(imgBytes);
            Image image = Image.newBuilder().setContent(byteString).build();
            Feature feature = Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build();
            AnnotateImageRequest request =
                    AnnotateImageRequest.newBuilder().addFeatures(feature).setImage(image).build();
            List<AnnotateImageRequest> requests = new ArrayList<>();
            requests.add(request);
            ImageAnnotatorClient client = ImageAnnotatorClient.create();
            BatchAnnotateImagesResponse batchResponse = client.batchAnnotateImages(requests);
            client.close();
            List<AnnotateImageResponse> imageResponses = batchResponse.getResponsesList();
            AnnotateImageResponse imageResponse = imageResponses.get(0);
            if (imageResponse.hasError()) {
                System.err.println("Error getting image labels: " + imageResponse.getError().getMessage());
                return null;
            }
            return imageResponse.getLabelAnnotationsList();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private CropHintsAnnotation detectCropHints(String imageUrl) {
        try {
			
			byte[]  imgBytes = downloadFile(new URL(imageUrl));  //Download photo from FB
			
			ByteString byteString = ByteString.copyFrom(imgBytes);
			Image image = Image.newBuilder().setContent(byteString).build();

			Feature feature = Feature.newBuilder().setType(Feature.Type.CROP_HINTS).build();
			AnnotateImageRequest request =
					AnnotateImageRequest.newBuilder().addFeatures(feature).setImage(image).build();
			List<AnnotateImageRequest> requests = new ArrayList<>();
			requests.add(request);

			ImageAnnotatorClient client = ImageAnnotatorClient.create();
			BatchAnnotateImagesResponse batchResponse = client.batchAnnotateImages(requests);
			client.close();
			List<AnnotateImageResponse> imageResponses = batchResponse.getResponsesList();
			AnnotateImageResponse imageResponse = imageResponses.get(0);

			if (imageResponse.hasError()) {
				System.err.println("Error : " + imageResponse.getError().getMessage());
				return null;
			}

			return imageResponse.getCropHintsAnnotation();
		}catch (Exception e) {
			e.printStackTrace();
		}
		return null;
      }


}

