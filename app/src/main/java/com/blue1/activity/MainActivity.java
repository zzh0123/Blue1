package com.blue1.activity;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.blue1.R;
import com.blue1.adapter.RecyclerBlueToothAdapter;
import com.blue1.bean.BlueTooth;
import com.blue1.receiver.BlueToothReceiver;
import com.blue1.service.BluetoothChatService;
import com.blue1.util.ToastUtil;
import com.blue1.vinterface.BlueToothInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener , BlueToothInterface, RecyclerBlueToothAdapter.OnItemClickListener{
    private static final String TAG = "MainActivity";
    public static final int BLUE_TOOTH_DIALOG = 0x111;
    public static final int BLUE_TOOTH_TOAST = 0x123;
    public static final int BLUE_TOOTH_WRAITE = 0X222;
    public static final int BLUE_TOOTH_READ = 0X333;
    public static final int BLUE_TOOTH_SUCCESS = 0x444;

    private RecyclerView recyclerView;
    private Switch st;
    private BluetoothAdapter mBluetoothAdapter;
    private Timer timer;
    private WifiTask task;
    private RecyclerBlueToothAdapter recyclerAdapter;
    private List<BlueTooth> list = new ArrayList<>();
    private BlueToothReceiver mReceiver;
    private ProgressDialog progressDialog;
    private SwipeRefreshLayout swipeRefreshLayout;

    private BluetoothChatService mBluetoothChatService;
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case BluetoothAdapter.STATE_ON:
                case BluetoothAdapter.STATE_OFF: {
                    if (msg.what == BluetoothAdapter.STATE_ON) {
                        st.setText("???????????????");
                        Log.e(TAG, "onCheckedChanged: startIntent");
                        //????????????
                        swipeRefreshLayout.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                swipeRefreshLayout.setRefreshing(true);
                                onRefreshListener.onRefresh();
                            }
                        }, 300);
                        //??????socket??????
                        mBluetoothChatService = BluetoothChatService.getInstance(handler);
                        mBluetoothChatService.start();
                    } else if (msg.what == BluetoothAdapter.STATE_OFF) {
                        st.setText("???????????????");
                        recyclerAdapter.setWifiData(null);
                        recyclerAdapter.notifyDataSetChanged();
                        mBluetoothChatService.stop();
                    }
                    timer.cancel();
                    timer = null;
                    task = null;
                    st.setClickable(true);
                }
                break;
            case BLUE_TOOTH_DIALOG:{
                showProgressDialog((String) msg.obj);
            }break;
            case BLUE_TOOTH_TOAST:{
                dismissProgressDialog();
                ToastUtil.showText(MainActivity.this, (String) msg.obj);
            }break;
            case BLUE_TOOTH_SUCCESS:{
                dismissProgressDialog();
                ToastUtil.showText(MainActivity.this , "????????????" + (String)msg.obj + "??????");
                Intent intent = new Intent(MainActivity.this , ChatActivity.class);
                intent.putExtra(ChatActivity.DEVICE_NAME_INTENT , (String) msg.obj);
                startActivity(intent);
                //??????????????????
                close();

            }break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        st = (Switch) findViewById(R.id.st);

        st.setOnCheckedChangeListener(this);

        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        swipeRefreshLayout.setOnRefreshListener(onRefreshListener);

        recyclerAdapter = new RecyclerBlueToothAdapter(this);
        recyclerAdapter.setWifiData(list);
        recyclerAdapter.setOnItemClickListener(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(recyclerAdapter);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); //????????????????????????
        //???????????????????????????????????????
        if(mBluetoothAdapter.isEnabled()){
            //????????????
            st.setChecked(true);
            st.setText("???????????????");
        }else {
            st.setChecked(false);
            st.setText("???????????????");
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        mReceiver = new BlueToothReceiver(this);
        //????????????????????????
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);

        if(mBluetoothAdapter.isEnabled())
        {
            Log.e(TAG, "onResume: resumeStart" );
            mBluetoothChatService = BluetoothChatService.getInstance(handler);
            mBluetoothChatService.start();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(isChecked == true){
            if (mBluetoothAdapter.getState() != BluetoothAdapter.STATE_ON) {
                mBluetoothAdapter.enable();  //????????????
                st.setText("??????????????????");
                ToastUtil.showText(this, "??????????????????");
            }
        }else {
            if (mBluetoothAdapter.getState() != BluetoothAdapter.STATE_OFF) {
                mBluetoothAdapter.disable();  //????????????
                st.setText("????????????Wifi");
                ToastUtil.showText(this, "??????????????????");
            }
        }
        st.setClickable(false);
        if(timer == null || task == null) {
            timer = new Timer();
            task = new WifiTask();
            task.setChecked(isChecked);
            timer.schedule(task , 0 , 1000);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        close();
        mBluetoothChatService.stop();
    }

    private void close(){
        if(timer != null)
            timer.cancel();
        //????????????
        mBluetoothAdapter.cancelDiscovery();
        swipeRefreshLayout.setRefreshing(false);
        unregisterReceiver(mReceiver);
    }
    /**
     * RecyclerView Item ????????????
     * @param position
     */
    @Override
    public void onItemClick(int position) {
        showProgressDialog("??????????????????");
        BlueTooth blueTooth = list.get(position);
        connectDevice(blueTooth.getMac());
    }

    private void connectDevice(String mac) {
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mac);
        mBluetoothChatService.connectDevice(device);
    }

    /**
     * ???????????????
     * @param msg
     */
    public void showProgressDialog(String msg) {
        if (progressDialog == null)
            progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(msg);
        progressDialog.setCancelable(true);
        progressDialog.setIndeterminate(false);
        progressDialog.show();
    }

    /**
     * ?????????????????????
     */
    public void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private class WifiTask extends TimerTask {
        private boolean isChecked;
        public void setChecked(boolean isChecked){
            this.isChecked = isChecked;
        }

        @Override
        public void run() {
            if(isChecked){
                if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON)
                    handler.sendEmptyMessage(BluetoothAdapter.STATE_ON);
            }else
            {
                if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF)
                    handler.sendEmptyMessage(BluetoothAdapter.STATE_OFF);
            }
        }
    }

    private SwipeRefreshLayout.OnRefreshListener onRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {

            if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
                list.clear();
                //????????????????????????
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                if (pairedDevices.size() > 0) {
                    list.add(new BlueTooth("??????????????????" ,  BlueTooth.TAG_TOAST));
                    for (BluetoothDevice device : pairedDevices) {
                        Log.e(TAG, device.getName() + "\n" + device.getAddress());
                        list.add(new BlueTooth(device.getName() , device.getAddress() , ""));
                    }
                    list.add(new BlueTooth("??????????????????" , BlueTooth.TAG_TOAST));
                } else {
                    ToastUtil.showText(getApplicationContext(), "?????????????????????????????????");
                    list.add(new BlueTooth("??????????????????" , BlueTooth.TAG_TOAST));
                }
                recyclerAdapter.notifyDataSetChanged();
                //??????????????????
                mBluetoothAdapter.startDiscovery();
                ToastUtil.showText(MainActivity.this, "??????????????????");
            }else{
                swipeRefreshLayout.setRefreshing(false);
                ToastUtil.showText(MainActivity.this, "???????????????");
            }
        }
    };
    //????????????
    public void save(View view){
     /*   if(list  != null){
            SQLiteDatabase db = sqlHelper.getWritableDatabase();
            int row = Integer.parseInt(etRow.getText().toString());
            int line = Integer.parseInt(etLine.getText().toString());
            //??????????????????
            StringBuffer sb = new StringBuffer();
            sb.append("(");
            for(BlueTooth blueTooth : list){
                sb.append(blueTooth.getName() + " : " + blueTooth.getRssi());
                sb.append(" , ");
            }
            sb.replace(sb.toString().length() - 2 , sb.toString().length() - 1 , ")");
            //????????????????????????
            Cursor cursor = db.query("blue_tooth_table", null, "id=?", new String[]{line + ""}, null, null, null);
            //???????????????????????????????????????????????????
            if(!cursor.moveToNext()){
                ContentValues contentValues = new ContentValues();
                contentValues.put("id" , line);
                contentValues.put("i" + row , sb.toString());
                db.insert("blue_tooth_table" , null , contentValues);
            }else{
                ContentValues contentValues = new ContentValues();
                contentValues.put("i" + row, sb.toString());
                String [] whereArgs = {String.valueOf(line)};
                db.update("blue_tooth_table" , contentValues , "id=?" , whereArgs);
            }
            Toast.makeText(MainActivity.this , "????????????" , Toast.LENGTH_SHORT).show();
        }*/
    }

    /**
     * ????????????????????????
     * @param device
     * @param rssi
     */
    @Override
    public void getBlutToothDevices(BluetoothDevice device , int rssi) {
        list.add(new BlueTooth(device.getName() , device.getAddress() , rssi + ""));
        //??????UI
        recyclerAdapter.notifyDataSetChanged();
    }

    @Override
    public void searchFinish() {
        swipeRefreshLayout.setRefreshing(false);
        ToastUtil.showText(MainActivity.this , "????????????" );
    }
}
