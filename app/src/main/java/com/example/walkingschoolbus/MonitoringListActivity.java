package com.example.walkingschoolbus;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.example.walkingschoolbus.model.Session;
import com.example.walkingschoolbus.model.User;
import com.example.walkingschoolbus.proxy.ProxyBuilder;
import com.example.walkingschoolbus.proxy.WGServerProxy;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;

/**
 * Activity lists users whom logged in users monitor with options to delete
 */
public class MonitoringListActivity extends AppCompatActivity {

    private static final String TAG = "MonitoringListActivity";
    private Session session;
    private User user;
    private static WGServerProxy proxy;

    private ArrayList<String> monitoringUser = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        user = User.getInstance();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitoring_list);

        Session.getStoredSession(this);
        session = Session.getInstance();
        String savedToken = session.getToken();
        setMonitoringTextView();

        proxy = ProxyBuilder.getProxy(getString(R.string.api_key),session.getToken());

        // Make call
        Call<List<User>> caller = proxy.getMonitorsUsers(user.getId());
        ProxyBuilder.callProxy(MonitoringListActivity.this, caller, returnedUsers -> response(returnedUsers));
    }

    private void response(List<User> returnedUsers) {
        notifyUserViaLogAndToast("Got list of " + returnedUsers.size() + " users! See logcat.");

        SwipeMenuListView monitoringList = (SwipeMenuListView) findViewById(R.id.monitoringList);
        //List<String> monitoringUser = new ArrayList<>();
        //List<Integer> child_ID = new ArrayList<Integer>();
        for (User user : returnedUsers) {
            Log.w(TAG, "    User: " + user.toString());
            String userInfo = getString(R.string.monitoring_user_name) + " "  + user.getName() +
                    getString(R.string.monitoring_user_email)+ " " + user.getEmail();

            monitoringUser.add(userInfo);
            ArrayAdapter adapter = new ArrayAdapter(MonitoringListActivity.this, R.layout.da_items, monitoringUser);
            monitoringList.setAdapter(adapter);
        }
        SwipeMenuCreator creator = new SwipeMenuCreator() {

            @Override
            public void create(SwipeMenu menu) {



                // create "add" item
                SwipeMenuItem addItem = new SwipeMenuItem( getApplicationContext());
                // set item background
                addItem.setBackground(new ColorDrawable(Color.rgb(0xC9, 0xC9,
                        0xCE)));

                // set item width
                addItem.setWidth(180);
                // set item title
                addItem.setTitle("Add");
                // set item title font size
                addItem.setTitleSize(18);
                // set item title font color
                addItem.setTitleColor(Color.WHITE);
                // add to menu
                menu.addMenuItem(addItem);

                // create "open" item
                SwipeMenuItem deleteItem = new SwipeMenuItem( getApplicationContext());
                // set item background
                deleteItem.setBackground(new ColorDrawable(Color.rgb(220, 20, 60)));
                // set item width
                deleteItem.setWidth(180);
                // set item title
                deleteItem.setTitle("Delete");
                // set item title fontsize
                deleteItem.setTitleSize(18);
                // set item title font color
                deleteItem.setTitleColor(Color.WHITE);
                // add to menu
                menu.addMenuItem(deleteItem);
            }
        };

        // set creator
        monitoringList.setMenuCreator(creator);

        monitoringList.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
                switch (index) {
                    case 0:

                        Intent intent = MonitorActivity.makeIntent(MonitoringListActivity.this,
                                returnedUsers.get(position).getEmail());
                        startActivity(intent);

                        break;


                    case 1:

                        // Make call
                        Call<Void> caller = proxy.removeFromMonitorsUsers(user.getId(), returnedUsers.get(position).getId());
                        ProxyBuilder.callProxy(MonitoringListActivity.this, caller, returnedNothing -> response(returnedNothing));
                        monitoringList.removeViewsInLayout(position,1);

                        break;


                }

                // false : close the menu; true : not close the menu
                return false;
            }
        });
    }

    private void response(Void returnedNothing) {
        notifyUserViaLogAndToast(MonitoringListActivity.this.getString(R.string.notify_not_monitor));
    }


    private void setMonitoringTextView() {
        TextView monitoringList = (TextView) findViewById( R.id.monitoringListText );
        String monitoring = getString(R.string.monitoring_title);
        monitoringList.setText( monitoring );

    }


    public static Intent makeIntent(Context context) {
        Intent intent = new Intent(context, MonitoringListActivity.class);
        return intent;
    }



    private void notifyUserViaLogAndToast(String message) {
        Log.w(TAG, message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }


}
