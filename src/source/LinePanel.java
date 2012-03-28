/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package source;

import javax.microedition.lcdui.*;
/*
 *
 * @author Administrator
 */

public class LinePanel extends Panel {
//bgColor背景颜色, fgColor前景颜色, cnw文字宽度（长度）, volumes缓冲区大小(可以容纳多少行)

    protected int mColor, bgColor, fgColor, cnw, volumes;
    protected String content[];
    protected Graphics g;
    protected Image icon[];

    public LinePanel(int width, int height, int cnw, int volumes, Image icon[]) {
        super(width, height);
        this.cnw = cnw;
        this.volumes = volumes;
        this.g = panel.getGraphics();
        this.icon = icon;
    }

    public void setColor(int mColor, int bgColor, int fgColor) {
        this.mColor = mColor;
        this.bgColor = bgColor;
        this.fgColor = fgColor;
    }

    public void setContent(String content[]) {
        this.content = null;
        this.content = content;
    }

    protected void preparePanel() {
        g.setColor(bgColor);
        g.fillRect(0, 0, panel.getWidth(), panel.getHeight());
    }

    protected void drawText(int startIndex, int endIndex, int left_border) {
        try {
            g.setColor(fgColor);
            for (int i = startIndex, y = 0; i < endIndex; i++) {
                g.drawString(content[i % volumes], left_border, y, Graphics.TOP | Graphics.LEFT);
                y += cnw;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
