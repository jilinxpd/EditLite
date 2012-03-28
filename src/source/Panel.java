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
 public class Panel {

    protected Image panel;

    Panel(int width, int height) {
        panel = Image.createImage(width, height);
    }

    Image getPanel() {
        return panel;
    }
    
}
