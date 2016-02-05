package cuzki.xiangyu.com.cuzkidownloaderdemo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

/**
 * Created by Cuzki on 2016/2/3.
 */
public class RXDownloadActivity extends Activity implements View.OnClickListener{

    /**
     * 用于发布下载进度的监听者
     */
    private PublishSubject<Integer> mProgressSubject=PublishSubject.create();

    private ProgressBar mProgress;
    private Button mBtnConfirm;
    private Activity mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rx_download);
        mContext=this;
        mProgress= (ProgressBar) findViewById(R.id.pb_bar);
        mBtnConfirm= (Button) findViewById(R.id.btn_confirm);
        mBtnConfirm.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        mProgress.setProgress(0);
        mProgressSubject.distinct().observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Integer>() {
            @Override
            public void onCompleted() {
                Toast.makeText(mContext,"下载完成",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Throwable e) {
                Toast.makeText(mContext,"下载出错",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNext(Integer integer) {
                mProgress.setProgress(integer);
            }
        });
        final String destination = "sdcardsoftboy.avi";
        observerableDownLoad("http://archive.blender.org/fileadmin/movies/softboy.avi", destination).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Boolean>() {
            @Override
            public void onCompleted() {
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
                File file = new File(destination);
                intent.setDataAndType(Uri.fromFile(file),"video/avi");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }

            @Override
            public void onError(Throwable e) {
                Toast.makeText(mContext,"下载失败",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNext(Boolean aBoolean) {

            }
        });

    }

    /**
     *
     * @param urlSting
     * @param loacalPath
     * @return
     */
    private Observable<Boolean> observerableDownLoad(final String urlSting, final String loacalPath) {
        return  Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                subscriber.onStart();
                try {
                    boolean result = downloadFile(urlSting, loacalPath);
                    if (result) {
                        subscriber.onNext(true);
                        subscriber.onCompleted();
                    } else {
                        subscriber.onError(new Throwable("Download failed."));
                    }
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });

        
    }



    private boolean downloadFile(String urlSting,String loacalPath){
        boolean result=false;
        HttpURLConnection connection = null;
        InputStream stream = null;
        FileOutputStream outputStream = null;
        OkHttpClient client=new OkHttpClient();
        long fileLength;
        try {

            Request request=new Request.Builder().url(urlSting).build();
            Response response=client.newCall(request).execute();
            fileLength=response.body().contentLength();
            stream=response.body().byteStream();
//            URL url=new URL(urlSting);
//            File file=new File(loacalPath);
//            connection= (HttpURLConnection) url.openConnection();
//            if(connection.getResponseCode()!=HttpURLConnection.HTTP_OK){
//                return false;
//            }
//            int fileLength=connection.getContentLength();
//            if(fileLength<0){
//                return false;
//            }
//            stream =connection.getInputStream();
            byte[] buf = new byte[4096];
            outputStream=new FileOutputStream(Environment.getExternalStorageDirectory()+"/"+loacalPath);
            long totle=0;
            int count=0;
            while((count=stream.read(buf))!=-1){
                totle+=count;
                if(mProgressSubject.hasObservers()){
                    mProgressSubject.onNext((int)(totle*100/fileLength));
                }
                outputStream.write(buf,0,count);
            }
            mProgressSubject.onCompleted();
            result=true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            mProgressSubject.onError(e);
        } catch (IOException e) {
            e.printStackTrace();
            mProgressSubject.onError(e);
        }finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
                mProgressSubject.onError(e);
            }
            if (connection != null) {
                connection.disconnect();
                mProgressSubject.onCompleted();
            }
        }
        return result;

    }


}
