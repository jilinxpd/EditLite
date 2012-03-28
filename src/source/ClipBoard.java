package source;

import javax.microedition.lcdui.*;

class ClipBoard {//替换最近已访问的内容，其计时数最大

    private String content[];
    private byte count[];

    ClipBoard() {
        content = null;
        content = new String[8];
        count = null;
        count = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
    }

    String getContent(int i) {
        if (i < 8 && i >= 0) {
            count[i]++;
            return content[i];
        }
        return null;
    }

    void setContent(String s, int i) {
        if (i > 7 || i < 0) {
            i = 0;
            for (int j = 1, m = count[0]; j < 8; j++) {
                if (count[j] > m) {
                    m = count[i = j];
                }
            }
        }
        content[i] = null;
        content[i] = s;
        count[i] = -1;// 后面加为0
        for (i = 0; i < 8; i++) {
            count[i]++;
        }
    }

    void delContent(int i) {
        if (i < 8 && i >= 0) {
            content[i] = null;
            count[i]++;
        }
    }

    String[] getAll() {
        String[] s = new String[8];
        for (int n = 0; n < 8; n++) {
            s[n] = "剪贴" + (n + 1);
        }
        return s;
    }

    void listContent(List l) {
        for (int i = 0; i < 8; i++) {
            if (content[i] != null) {
                l.append(content[i], null);
            }
        }
    }
}
