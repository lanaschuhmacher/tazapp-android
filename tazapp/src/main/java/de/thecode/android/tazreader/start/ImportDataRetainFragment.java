package de.thecode.android.tazreader.start;

import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.os.Environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import de.thecode.android.tazreader.download.PaperDeletedEvent;
import de.thecode.android.tazreader.download.PaperDownloadFinishedEvent;
import de.thecode.android.tazreader.utils.BaseFragment;

/**
 * Created by mate on 13.04.2015.
 */
public class ImportDataRetainFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<List<ImportDirectoryLoader.ImportFileWrapper>> {

    private static final Logger log = LoggerFactory.getLogger(ImportDataRetainFragment.class);

    private static final String TAG = "RetainFragmentImport";
    private static final int DIRECTORY_LOADER = 3;
    List<ImportDirectoryLoader.ImportFileWrapper> data;

    private File rootDir;
    private File currentDir;

    private WeakReference<ImportDataCallback> callback;

    public ImportDataRetainFragment() {
    }

    public static ImportDataRetainFragment findOrCreateRetainFragment(FragmentManager fm, ImportDataCallback callback) {
        log.debug("");
        ImportDataRetainFragment fragment = (ImportDataRetainFragment) fm.findFragmentByTag(TAG);
        if (fragment == null) {
            log.debug("new");
            fragment = new ImportDataRetainFragment();
            fragment.setCurrentDir(Environment.getExternalStorageDirectory());
            fragment.setCallback(callback);
            fm.beginTransaction()
              .add(fragment, TAG)
              .commit();
        } else {
            fragment.setCallback(callback);
            log.debug("retained");
        }
        return fragment;
    }

    public void setCallback(ImportDataCallback callback) {
        this.callback = new WeakReference<>(callback);
    }

    private boolean hasCallback() {
        return callback.get() != null;
    }

    private ImportDataCallback getCallback() {
        return callback.get();
    }

    public void setCurrentDir(File currentDir) {
       log.debug("");
        if (rootDir == null) this.rootDir = currentDir;

        this.currentDir = currentDir;
    }

    public File getCurrentDir() {
       log.debug("");
        return currentDir;
    }

    public File getRootDir() {
        log.debug("");
        return rootDir;
    }

    public List<ImportDirectoryLoader.ImportFileWrapper> getData() {
        return data;
    }

    public void setData(List<ImportDirectoryLoader.ImportFileWrapper> data) {
        log.debug("");
        this.data = data;

        if (hasCallback()) getCallback().dataChanged();
    }

    public void restart() {
        getLoaderManager().restartLoader(DIRECTORY_LOADER, null, this);
    }

    public void destroyLoader() {getLoaderManager().destroyLoader(DIRECTORY_LOADER);}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log.debug("");
        setRetainInstance(true);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        log.debug("");
        getLoaderManager().initLoader(DIRECTORY_LOADER,null, this);
    }

    @Override
    public void onStop() {
        log.debug("");
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onDetach() {
        log.debug("");
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void onEventMainThread(PaperDownloadFinishedEvent event) {
        restart();
    }

    public void onEventMainThread(PaperDeletedEvent event) {
        restart();
    }

    @Override
    public Loader<List<ImportDirectoryLoader.ImportFileWrapper>> onCreateLoader(int id, Bundle args) {
       log.debug("");


        if (data==null)
            data = new ArrayList<>();
        data.clear();

        ImportDirectoryLoader.ImportFileWrapper waitIpf = new ImportDirectoryLoader.ImportFileWrapper();
        waitIpf.type = ImportDirectoryLoader.ImportFileWrapper.TYPE.WAIT;
        data.add(waitIpf);
        if (hasCallback()) getCallback().dataChanged();

        return new ImportDirectoryLoader(getActivity(), getRootDir(), getCurrentDir());
    }

    @Override
    public void onLoadFinished(Loader<List<ImportDirectoryLoader.ImportFileWrapper>> loader, List<ImportDirectoryLoader.ImportFileWrapper> data) {
        log.debug("");
        setData(data);

    }

    @Override
    public void onLoaderReset(Loader<List<ImportDirectoryLoader.ImportFileWrapper>> loader) {
        log.debug("loader: {}",loader);
    }

    public interface ImportDataCallback {
        public void dataChanged();
//        public void onStartLoading();
    }



}
