/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package source;

import javax.microedition.lcdui.*;

/**
 *
 * @author xpd
 */
public class DirectoryPanel extends LinePanel {

    private int left_border;

    DirectoryPanel(int width, int height, int cnw, int volumes, Image icon[]) {
        super(width, height, cnw, volumes, icon);
    }

    void setLeftBorder(int increment) {
        left_border += increment;
    }

    void refreshPanel(int startIndex, int length, int selectedIndex, boolean isBrowsingFile, boolean multi_select[]) {
        preparePanel();
        int x = cnw + 3, y = 0;//Y坐标，用于每行文字的Y坐标
        int i, j;
        int width = panel.getWidth() - cnw - 9;
        length += startIndex;
        //如果多选了，则要画出多选标记
        if (multi_select != null) {
            g.setColor((bgColor + fgColor) / 2);
            y = 0;
            for (i = startIndex; i < length; i++) {
                j = i % volumes;
                if (multi_select[j]) {
                    g.fillRect(x, y, width, cnw);
                }
                y += cnw;
            }
            y = 0;
        }
        //标记当前选择项
        g.setColor(mColor);
        g.fillRect(x, selectedIndex * cnw, width, cnw);
        //如果正在浏览文件，则要显示文件类型图标
        if (isBrowsingFile) {
            String file_name;
            for (i = startIndex; i < length; i++) {
                file_name = content[i % volumes].toLowerCase();
                if (file_name.endsWith("/") || file_name.equals("..")) {
                    j = 0;
                } else if (file_name.endsWith(".txt")) {
                    j = 1;
                } else {//不识别类型
                    j = 2;
                }
                g.drawImage(icon[j], 2, y, 20);
                y += cnw;
            }
        } else {//不是文件管理，说明是记事本，历史记录，书签
            for (i = startIndex; i < length; i++) {
                g.drawImage(icon[3], 2, y, 20);
                y += cnw;
            }
        }
        drawText(startIndex, length, cnw + 5);
        left_border = cnw + 7;
    }

    void DisplayFullFileName(int selectedIndex, String full_file_name) {
        int width = panel.getWidth();
        g.setClip(cnw + 3, selectedIndex * cnw, width - cnw - 9, cnw);
        g.setColor(mColor);
        g.fillRect(0, selectedIndex * cnw, width, cnw);
        g.setColor(fgColor);
        g.drawString(full_file_name, left_border, selectedIndex * cnw, 20);
        g.setClip(0, 0, width, panel.getHeight());
    }
}
