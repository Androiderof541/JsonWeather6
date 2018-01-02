package sqlitedemo.administrator.example.com.jsonweather23_4;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity{
    private String cityname="广州";
    HttpURLConnection httpConn = null;
    InputStream din =null;
    //private EditText mCityname;
    private AutoCompleteTextView mCityname;
    private Button mSearch;
    private TextView mShowTV;
    String db_name = "weather";
    String db_path = "data/data/sqlitedemo.administrator.example.com.jsonweather23_4/database/";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("天气预报JSON");
        //复制数据库
        copydb();

        mSearch = (Button) findViewById(R.id.search);
        mShowTV = (TextView) findViewById(R.id.show_weather);
        mShowTV.setMovementMethod(ScrollingMovementMethod.getInstance());
        mSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mShowTV.setText("");
                cityname = mCityname.getText().toString();
                Toast.makeText(MainActivity.this,"正在查询天气...",Toast.LENGTH_LONG).show();
                GetJson gd = new GetJson(cityname);
                gd.start();
            }
        });
        mCityname = (AutoCompleteTextView) findViewById(R.id.cityname);
        mCityname.setThreshold(1);
        mCityname.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String str = s.toString();
                SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(db_path+db_name,null);
                Cursor cursor = null;
                try{
                    cursor = db.rawQuery("select area_name from weathers where area_name like '%"+str+"%'", null);
                }catch(Exception e){
                    e.printStackTrace();
                }
                List<String> list = new ArrayList<String>();
                String pro="";
                while(cursor.moveToNext()){
                    pro = cursor.getString(cursor.getColumnIndex("area_name"));
                    list.add(pro);

                }
                cursor.close();
                final ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_spinner_dropdown_item,list);
                mCityname.setAdapter(adapter);

            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    private final Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 1:
                    showData((String)msg.obj);
                    break;
            }
            super.handleMessage(msg);
        }
    };
    private  void showData(String jData){
        try {
            JSONObject jobj = new JSONObject(jData);
            JSONObject weather = jobj.getJSONObject("data");
            StringBuffer wbf = new StringBuffer();
            wbf.append("当前温度："+weather.getString("wendu")+"℃"+"\n");
            wbf.append("天气提示："+weather.getString("ganmao")+"\n");
            JSONArray jary = weather.getJSONArray("forecast");
            for(int i=0;i<jary.length();i++){
                JSONObject pobj = (JSONObject)jary.opt(i);
                wbf.append("日期："+pobj.getString("date")+"\n");
                wbf.append("最高温："+pobj.getString("high")+"\n");
                wbf.append("最低温："+pobj.getString("low")+"\n");
                wbf.append("风向："+pobj.getString("fengxiang")+"   ");
                String fengli = pobj.getString("fengli");
                int eq = fengli.indexOf("]]>");
                fengli = fengli.substring(9,eq);
                wbf.append("风力："+fengli+"\n");

                wbf.append("天气："+pobj.getString("type")+"\n");
            }
            mShowTV.setText(wbf.toString());
        }catch (Exception ex){
            ex.printStackTrace();
        }

    }
    private void copydb(){
        File db_file = new File(db_path+db_name);
        Log.i("weather","数据库创建");
        if(!db_file.exists()){   //如果第一次运行，文件不存在，那么就建立database目录，并从raw目录下复制weateher.db
            File db_dir= new File(db_path);
            if(!db_dir.exists()){  //如果database目录不存在，新建此目录
                db_dir.mkdir();
            }
            InputStream is = getResources().openRawResource(R.raw.weather);//获取输入流，就是随程序打包，放到raw目录下的weather.db文件
            try {
                OutputStream os = new FileOutputStream(db_path+db_name);//建立一个输出流
                byte[]buff = new byte[1024];//缓冲区大小
                int length = 0;
                while((length=is.read(buff))>0){
                    os.write(buff,0,length); //将buff写入os。写入长度为实践的buff的长度
                }
                os.flush(); //强制把缓冲区内容写入。确保缓存区所有的内容全部写入os
                os.close();
                is.close();
            }catch (Exception ee){
                ee.printStackTrace();
            }

        }

    }
    class GetJson extends Thread{
        private String urlstr =  "http://wthrcdn.etouch.cn/weather_mini?city=";
        public GetJson(String cityname){
            try{
                urlstr = urlstr+ URLEncoder.encode(cityname,"UTF-8");
            }catch (Exception ee){
                ee.printStackTrace();
            }
        }
        @Override
        public void run() {
            try {
                URL url = new URL(urlstr);
                httpConn = (HttpURLConnection)url.openConnection();
                httpConn.setRequestMethod("GET");
                din = httpConn.getInputStream();
                InputStreamReader in = new InputStreamReader(din);
                BufferedReader buffer = new BufferedReader(in);
                StringBuffer sbf = new StringBuffer();
                String line = null;
                while( (line=buffer.readLine())!=null) {
                    sbf.append(line);
                }
                Message msg = new Message();
                msg.obj = sbf.toString();
                msg.what = 1;
                handler.sendMessage(msg);
                Looper.prepare();
                Toast.makeText(MainActivity.this,"获取数据成功",Toast.LENGTH_LONG).show();
                Looper.loop();
            }catch (Exception ee){
                Looper.prepare();
                Toast.makeText(MainActivity.this,"获取数据失败，网络连接失败或输入有误",Toast.LENGTH_LONG).show();
                Looper.loop();
                ee.printStackTrace();
            }finally {
                try{
                    httpConn.disconnect();
                    din.close();

                }catch (Exception ee){
                    ee.printStackTrace();
                }
            }
        }
    }
}