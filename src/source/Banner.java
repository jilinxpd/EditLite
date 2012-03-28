/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package source;

import javax.microedition.lcdui.*;

/**
 *
 * @author Administrator
 */
public class Banner extends Panel {

    Banner(int width, int height) {
        super(width, height);
    }

    void createPanel(int bgColor, int fgColor, String title, boolean isAlertPanel) {
        Graphics g = panel.getGraphics();
        int width = panel.getWidth(), height = panel.getHeight();
        g.setColor(bgColor);
        g.fillRect(0, 0, width, height);
        g.setColor(fgColor);
        if (isAlertPanel) {
            g.drawRect(0, 0, width-1, height-1);
            g.drawString(title, width / 2, height / 2, Graphics.HCENTER | Graphics.BASELINE);
        } else {
            g.drawLine(0, height-1, width, height-1);
            g.drawString(title, 1, 1, Graphics.TOP | Graphics.LEFT);
        }
    }

    void createPanel(int bgColor, int fgColor, String option_1, String option_2) {
        Graphics g = panel.getGraphics();
        int width = panel.getWidth(), height = panel.getHeight();
        g.setColor(bgColor);
        g.fillRect(0, 0, width, height);
        g.setColor(fgColor);
        g.drawLine(0, 0, width, 0);
        g.drawString(option_1, 1, 1, Graphics.TOP | Graphics.LEFT);
        g.drawString(option_2, width, 1, Graphics.TOP | Graphics.RIGHT);
    }
}
