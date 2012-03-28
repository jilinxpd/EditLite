package source;

import javax.microedition.lcdui.*;

class SetKey extends Canvas {

    boolean undone;
    private int n, w, h, key[];
    private String content[];

    SetKey(int[] k) {
        setFullScreenMode(true);
        undone = true;
        n = 0;
        w = getWidth();
        h = getHeight();
        key = k;
        content = new String[]{"请按 上导航键", "请按 下导航键", "请按 左导航键", "请按 右导航键", "请按 OK键", "请按 左软键", "请按 右软键", "完成校检!"};
    }

    public void paint(Graphics g) {
        g.setColor(0xffffff);
        g.fillRect(0, 0, w, h);
        g.setColor(0xd2);
        if (n == 0) {
            g.drawString("校验键值", w / 2, h / 4, 33);
            g.drawString("请根据提示按下相应键", w / 2, h / 3, 33);
        } else if (n == 7) {
            g.drawString("保存(*)", 1, h - 1, 36);
            g.drawString("重新校验(#)", w, h - 1, 40);
        }
        if (content != null) {
            g.drawString(content[n], w / 2, h / 2, 33);
        }
    }

    protected void keyPressed(int i) {
        if (n < 7) {
            key[n] = i;
            n++;
            repaint();
        } else {
            if (i == 42) {//*
                content = null;
                key = null;
                undone = false;
            } else if (i == 35) {//#
                n = 0;
                repaint();
            }
        }
    }
}
