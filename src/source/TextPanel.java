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
public class TextPanel extends LinePanel {

    public TextPanel(int width, int height, int cnw, int volumes, Image icon[]) {
        super(width, height, cnw, volumes, icon);
    }

    public void refreshPanel(int startIndex, int length) {
        try {
            preparePanel();
            drawText(startIndex, startIndex + length, 1);
        } catch (Exception e) {

//            e.printStackTrace();
        }

    }

    public void refreshPanel(int startIndex, int length, int windowSelectedIndex, int windowSelectedRows, int startCharIndex, int endCharIndex) {
        preparePanel();
        setSelected(startIndex + windowSelectedIndex, windowSelectedIndex, windowSelectedRows, startCharIndex, endCharIndex, 1);
        drawText(startIndex, startIndex + length, 1);
    }

    private void setSelected(int selectedIndex, int windowSelectedIndex, int windowSelectedRows, int startCharIndex, int endCharIndex, int left_border) {
        int startX, endX;
        Font font;
        String s;
        font = Font.getFont(0, 0, 0);
        s = content[(selectedIndex + windowSelectedRows - 1) % volumes];
        endX = font.substringWidth(s, 0, endCharIndex);
        g.setColor(mColor);
        if (windowSelectedRows > 1) {
            g.fillRect(left_border, (windowSelectedIndex + windowSelectedRows - 1) * cnw, endX, cnw);
        }
        s = content[selectedIndex % volumes];
        startX = font.substringWidth(s, 0, startCharIndex);
        if (windowSelectedRows > 1) {
            endX = font.stringWidth(s);
            windowSelectedRows += (windowSelectedIndex - 2);
            for (; windowSelectedRows > windowSelectedIndex; windowSelectedRows--) {
                g.fillRect(left_border, windowSelectedRows * cnw, endX, cnw);
            }
        }
        g.fillRect(left_border + startX, windowSelectedIndex * cnw, endX - startX, cnw);
    }

    public Image clonePanel() {
        Image image = Image.createImage(panel.getWidth(), panel.getHeight());
        return image;
    }

    public void setPanel(Image panel,int bgColor) {
        g = null;
        this.panel = null;
        this.panel = panel;
        g = panel.getGraphics();
        this.bgColor=bgColor;
    }

        public void setPanel(Image panel) {
        g = null;
        this.panel = null;
        this.panel = panel;
        g = panel.getGraphics();
    }

}
