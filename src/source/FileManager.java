/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package source;

import javax.microedition.io.*;
import javax.microedition.io.file.*;

class FileManager {

    private CopyFile cf;


    public void makeDir(String df, String dp) {
        try {
            FileConnection f = (FileConnection) Connector.open("file:///" + dp + "/");
            f.mkdir();
            f.close();
            f = null;
        } catch (Exception x) {
            EditLite.ek.alert("文件夹 " + df + " 已存在");
        }
    }

    public void saveNewFile(String textContent, String desDirName, String desFileName, int target_char_code) {
        cf = null;
        cf = new CopyFile();
        cf.set(null, desFileName, desDirName, textContent, target_char_code, 0, 0, 0, 0, false, false);
        cf.startCopy();
        cf = null;
    }

    public void saveEditedFile(String textContent, String srcDirName, String srcFileName, String desFileName, int offset, int skip, int rest, int target_char_code, int original_char_code, boolean to_del) {
        cf = null;
        cf = new CopyFile();
        cf.set(srcDirName + srcFileName, desFileName, srcDirName, textContent, target_char_code, original_char_code, offset, skip, rest, to_del, to_del);
        cf.startCopy();
        cf = null;
    }

    public boolean doesFileExist(String file_path) {
        boolean b = false;
        try {
            FileConnection fc = (FileConnection) Connector.open("file:///" + file_path);
            b = fc.exists();
            fc.close();
            fc = null;
        } catch (Exception x) {
        }
        return b;
    }

}
