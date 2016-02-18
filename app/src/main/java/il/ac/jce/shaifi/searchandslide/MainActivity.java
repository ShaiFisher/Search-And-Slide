package il.ac.jce.shaifi.searchandslide;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends ActionBarActivity implements SearchImagesHandler {

    private Button btnSearch;
    private ViewPager viewPager;
    private AutoCompleteTextView txtQuery;
    private SearchImagesService searchImagesService;
    private List<ImageResult> imageResults;
    private History history;
    private ArrayAdapter<String> historyAdapter;
    private SwipePageAdapter mSwipePager;
    private Gson mGson;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Utils.log("onCreate");

        initComponents();

        // check if comes here back from the app and reload search images if so
        if (savedInstanceState != null)
            reload(savedInstanceState);
    }

    private void initComponents() {

        mGson= new GsonBuilder().create();

        // load search service
        //searchImagesService = new DummySearchImagesService(this, this);
        //searchImagesService = new GoogleSearchImageService(this, this);
        searchImagesService = new BingSearchImageService(this, this);

        // load components
        btnSearch = (Button) findViewById(R.id.btn_search);
        txtQuery = (AutoCompleteTextView) findViewById(R.id.txt_query);

        // load autocomplete history
        history = new History(this);
        historyAdapter = new ArrayAdapter<String>
                (this, android.R.layout.select_dialog_item, history.getHistory());
        txtQuery.setAdapter(historyAdapter);
        txtQuery.setTextColor(Color.WHITE);

        // set the pager adapter to swipe images
        viewPager = (ViewPager) findViewById(R.id.viewPager);

        // set the search button listener
        btnSearch.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 // hide keyboard
                 InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                 imm.hideSoftInputFromWindow(txtQuery.getWindowToken(), 0);

                 String query = txtQuery.getText().toString();
                 // search
                 searchImagesService.searchImages(query);
                 // add to history
                 history.add(query);
                 historyAdapter.insert(query, 0);
             }
        });
    }

    // this method is for reloading the activity - for orientation chnage etc.
    // this method must be loaded AFTER all components have been loaded
    private void reload(Bundle savedInstanceState) {

        // this is a workaround to prevent list to popup on load (while orientation change)
        new Handler().post(new Runnable() {
            public void run() {
                txtQuery.dismissDropDown();
            }
        });

        // here we get data from bundle and load images onto page
        String jsonQueryResults = savedInstanceState.getString(getString(R.string.queryResults));
        if (jsonQueryResults != null) {
            ImageResult[] existingImages = (ImageResult[]) mGson.fromJson(jsonQueryResults, ImageResult[].class);
            List<ImageResult> imagesList = new ArrayList<ImageResult>(Arrays.asList(existingImages));
            handleImagesList(imagesList);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        history.save();
    }

    @Override
    public void handleImagesList(List<ImageResult> imageResults) {
        this.imageResults = imageResults;
        mSwipePager = new SwipePageAdapter(MainActivity.this, this.imageResults);
        viewPager.setAdapter(mSwipePager);

        Utils.log("results recieved:", imageResults.size());
    }

    // save search while app going to sleep or changes orientation
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Store UI state to the savedInstanceState.
        // This bundle will be passed to onCreate on next call.
        // we serialize the object into a json so it can be passed inside a bundle
        String jsonImages = mGson.toJson(imageResults);
        savedInstanceState.putString(getString(R.string.queryResults), jsonImages);

        super.onSaveInstanceState(savedInstanceState);
    }
}
