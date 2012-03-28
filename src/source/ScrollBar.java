/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package source;

/**
 *
 * @author Administrator
 */
import javax.microedition.lcdui.*;

public class ScrollBar extends Panel {

    private int bgColor,  fgColor,  window_volumes,  total_volumes;
    private Graphics g;

    ScrollBar(int width, int height) {
        super(width, height);
        g = panel.getGraphics();
    }

    //设置前景色和背景色
    void setColor(int bgColor, int fgColor) {
        this.bgColor = bgColor;
        this.fgColor = fgColor;
    }

    //初始化进度条
    void setIterator(int begin, int window_volumes, int total_volumes) {
        this.total_volumes = total_volumes > 0 ? total_volumes : 1;
        this.window_volumes = window_volumes;
        refreshIterator(begin);
    }

    //刷新进度条
    void refreshIterator(int begin) {
        int width = panel.getWidth(), height = panel.getHeight();
        g.setColor(bgColor);
        g.fillRect(0, 0, width, height);
        g.setColor(fgColor);
        g.fillRect(0, begin * height / total_volumes, width, window_volumes * height / total_volumes);
        g.drawRect(0, 0, width, height);
    }
}
