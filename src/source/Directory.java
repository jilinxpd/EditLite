/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package source;

import java.util.*;
import javax.microedition.io.*;
import javax.microedition.io.file.*;

public class Directory {

    private boolean show_hidden;
    private int frames;
    private Enumeration e;


    Directory(int size) {
        frames = size;
        show_hidden = false;
    }

    public void switchHidden() {
        show_hidden = !show_hidden;
    }

 
    int readDirectory(String file_path, String content[]) {
        int m = 0, bottom = 0;
        try {
            e = null;
            if ("/".equals(file_path)) {
                e = FileSystemRegistry.listRoots();
            } else {
                FileConnection fc = (FileConnection) Connector.open("file:///" + file_path);
                e = fc.list("*", show_hidden);
                fc.close();
                fc = null;
                bottom = 1;
                content[0] = "..";
            }
        } catch (Exception x) {
        }
        String st, s[] = new String[frames];
        while (e.hasMoreElements()) {
            st = null;
            st = (String) e.nextElement();
            if (st.endsWith("/")) {
                content[bottom++] = st;
            } else {
                s[m++] = st;
            }
        }
        for (int i = 0; i < m; i++) {
            content[bottom++] = s[i];
        }
        s = null;
        return bottom;
    }

}
