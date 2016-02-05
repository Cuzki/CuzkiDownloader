package cuzki.xiangyu.com.cuzkidownloaderdemo;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import cuzki.xiangyu.download.IDownloadService;
import cuzki.xiangyu.download.IDownloadServiceCallback;

/**
 * Created by Cuzki on 2016/2/5.
 */
public class DownloadService extends Service {

    private RemoteCallbackList<IDownloadServiceCallback> mCallBack=new RemoteCallbackList<IDownloadServiceCallback>();

    private ReentrantLock mLock=new ReentrantLock();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    private IDownloadService.Stub mBinder= new IDownloadService.Stub() {
        @Override
        public int getDownloadState(String urlString) throws RemoteException {
            return 0;
        }

        @Override
        public void registDownloadCallback(IDownloadServiceCallback callback) throws RemoteException {
            mLock.lock();
            mCallBack.register(callback);
            mLock.unlock();
        }

        @Override
        public void unregistDownloadCallback(IDownloadServiceCallback callback) throws RemoteException {
            mLock.lock();
            mCallBack.unregister(callback);
            mLock.unlock();
        }

        @Override
        public List<String> getDownloadingUrlStrings() throws RemoteException {
            return null;
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
