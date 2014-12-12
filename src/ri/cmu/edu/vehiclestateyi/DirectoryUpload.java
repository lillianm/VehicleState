package ri.cmu.edu.vehiclestateyi;

import android.os.Handler;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;

import android.util.Log;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: lars
 * Date: 9/28/13
 * Time: 4:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class DirectoryUpload {

    File uploadDir;

    
    DbxFileSystem dbxFileSystem;
    DbxAccountManager dbxAccountManager;

    public DirectoryUpload(DbxAccountManager accountManager) throws DbxException.Unauthorized {
        dbxAccountManager = accountManager;
        if (accountManager != null) {
        	dbxFileSystem = DbxFileSystem.forAccount(dbxAccountManager.getLinkedAccount());
        }
    }

//    public void setUploadDir(String dir) {
//        if (!dir.isDirectory()) {
//            throw new IllegalArgumentException();
//        }
//        uploadDir = dir;
//    }

    public void setUploadDir(String dir) {
        uploadDir = new File(dir);
        if (!uploadDir.isDirectory()) {
        	Log.e("FileUpload",dir);
            //throw new IllegalArgumentException();
        }
    }

    private void deleteR(File f) {
        if (f.isFile()) {
            f.delete();
            return;
        }
        File[] dirList = f.listFiles();
        for (int i = 0; i < dirList.length; i++) {
            File d = dirList[i];
            deleteR(d);
            d.delete();
        }
    }

    public void delete() {
        File[] dirList = uploadDir.listFiles();
        for (int i = 0; i < dirList.length; i++) {
            deleteR(dirList[i]);
            dirList[i].delete();
        }
    }

    private void uploadSubDir(String fileDir) {
        File[] dirList = new File(uploadDir.getPath() + File.separator + fileDir).listFiles();
        if (dirList == null) {
            return;
        }
        Log.e("dirlist", uploadDir.getPath() + ", " + fileDir);

        for (int i = 0; i < dirList.length; i++) {
            File subDir = dirList[i];
            if (!subDir.isDirectory()) {
                continue;
            }
            File[] data = subDir.listFiles();

            for (int j = 0; j < data.length; j++) {

                File in = data[j];
                if (!in.isFile()) {
                    continue;
                }

                DbxFile out;
                try {
                    out = dbxFileSystem.create(new DbxPath(fileDir + File.separator + in.getName()));
                } catch (DbxException e) {
                    Log.e("DirectoryUpload", "Error creating " + in.getName() + " : " + e.getMessage());
                    continue;
                }


                Log.e("completed", in.getName());
            }
        }

    }

    public void upload(Handler h) {
        MainActivity.interruptedUpload = false;
        MainActivity.uploading = true;
        h.sendEmptyMessage(0);
        uploadSubDir(Protocol.UPLOAD_IMAGE_DIR);
        uploadSubDir(Protocol.UPLOAD_VIDEO_DIR);

        if (!MainActivity.interruptedUpload) {
            MainActivity.upToDate = true;
        }
        MainActivity.uploading = false;
        h.sendEmptyMessage(0);
    }
}
