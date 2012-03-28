package source;

import javax.microedition.lcdui.*;

class Menu extends Canvas {

    private boolean refresh = true;
    private int screen_height, cnw, menu_level;//menu_level初始是-1
    private int menuWidth[], extend[], selectedIndex[], color[], key[];
    private String[][] menuItems;//存放3个级别的菜单的选项字符串
    private Image menuImage[];//存放3个级别的菜单的图片
    private Image bgImage;
    private Graphics graphics;

    Menu(int cnw, int screen_height, int[] color, int[] key, Image bgImage) {
        this.cnw = cnw;
        this.screen_height = screen_height;
        this.color = color;
        this.key = key;
        this.bgImage = bgImage;
        //本程序只用到三级菜单，所以下面的数组长度都是3
        selectedIndex = new int[3];
        menuItems = new String[3][];
        menuImage = new Image[3];
        extend = new int[3];
        menuWidth = new int[3];
    }

    protected void paint(Graphics a) {
        setFullScreenMode(true);
        switch (menu_level) {
            case -1:
                return;
            case 0:
                a.drawImage(bgImage, 0, 0, Graphics.LEFT | Graphics.TOP);
                a.drawImage(menuImage[0], 1, screen_height - cnw - 2, Graphics.LEFT | Graphics.BOTTOM);
                return;
            case 1:
                if (refresh) {
                    refresh = false;
                    a.drawImage(bgImage, 0, 0, Graphics.LEFT | Graphics.TOP);
                    a.drawImage(menuImage[0], 1, screen_height - cnw - 2, Graphics.LEFT | Graphics.BOTTOM);
                }
                a.drawImage(menuImage[1], 2 * cnw + 5, screen_height - (5 - selectedIndex[0]) * cnw, Graphics.LEFT | Graphics.VCENTER);
                return;
            case 2:
                a.drawImage(menuImage[2], 4 * cnw + 10, screen_height - (6 - selectedIndex[0] - selectedIndex[1]) * cnw, Graphics.LEFT | Graphics.VCENTER);
                return;
        }
    }

    protected void keyPressed(int i) {
        try {

            if (i == 50 || i == key[0]) {// -1
                if (--selectedIndex[menu_level] < 0) {
                    selectedIndex[menu_level] += menuItems[menu_level].length;
                }
                flushImage(selectedIndex[menu_level]);
                repaint();
            } else if (i == 56 || i == key[1]) {// -2
                if (++selectedIndex[menu_level] == menuItems[menu_level].length) {
                    selectedIndex[menu_level] = 0;
                }
                flushImage(selectedIndex[menu_level]);
                repaint();
            } else if (i == 53 || i == key[4]) {// -5
                i = menu_level;
                EditLite.ek.keyAction();
                if (i == menu_level) {
                    menu_level = 0;// 本应为-1，但switchMenu()中会减一
                    switchMenu(true);
                } else {
                    repaint();
                }
            } else if (i == 52 || i == key[2]) {// -3
                if (menu_level > 0) {
                    switchMenu(refresh = true);
                    repaint();
                }
            } else if (i == 54 || i == key[3]) {// -4
                if (((1 << selectedIndex[menu_level]) & extend[menu_level]) > 0) {
                    EditLite.ek.keyAction();
                    repaint();
                }
            } else if (i == key[6]) {// -7
                menu_level = 0;// 本应为-1，但switchMenu()中会减一
                switchMenu(true);
                EditLite.ek.changeDisplay(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initialMenu(String s[], int w, int e) {
        //s是命令列表，w是菜单宽度，e是下级菜单扩展标记
        int n;
        if (menu_level < 0) {
            n = 0;
        } else {
            n = menu_level;
        }
        menuItems[n] = null;
        menuItems[n] = s;
        menuWidth[n] = w;
        extend[n] = e;
    }

    public void initialImage(int selected_index) {
        if (menu_level < 0) {
            menu_level = 0;
        }
        graphics = null;
        menuImage[menu_level] = null;
        menuImage[menu_level] = Image.createImage(menuWidth[menu_level] * cnw + 5, menuItems[menu_level].length * cnw + 5);
        graphics = menuImage[menu_level].getGraphics();
        //初始化当前菜单的被选号
        selectedIndex[menu_level] = selected_index;
        flushImage(selected_index);
        repaint();

    }

    //当菜单级别发生改变时
    public void switchMenu(boolean backward) {
        if (backward) {//如果是回退
            //先清空当前的菜单
//            menuItems[menu_level] = null;
            menuImage[menu_level] = null;
            graphics = null;
            menu_level--;//先递减1
            if (menu_level >= 0) {
                graphics = menuImage[menu_level].getGraphics();
            }
        } else {//如果是前进
            ++menu_level;
        }
    }

    //获得当前菜单级别，注意menu_level为0不代表第一级菜单正在显示，因为菜单不显示时menu_level初始也是0
    public int getMenuLevel() {
        return menu_level;
    }

    //获得第i级菜单所选项的序号
    public int getSelectedIndex(int i) {
        if (i < 0) {
            return 0;
        }
        return selectedIndex[i];
    }

    public void resetMenuLevel() {
        menu_level = -1;//menu_level初始是-1
    }    //填充菜单图片

    private void flushImage(int selected_index) {
        int n = menuItems[menu_level].length, wi = cnw, width = menuImage[menu_level].getWidth(), height = menuImage[menu_level].getHeight(), t = 2 + (n - 1) * wi;
        graphics.setColor(color[1]);//背景颜色
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(color[0]);
        graphics.fillRect(0, 0, 4, height);//顶部装饰条
        graphics.fillRect(0, selected_index * wi, width, wi + 4);//被选择项背景颜色
        graphics.setColor(0);
        graphics.drawRect(0, 0, width - 1, height - 1);//菜单边框
        graphics.setColor(color[2]);
        if (extend[menu_level] > 0) {//画出小三角，标识其有下级菜单
            height = (2 * menuWidth[menu_level] - 1) * wi / 2 + 6;
            int s = 0;
            while (s < 8) {
                if ((extend[menu_level] & (1 << s)) > 0) {
                    graphics.fillTriangle(height, s * wi + 3, height, (s + 1) * wi - 3, width - 2, (2 * s + 1) * wi / 2);
                }
                s++;
            }
        }
        while (n > 0) {//画出选项的字符串
            graphics.drawString(menuItems[menu_level][--n], 8, t, Graphics.TOP | Graphics.LEFT);
            t -= wi;
        }
    }
}
