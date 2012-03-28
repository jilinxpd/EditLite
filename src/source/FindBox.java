package source;

import javax.microedition.lcdui.*;

/**
 *
 * @author xpd
 */
public class FindBox {

    private TextField tf;
    private Form form;
    private ChoiceGroup dcg, ocg;
    ///////////////////////////////////////////
    String content[];
    int num;
    int index;

    //@TODO 实现正则表达式解析器
    public void initFindBox() {
        dcg = new ChoiceGroup("方向", ChoiceGroup.EXCLUSIVE, new String[]{"向下", "向上"}, null);
        ocg = new ChoiceGroup(null, ChoiceGroup.MULTIPLE, new String[]{"忽略大小写"}, null);
        tf = new TextField("输入关键词", null, 8, TextField.ANY);
        form = new Form("查找");
        form.append(tf);
        form.append(dcg);
        form.append(ocg);
        EditLite.ek.registControl(form);
    }

    public void startFind() {
        int direction = dcg.getSelectedIndex();
        boolean option[] = new boolean[1];
        ocg.getSelectedFlags(option);
        String pattern = tf.getString();
    }

    //@TODO 未完成的函数
    private int[] FindString(String s) {
        StringBuffer sb = new StringBuffer(512);
        String des;
        int anchor = 0;
        int i = 0, j = index;
        while (i > 0) {
            for (i = 0; i < num + 1; i++) {
                sb.append(content[j]);
                j++;
            }
            des = sb.toString();
            anchor = des.indexOf(s);
            if (anchor >= 0) {
                break;
            }
            if (j > 99999) {
                return null;
            }
            j--;
        }
        int length[] = new int[num + 1];
        for (i = 0; i < num + 1; i++) {
            length[i] += content[j].length();
            length[i + 1] = length[i];
            j++;
        }

        int low = 0, high = num, m = 0;
        while (low <= high) {
            m = (low + high) / 2;
            if (length[m] == anchor) {
                break;
            } else if (length[m] < anchor) {
                low = m + 1;
            } else {
                high = m - 1;
            }
        }

        return new int[]{m, anchor - length[m]};


    }

    public void destroy() {
        tf = null;
        dcg = null;
        ocg = null;
        form = null;

    }
}
