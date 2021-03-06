package il.ac.jce.shaifi.searchandslide;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.util.Base64;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by itais on 09-Feb-16.
 * this class implements image search on the bing search engine
 * gets a search string and returns an ImageResult array with the search results
 */
public class BingSearchImageService  implements SearchImagesService{

    //http request queue
    private RequestQueue mHttpRequestQueue;

    // url of the google web service
    private final String mUrlImageSearch = "https://api.datamarket.azure.com/Bing/Search/v1/Image";
    // api key to use with the search web service
    private final String mApiKey = "HRUwANcXIhfzALDqQzYAi9qlwUd1sgrtJkw+ex4hX1w";

    // returned images list
    private List<ImageResult> mImageUrls;
    private SearchImagesHandler mImageHandler;
    private Context mContext;
    private ProgressDialog mDialog;

    // constructor that gets the activity context for doing visual stuff
    // and SearchImagesHandler to send the image list through
    public BingSearchImageService(Context context, SearchImagesHandler handler){
        mImageUrls = new ArrayList<ImageResult>();
        mImageHandler = handler;
        mContext = context;
        // init the http queue on the activity context
        mHttpRequestQueue = Volley.newRequestQueue(mContext);
        // init the progress dialog
        mDialog = new ProgressDialog(mContext);
        mDialog.setTitle(mContext.getString(R.string.message_search_images));
    }

    // override this function so we can run it here in this class
    @Override
    public void searchImages(String strQuery) {
        // generate web service url from the search string
        String webServiceUrl = generateUrl(strQuery);

        // request the images from web service and return an image list
        getNewImageList(webServiceUrl);
    }

    // generate the url to send request to
    private String generateUrl(String strQuery) {

        // remove all spaces from search string
        strQuery = strQuery.trim();

        // encode the search to be good as a url string
        strQuery = Uri.encode(strQuery);

        // create the url
        String url = mUrlImageSearch;
        url += "?$format=json";
        url += "&Query=%27" + strQuery + "%27";
        Utils.log("BingSearchImageService start url:", url);
        return url;
    }

    // generate image list from given url
    private void getNewImageList(String url) {

        // start the waiting dialog
        mDialog.show();

        // generate the web service request
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                // start response listener to wait for a good response
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // 1. get the list of all images fromm the response
                            JSONObject obj = response.getJSONObject("d");
                            JSONArray arrImages = obj.getJSONArray("results");
                            // check if list is not empty
                            if (arrImages.length() == 0)
                                Utils.displayMessage(mContext, mContext.getString(R.string.message_found_no_images));
                            else {
                                // 2. generate url string array from the json array of image items
                                mImageUrls = getImagesFromJSON(arrImages);
                                // 3. display all images on activity
                                mImageHandler.handleImagesList(mImageUrls);
                                // 4. display toast with returned images found
                                Utils.displayMessage(mContext, mContext.getString(R.string.message_web_service_return_number) + " " + mImageUrls.size());
                            }
                            // 5. stop the wait dialog
                            mDialog.cancel();
                        }
                        catch (JSONException ex){
                            // stop the wait dialog
                            mDialog.cancel();
                            Utils.displayMessage(mContext, mContext.getString(R.string.message_web_service_failed));
                            ex.printStackTrace();
                        }
                    }
                },
                // start listener for an error on the request
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // stop the wait dialog
                        mDialog.cancel();
                        Utils.displayMessage(mContext, mContext.getString(R.string.message_web_service_failed));
                        error.printStackTrace();
                    }
                }
        ) {
            // add the bing api key to the request headers to authenticate
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                String auth = Base64.encodeToString((":" + mApiKey).getBytes(), Base64.NO_WRAP);
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "Basic " + auth);
                return headers;
            }
        } ;

        // add the request to the http queue
        mHttpRequestQueue.add(request);
    }

    // extract a string array of images from the json
    private ArrayList<ImageResult> getImagesFromJSON(JSONArray imageArray) throws JSONException{

        Gson gson = new GsonBuilder().create();
        JSONObject obj;
        String url;
        ArrayList<ImageResult> items = new ArrayList<ImageResult>();
        BingImage item;
        ImageResult imageResult;

        // go over all json image items given
        for (int i = 0; i < imageArray.length(); i++) {

            // get the specific object
            obj = imageArray.getJSONObject(i);
            try{
                // convert json into a bingItem object
                item = gson.fromJson(obj.toString(), BingImage.class);
                // convert the BingImage object to our ImageResult object to be returned (if url exist)
                url = item.getImageUrl();
                if (url != null) {
                    imageResult = new ImageResult(url);
                    imageResult.setTitle(item.getTitle());
                    imageResult.setThumbnailUrl(item.Thumbnail.MediaUrl);
                    items.add(imageResult);
                }
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
        // return the result image array
        return items;
    }

    // inner class to hold a bing image object
    private class BingImage {

        private String ID;
        private String Title;
        private String MediaUrl;
        private String SourceUrl;
        private String DisplayUrl;
        private int Width;
        private int Height;
        private int FileSize;
        private String ContentType;
        private thumbnail Thumbnail;

        public String getImageUrl() {
            if (MediaUrl != null)
                return MediaUrl;
            else
                return null;
        }

        public String getTitle() {
            return Title;
        }

        private class thumbnail {
            private String MediaUrl;
            private String ContentType;
            private int Width;
            private int Height;
            private int FileSize;
        }
    }
}
