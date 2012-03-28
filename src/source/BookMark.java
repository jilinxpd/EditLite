/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package source;

import java.io.*;
import javax.microedition.rms.*;

public class BookMark {

    private RecordStore rs;
    private int ids[], var[], index[];
    private String file_path[], title[];

    void initBookMark() {
        byte[] data = null;
        int i, j, nums;
        RecordEnumeration re = null;
        DataInputStream dis = null;
        try {
            rs = RecordStore.openRecordStore("b", true);//打开存储管理系统
            re = rs.enumerateRecords(null, null, false);//获得枚举
            nums = rs.getNumRecords();//获得数目
            ids = new int[nums];
            index = new int[nums + 1];
            title = new String[nums];
            file_path = new String[nums];
            var = new int[5 * nums];
            nums = 0;
            while (re.hasNextElement()) {
                data = rs.getRecord(ids[nums] = (byte) re.nextRecordId());
                dis = new DataInputStream(new ByteArrayInputStream(data, 0, data.length));
                for (i = 5 * nums, j = i + 5; i < j; i++) {
                    var[i] = dis.readInt();
                }
                index[nums] = nums;
                title[nums] = dis.readUTF();
                file_path[nums++] = dis.readUTF();
                dis = null;
                data = null;
            }
            index[nums] = -1;//设置哨兵
        } catch (Exception x) {
        }
        data = null;
        re = null;
    }

    String[] getAll() {
        return title;
    }

    BookMarkNode getBookMark(int i) {//i传递进来的是屏幕上的序号
        int j = index[i];
        i = 5 * j;
        return new BookMarkNode(var[i], var[i + 1], var[i + 2], var[i + 3], var[i + 4], file_path[j]);
    }

    String getFilePath(int i) {
        return file_path[i];
    }

    public void destroy() {
        try {
            rs.closeRecordStore();
            rs = null;
            title = null;
            file_path = null;
            var = null;
            ids = null;
            index = null;
        } catch (Exception x) {
        }
    }

    void delete(int i) {//i传递进来的是屏幕上的序号
        try {
            rs.deleteRecord(ids[index[i]]);
        } catch (Exception x) {
        }
        deleteTitle(i);
    }

    private void deleteTitle(int t) {//t传递进来的是屏幕上的序号
        int i = 0, j = title.length, k = 0;
        String s[] = new String[j - 1];
        for (; i < j; i++) {
            if (i != t) {
                s[k++] = title[i];
            } else {
                title[i] = null;
                file_path[index[i]] = null;
            }
        }
        title = null;
        title = s;
        while ((index[t] = index[t + 1]) != -1) {
            t++;
        }
    }
}
