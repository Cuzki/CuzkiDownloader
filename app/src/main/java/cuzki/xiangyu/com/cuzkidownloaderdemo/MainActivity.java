package cuzki.xiangyu.com.cuzkidownloaderdemo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText mEtInput;

    private Button mBtnConfirm;

    private Button mBtnNext;

    private Context mContext;

    private int totle = 0;

    private List<HashMap<String, Integer>> mThreadRecorder;

    private boolean mIsDownLoading = false;

    private URL mUrl;

    private File mFile;

    private String mUrlString="";

    private ProgressBar mBar;

    private int mFileLength;
    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == 0) {
                mBar.setProgress(msg.arg1);
                if (msg.arg1 >= mFileLength) {
                    Toast.makeText(MainActivity.this, "下载完成！", Toast.LENGTH_SHORT).show();
                    totle = 0;
                    mBtnConfirm.setText("下载");
                    mThreadRecorder=new ArrayList<>();
                    mIsDownLoading=false;
                }
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        initView();
        initEvent();
        mThreadRecorder = new ArrayList<>();
        mEtInput.setText("http://imgsrc.baidu.com/forum/pic/item/3ac79f3df8dcd1004e9102b8728b4710b9122f1e.jpg");
    }

    private void initView() {
        mEtInput = (EditText) findViewById(R.id.et_input);
        mBtnConfirm = (Button) findViewById(R.id.btn_confirm);
        mBar=(ProgressBar) findViewById(R.id.process);
        mBtnNext = (Button) findViewById(R.id.btn_next);

    }

    private void initEvent() {
        mBtnConfirm.setOnClickListener(this);
        mBtnNext.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
//        if(!mUrlString.equals(mEtInput.getText().toString())){
//            mUrlString=mEtInput.getText().toString();
//            mThreadRecorder=new ArrayList<>();
//        }
        switch (v.getId()){
            case R.id.btn_confirm:
                if (mIsDownLoading) {
                    mIsDownLoading = false;
                    mBtnConfirm.setText("下载");
                    return;
                }
                mIsDownLoading = true;
                mBtnConfirm.setText("暂停");

                if (mThreadRecorder.size() == 0) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mUrlString = mEtInput.getText().toString();
                            totle = 0;
                            try {
                                mUrl = new URL(mUrlString);
                                HttpURLConnection connection = (HttpURLConnection) mUrl.openConnection();
                                connection.setConnectTimeout(6000);
                                connection.setRequestMethod("GET");
                                connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0; .NET CLR 2.0.50727)");
                                mFileLength = connection.getContentLength();
                                int code = connection.getResponseCode();
                                if (mFileLength < 0||code!=200) {
//                        Toast.makeText(mContext, "文件未获取到", Toast.LENGTH_SHORT).show();
                                    Log.i("cxy", "文件未获取到");
                                    return;
                                }
                                mBar.setMax(mFileLength);
                                mBar.setProgress(0);
                                mFile= new File(Environment.getExternalStorageDirectory(), getFileName(mUrlString));
                                RandomAccessFile accessFile = new RandomAccessFile(mFile, "rw");
                                accessFile.setLength(mFileLength);

                                int blockSize = mFileLength / 3;
                                for (int i = 0; i < 3; i++) {

                                    int begin = i * blockSize;
                                    int end = (i + 1) * blockSize;
                                    if (i == 2) {
                                        end = mFileLength;
                                    }
                                    HashMap<String,Integer> recorder=new HashMap<String, Integer>();
                                    recorder.put("begin",begin);
                                    recorder.put("end",end);
                                    recorder.put("finish",0);
                                    mThreadRecorder.add(recorder);
                                    Thread t = new Thread(new ChildDonwLoadThread(i, mUrl, begin, end, mFile));
                                    t.start();
                                }
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                                Toast.makeText(MainActivity.this, "URL 非法", Toast.LENGTH_SHORT).show();
                                Log.i("cxy", "URL 非法");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    }).start();
                }else{
                    for(int i=0;i<3;i++){
                        HashMap<String,Integer> recorder=mThreadRecorder.get(i);
                        int begin=recorder.get("begin");
                        int end=recorder.get("end");
                        int finish=recorder.get("finish");
                        Thread t=new Thread(new ChildDonwLoadThread(i, mUrl, begin+finish, end, mFile) {
                        });
                        t.start();
                    }
                }
                break;
            case R.id.btn_next:
                Intent intent=new Intent(mContext,RXDownloadActivity.class);
                startActivity(intent);
                break;
        }

    }

    class ChildDonwLoadThread implements Runnable {
        private int id;
        private URL url;
        private int begin;
        private File file;

        public ChildDonwLoadThread(int id, URL url, int begin, int end, File file) {
            this.id = id;
            this.url = url;
            this.begin = begin;
            this.end = end;
            this.file = file;
        }

        private int end;

        @Override
        public void run() {
            try {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Mozilla/4.0(compatiblelMSIE 8.0;Window NT 5.1;Trident/4.0;NET CLR 2.0.50727)");
                connection.setRequestProperty("Range", "bytes=" + begin + "-" + end);
                InputStream is = connection.getInputStream();
                byte[] buf = new byte[1024 * 1024];
                RandomAccessFile accessFile = new RandomAccessFile(file, "rw");
                accessFile.seek(begin);
                int len = 0;
                HashMap<String,Integer> recoder=mThreadRecorder.get(id);
                while ((len = is.read(buf)) != -1&&mIsDownLoading) {
                    accessFile.write(buf, 0, len);
                    recoder.put("finish", recoder.get("finish")+len);
                    updateProcess(len);
                }
                is.close();
                accessFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private String getFileName(String urlString) {
        if (TextUtils.isEmpty(urlString)) {
            return null;
        }
        return urlString.substring(urlString.lastIndexOf("/"));
    }

    synchronized private void updateProcess(int add) {
        totle += add;
        Log.i("cxy", "------totle=" + totle+"  length="+mFileLength);
        handler.obtainMessage(0,totle,0).sendToTarget();
    }

}
