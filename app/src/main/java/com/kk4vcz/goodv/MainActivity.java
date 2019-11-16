package com.kk4vcz.goodv;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;

import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.widget.EditText;

import java.io.IOException;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    NfcAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        //Show the default fragment.
        setFragment();
    }

    @Override
    public void onResume(){
        super.onResume();

        /* When we resume, we need to tell Android that while we have focus, no other app should
         * interfere with our reading of NFC tags.  In onPause(), we'll release that claim.
         */


        PendingIntent intent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        NfcAdapter.getDefaultAdapter(this).enableForegroundDispatch(this, intent, null, null);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(NfcAdapter.getDefaultAdapter(this)!=null)
            NfcAdapter.getDefaultAdapter(this).disableForegroundDispatch(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        String action=intent.getAction();

        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)){
            //The tag is attached to the intent.
            Tag mTag=intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Log.d("TAGID", GoodVUtil.byteArrayToHex(mTag.getId()));


            //We only connect if there's a fragment waiting to handle it.
            if (handler != null) {
                NfcRF430 rf430 = NfcRF430.get(mTag);//new NfcRF430(mTag);
                try {
                    rf430.connect();
                    handler.tagTapped(rf430);
                    rf430.close();
                } catch (IOException e) {
                    Log.d("FAIL", "NFCV connection died before completion.");
                }
            }
        }else{
            //Unknown action type.  Maybe we forgot to make a handler?
            Log.d("GoodV", "onNewIntent()="+action);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //This holds the shown fragment.
    private Fragment fragment=null;
    private Class fragmentClass=InfoFragment.class;
    private NfcRF430Handler handler=null;

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        //Creates a new fragment

        if (id == R.id.nav_info) {
            Log.d("GoodV", "Showing the info activity.");
            fragmentClass=InfoFragment.class;
        } else if (id == R.id.nav_dump) {
            // Handle dumping the firmware
            Log.d("GoodV", "Showing the dumping activity.");
            fragmentClass=DumpFragment.class;
        } else if (id == R.id.nav_program) {
            Log.d("GoodV", "Showing the programming activity.");
            fragmentClass=ProgramFragment.class;
        } else if (id == R.id.nav_erase) {
            Log.d("GoodV", "Showing the erase activity.");
            fragmentClass=EraseFragment.class;
        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }


        setFragment();

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void setFragment(){
        if(fragmentClass!=null) {
            try {
                fragment = (Fragment) fragmentClass.newInstance();
                handler = (NfcRF430Handler) fragment;
            } catch (Exception e) {
                e.printStackTrace();
            }

            //Insert the fragment, replacing what was already there.
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.flContent, fragment).commit();
        }
    }
}
