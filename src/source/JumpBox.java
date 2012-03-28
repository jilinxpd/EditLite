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
public class JumpBox {

    private TextField tf;
    private Form form;

    public void initJumpBox() {
        tf = new TextField("输入百分比(%)", null, 3, TextField.NUMERIC);
        form = new Form("跳转");
        form.append(tf);
        EditLite.ek.registControl(form);
    }

    public int getPercentage() {
        int percentage = Integer.parseInt(tf.getString());
        if (percentage > 100) {
            percentage = 100;
        }
        return percentage;
    }
}
