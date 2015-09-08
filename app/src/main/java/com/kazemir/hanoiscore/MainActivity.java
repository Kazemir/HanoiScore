package com.kazemir.hanoiscore;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.google.gson.JsonArray;

import com.koushikdutta.ion.Ion;
import com.koushikdutta.async.future.FutureCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, SwipeRefreshLayout.OnRefreshListener {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer and swipe refresher.
     */
    private NavigationDrawerFragment    mNavigationDrawerFragment;
    private SwipeRefreshLayout          mSwipeRefreshLayout;

    /**
     * Used to store the last screen title.
     */
    private CharSequence mTitle;

    /**
     * Used to store the last screen number.
     */
    private int mCurrentTab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTitle = getTitle();
        mCurrentTab = 0;

        // Set up the refresher.
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        // Set up the drawer.
        mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                .commit();
    }

    /**
     * Fills scores data from the server to the listView
     * @param difficulty 0 - Easy, 1 - Medium, 2 - Hard
     */
    private void getRecordsData(int difficulty){
        String hanoiURL;

        switch (difficulty) {
            case 0:
                hanoiURL = "http://oxyel.azurewebsites.net/hanoiScores.php?type=getTOPEasy";
                break;
            case 1:
                hanoiURL = "http://oxyel.azurewebsites.net/hanoiScores.php?type=getTOPMedium";
                break;
            case 2:
                hanoiURL = "http://oxyel.azurewebsites.net/hanoiScores.php?type=getTOPHard";
                break;
            default:
                hanoiURL = "http://oxyel.azurewebsites.net/hanoiScores.php?type=getTOPEasy";
                break;
        }

        mCurrentTab = difficulty;

        // Execute HTTP GET request for gettin' JsonArray from the server
        Ion.with(getApplicationContext())
            .load(hanoiURL)
            .asJsonArray()
            .setCallback(new FutureCallback<JsonArray>() {
                @Override
                public void onCompleted(Exception e, JsonArray result) {
                    // Check network accessibility
                    if (e instanceof UnknownHostException) {
                        Toast.makeText(getApplicationContext(), getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                        return;
                    } else if (e != null) { // check any other errors
                        Toast.makeText(getApplicationContext(), getString(R.string.error_occur) + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ListView listView = (ListView) findViewById(R.id.listView);

                    List<Map<String, String>> data = new ArrayList<Map<String, String>>();

                    // put the data from server to the Array
                    for (int i = 0; i < result.size(); i++) {
                        Map<String, String> datum = new HashMap<String, String>(2);

                        // title forming
                        String title = String.format("%02d", i + 1) + ". " + result.get(i).getAsJsonObject().get("fullname").getAsString();

                        int steps = result.get(i).getAsJsonObject().get("steps").getAsInt();
                        int time = result.get(i).getAsJsonObject().get("time").getAsInt();

                        // time converting
                        int minutes = (time / 1000) / 60;
                        int seconds = (time / 1000) - minutes * 60;
                        int mSeconds = time - (minutes * 60 + seconds) * 1000;

                        datum.put("title", title);
                        // sub-title forming
                        datum.put("time", String.format(getString(R.string.steps) + " %04d, " + getString(R.string.time) + " %02d:%02d:%03d", steps, minutes, seconds, mSeconds));
                        data.add(datum);
                    }

                    // put the data to the listView
                    SimpleAdapter adapter = new SimpleAdapter(getBaseContext(), data,
                            android.R.layout.simple_list_item_2,
                            new String[]{"title", "time"},
                            new int[]{android.R.id.text1,
                                    android.R.id.text2});

                    listView.setAdapter(adapter);

                    mSwipeRefreshLayout.setRefreshing(false);
                }
            });
    }

    @Override
    public void onRefresh() {
        // set the SwipeRefresher state to 'Refreshing' and update current data set
        mSwipeRefreshLayout.setRefreshing(true);
        getRecordsData(mCurrentTab);
    }

    /**
     * Change title and data according to section change
     * @param number Number of section
     */
    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                getRecordsData(0);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                getRecordsData(1);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                getRecordsData(2);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // execute HTTP GET request for getting' the visitors table
            Ion.with(getApplicationContext())
                    .load("http://oxyel.azurewebsites.net/hanoiDB.php?type=hanoiGet")
                    .asString()
                    .setCallback(new FutureCallback<String>() {
                        @Override
                        public void onCompleted(Exception e, String result) {
                            // Check network accessibility
                            if (e instanceof UnknownHostException) {
                                Toast.makeText(getApplicationContext(), getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                                return;
                            } else if (e != null) { // check any other errors
                                Toast.makeText(getApplicationContext(), getString(R.string.error_occur) + e.getMessage(), Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // get the last row of the table and take the number
                            String num = result.substring(result.lastIndexOf("</th></tr><tr><th>") + 18);
                            num = num.substring(0, num.indexOf("</th><th>"));

                            // build the AlertDialog and show number of unique visitors
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                            builder.setMessage(getString(R.string.dialog_message) + " " + num)
                                    .setTitle(R.string.action_settings)
                                    .setCancelable(false)
                                    .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    });

                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                    });

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

}
