package cuzki.xiangyu.download;
import cuzki.xiangyu.download.IDownloadServiceCallback;

interface IDownloadService {

	int getDownloadState(String urlString);
	void registDownloadCallback(IDownloadServiceCallback callback);
	void unregistDownloadCallback(IDownloadServiceCallback callback);
	List<String> getDownloadingUrlStrings();

}