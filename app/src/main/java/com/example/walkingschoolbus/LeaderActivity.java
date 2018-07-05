package com.example.walkingschoolbus;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.example.walkingschoolbus.model.Group;
import com.example.walkingschoolbus.model.Session;
import com.example.walkingschoolbus.model.User;
import com.example.walkingschoolbus.proxy.ProxyBuilder;
import com.example.walkingschoolbus.proxy.WGServerProxy;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;

public class LeaderActivity extends AppCompatActivity {

    private Session tokenSession = Session.getInstance();
    private List<User> userList = new ArrayList<>();
    private List<String> stringUserList = new ArrayList<>( );
    private Group group;
    private User user;

    private String userToken;
    private static WGServerProxy proxy;

    private static final String TAG = "LeaderActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_leader );

        //get token from session
        userToken = tokenSession.getToken();
        // Build the server proxy
        proxy = ProxyBuilder.getProxy(getString( R.string.api_key),userToken);
        //get the group instance
        user = User.getInstance();
        group = Group.getInstance();
        //Make call
        Call<List<User>> caller = proxy.getGroupMembers(group.getId());
        ProxyBuilder.callProxy(LeaderActivity.this, caller, returnedUsers-> responseForList(returnedUsers));




    }

    private void responseForList(List<User> returnedUsers) {
        notifyUserViaLogAndToast("Got list of " + returnedUsers.size() + " users! See logcat.");
        Log.w(TAG, "Got all users of this group!!");

        SwipeMenuListView userListView = (SwipeMenuListView) findViewById(R.id.userList);

        for (User member : returnedUsers) {

            if(member.getId() != user.getId()) {
                Log.w( TAG, "    member: " + member.getId() );


                String userInfo = "Member : " + member.getName();
                stringUserList.add( userInfo );

                ArrayAdapter adapter = new ArrayAdapter( LeaderActivity.this, R.layout.da_items, stringUserList );

                userListView.setAdapter( adapter );
            }
        }

        SwipeMenuCreator creator = new SwipeMenuCreator() {

            @Override
            public void create(SwipeMenu menu) {

                // create "delete" item
                SwipeMenuItem deleteItem = new SwipeMenuItem( getApplicationContext());
                // set item background
                deleteItem.setBackground(new ColorDrawable( Color.rgb(220, 20, 60)));
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
        userListView.setMenuCreator(creator);

        userListView.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
                switch (index) {

                    case 0:

                         Call<Void> caller = proxy.removeFromMonitoredByUsers(group.getId(), returnedUsers.get(position).getId());
                         ProxyBuilder.callProxy(LeaderActivity.this, caller, returnedNothing -> response(returnedNothing));
                         userListView.removeViewsInLayout(position,1);
                         finish();
                         ArrayAdapter adapter = new ArrayAdapter(LeaderActivity.this, R.layout.da_items, stringUserList);
                         userListView.setAdapter(adapter);




                }


                // false : close the menu; true : not close the menu
                return false;
            }
        });


    }

    private void response(Void returnedNothing) {
        notifyUserViaLogAndToast(" You will not be monitored by this user anymore.");
    }


    private void notifyUserViaLogAndToast(String message) {
        Log.w(TAG, message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**Make intent for main menu activity
     *
     * @param context
     * @return
     */
    public static Intent makeIntent(Context context){
        Intent intent = new Intent( context, LeaderActivity.class );
        return intent;
    }


}
