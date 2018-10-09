package de.thecode.android.tazreader.utils;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.ContextCompat;

import de.thecode.android.tazreader.data.FileCachePDFThumbHelper;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.secure.HashHelper;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import timber.log.Timber;


public class StorageManager extends ContextWrapper {

    public static final String TEMP     = "temp";
    public static final String PAPER    = "paper";
    public static final String RESOURCE = "resource";

    private static final String DOWNLOAD  = "download";
    private static final String IMPORT    = "import";
    private static final String APPUPDATE = "appUpdate";
    private static final String LOG       = "logs";



    private static StorageManager instance;

    private static class Volume {
        private final boolean isRemovable;
        private final boolean isEmulated;
        private final String root;

        public Volume(File file) {
            root = file.getAbsolutePath();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                isRemovable = Environment.isExternalStorageRemovable(file);
                isEmulated = Environment.isExternalStorageEmulated(file);
            } else {
                isRemovable = Environment.isExternalStorageRemovable();
                isEmulated = Environment.isExternalStorageEmulated();
            }
        }

        public File getFile() {
            return new File(root);
        }

        public static List<Volume> getVolumes(Context context){
            List<File> dirList = new ArrayList<>(Arrays.asList(ContextCompat.getExternalFilesDirs(context, null)));
            List<Volume> result = new ArrayList<>();
            for (File dir : dirList) {
                result.add(new Volume(dir));
            }
            Collections.sort(result, new Comparator<Volume>() {
                @Override
                public int compare(Volume o1, Volume o2) {
                    // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                    if (o1.isEmulated && !o2.isEmulated) return -1;
                    if (o2.isEmulated && !o1.isEmulated) return 1;
                    return 0;
                }
            });
            return result;
        }
    }

    public static StorageManager getInstance(Context context) {
        if (instance == null) instance = new StorageManager(context.getApplicationContext());
        return instance;
    }

    private final TazSettings settings;

    private StorageManager(Context context) {
        super(context);
        settings = TazSettings.getInstance(context);
        createNoMediaFileInDir(getCache(null));
        createNoMediaFileInDir(get(null));

        List<Volume> volumes = Volume.getVolumes(context);
        Timber.i("Found dirs");

    }

    private void createNoMediaFileInDir(File dir) {
        File noMediaFile = new File(dir, ".nomedia");
        if (!noMediaFile.exists()) {
            try {
                if (!noMediaFile.createNewFile()) Timber.w("cannot create file: %s", noMediaFile.getAbsolutePath());
            } catch (IOException e) {
                Timber.w(e);
            }
        }
    }

    public File get(String type) {
        File result = getExternalFilesDir(type);
        if (result != null) //noinspection ResultOfMethodCallIgnored
            result.mkdirs();
        return result;
    }


    public File getCache(String subDir) {
        File result = getExternalCacheDir();
        if (result != null) {
            if (subDir != null) result = new File(result, subDir);
            result.mkdirs();

        }
        return result;
    }

    public File getDownloadCache() {
        return getCache(DOWNLOAD);
    }

    public File getImportCache() {
        return getCache(IMPORT);
    }

    public File getUpdateAppCache() {
        return getCache(APPUPDATE);
    }

    public File getLogCache() {
        return getCache(LOG);
    }


    public File getDownloadFile(Paper paper) {
        try {
            return getDownloadFile(HashHelper.getHash(paper.getBookId(), HashHelper.UTF_8, HashHelper.SHA_1) + ".paper.zip");
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            Timber.e(e, "Error");
        }
        return null;
    }

    public File getDownloadFile(Resource resource) {
        return getDownloadFile(resource.getKey() + ".res.zip");
    }

    private File getDownloadFile(String key) {
        File downloadCache = getDownloadCache();
        if (downloadCache != null) return new File(getDownloadCache(), key);
        return null;
    }

    public File getPaperDirectory(Paper paper) {
        return new File(get(PAPER), paper.getBookId());
    }

    public File getResourceDirectory(String key) {
        return new File(get(RESOURCE), key);
    }

    public File getResourceDirectory(Resource resource) {
        if (resource == null) return null;
        return getResourceDirectory(resource.getKey());
    }


    public void deletePaperDir(Paper paper) {
        if (getPaperDirectory(paper).exists()) FileUtils.deleteQuietly(getPaperDirectory(paper));
//        Utils.deleteDir(getPaperDirectory(paper));
        new FileCachePDFThumbHelper(this, paper.getFileHash()).deleteDir();
    }

    public void deleteResourceDir(String key) {
        File dir = getResourceDirectory(key);
        if (dir.exists()) FileUtils.deleteQuietly(getResourceDirectory(key));
        //Utils.deleteDir(getResourceDirectory(key));
    }

}
