package source;

import java.util.*;
import javax.microedition.lcdui.*;

class ViewCanvas extends Canvas {

    private boolean not_full_screen, not_waiting;
    private int frames, width, height, display_panel_height;
    private int cnw, rows, mode, color[], key[];
    private int /*coordinate_Y,*/ YYY[];//处于中间的图片的Y坐标
    private int first_panel_index;//第一个图片的指针，用于标记第一个图片
    private int Y_min;
    private String headline, file_path, content[];
    private EditLite ebook;
    private Image bgImage, text_panel_image[];
    private Banner top_banner, bottom_banner, alert_panel;
    private DirectoryPanel directory_panel;
    private TextPanel text_panel;
    private ScrollBar scroll_bar;
    private Directory directory;
    private Graphics graphics;
    private Text text;
    private TextController tc;
    ListController list;
    //mode常量
    public static final int MODE_NONE = 0;//空模式，不显示任何内容
    public static final int MODE_DEFAULT = 1;//默认的模式，显示文本面板
    public static final int MODE_OK = 2;//显示操作成功提示信息
    public static final int MODE_BOTTOM_MOST = 3;//到达底部，显示阅读完毕
    public static final int MODE_TOP_MOST = 4;//显示到达顶部
    public static final int MODE_WAITING = -1;//显示请等待
//    int bgColor[] = new int[]{0xff9999, 0x99ff99, 0x9999ff};
    int bgColor[] = new int[]{0xf0f0fa, 0xf0f0fa, 0xf0f0fa};

    public ViewCanvas() {
        setFullScreenMode(true);
        cnw = Font.getFont(0, 0, 16).charWidth('田') + 2;
        width = getWidth();
        height = getHeight();
        rows = (height + 6) / cnw;
        frames = 30 * rows;
        display_panel_height = rows * cnw;
        not_full_screen = true;
        not_waiting = true;
        mode = MODE_NONE;
    }

    //实例化一些大对象
    public void postCreateReadCanvas(int color[], int key[], Image images[]) {
        ebook = EditLite.ek;//便于直接引用
        this.color = color;
        this.key = key;
        directory = new Directory(frames);
        text = new Text(width, rows, frames);
        list = new ListController(rows - 2);
        //生成面板
        top_banner = new Banner(width, cnw);//顶部边框
        bottom_banner = new Banner(width, cnw);//底部边框
        scroll_bar = new ScrollBar(5, height - 2 * cnw);//滚动条
        directory_panel = new DirectoryPanel(width, display_panel_height, cnw, frames, images);//文件夹浏览面板
        text_panel = new TextPanel(width, display_panel_height, cnw, frames + rows, images);//阅读面板
        refreshColor();
        text_panel_image = new Image[3];
        text_panel_image[0] = text_panel.getPanel();
        text_panel_image[1] = text_panel.clonePanel();
        text_panel_image[2] = text_panel.clonePanel();
        //生成背景面板
        bgImage = Image.createImage(width, height);
        graphics = bgImage.getGraphics();
        YYY = new int[3];
    }

    public void paint(Graphics a) {
        setFullScreenMode(true);
        if (mode == MODE_DEFAULT) {
            a.drawImage(bgImage, 0, 0, Graphics.LEFT | Graphics.TOP);
        } else if (mode > MODE_DEFAULT || mode < MODE_NONE) {
            if (null == alert_panel) {
                alert_panel = new Banner(6 * cnw, 3 * cnw);
            }
            //提示框前景色
            int fgColor = color[2];
            String title = null;
            if (mode == MODE_OK) {
                title = "添加成功!";
            } else if (mode == MODE_BOTTOM_MOST) {
                title = "看完喽!";
            } else if (mode == MODE_TOP_MOST) {
                title = "上面没啦!";
            } else if (mode == MODE_WAITING) {
                title = "请稍等…";
            }
            alert_panel.createPanel(color[0], fgColor, title, true);
            a.drawImage(alert_panel.getPanel(), width / 2, height / 2, Graphics.HCENTER | Graphics.VCENTER);
            if (mode > MODE_NONE) {//正数表示一定时间后提示框自动消失，负数则表示提示框不会自动消失，需要调用者执行
                new Thread(new Runnable() {

                    public void run() {
                        try {
                            Thread.sleep(400);
                        } catch (Exception x) {
                        }
                        mode = MODE_DEFAULT;
                        repaint();
                    }
                }).start();
            }
        }
    }

    protected void keyPressed(int i) {
        if (i == key[5]) {// -6 如果按下选项键
            ebook.menu.initialImage(0);
            ebook.changeDisplay(ebook.menu);
        } else if (i == Canvas.KEY_NUM3) {//如果按下时间键
            StringBuffer s = new StringBuffer("现在是");
            s.append(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
            s.append(":");
            s.append(Calendar.getInstance().get(Calendar.MINUTE));
            ebook.alert(s.toString());
            s = null;
        } else {
            switch (ebook.scene) {
                case Scene.READ_TXT://如果在看txt
                    tkey(i);
                    break;
                default://如果在浏览文件夹
                    dkey(i);
            }
        }
        System.gc();
    }

    private void tkey(int i) {//阅读模式的按键响应函数
        if (i == 50 || i == key[0]) {// -1向上
            if (YYY[first_panel_index] >= 0) {
                if (not_waiting) {
                    mode = MODE_TOP_MOST;//显示提示框，上面没有了
                    repaint();
                }
                return;
            } else {
                YYY[0] += cnw;
                YYY[1] += cnw;
                YYY[2] += cnw;
                i = YYY[first_panel_index] + display_panel_height / 2;
                if (i > 0 && i <= cnw) {//在跨越height/2这一分界线时，要滚动容器，刷新容器里的内容
                    new Thread(new Runnable() {

                        public void run() {
                            not_waiting = false;
                            refreshLast();
                            not_waiting = true;
                        }
                    }).start();
                }
            }
        } else if (i == 56 || i == key[1]) {// -2向下
            if (YYY[first_panel_index] <= Y_min) {
                mode = MODE_BOTTOM_MOST;//显示提示框，读完了
                repaint();
                return;
            } else {
                YYY[0] -= cnw;
                YYY[1] -= cnw;
                YYY[2] -= cnw;
                i = YYY[first_panel_index] + 5 * display_panel_height / 2 - height;
                if (i < 0 && i + cnw >= 0) {
//                    System.out.println("aaaaaaaaaaaaaaaa");
                    new Thread(new Runnable() {

                        public void run() {
                            refreshNext();
                        }
                    }).start();
                }
            }
        } else if (i == 52 || i == key[2]) {// -3向上翻页
            if (YYY[first_panel_index] >= 0) {
                if (not_waiting) {
                    mode = MODE_TOP_MOST;//显示提示框，上面没有了
                    repaint();
                }
                return;
            } else if (YYY[first_panel_index] >= -display_panel_height) {
                YYY[first_panel_index] = 0;
                YYY[(first_panel_index + 1) % 3] = display_panel_height;
                YYY[(first_panel_index + 2) % 3] = 2 * display_panel_height;
                new Thread(new Runnable() {

                    public void run() {
                        not_waiting = false;
                        refreshLast();
                        not_waiting = true;
                    }
                }).start();
            } else {//&&coordinate+height/2>0
                YYY[first_panel_index] = -display_panel_height;
                YYY[(first_panel_index + 1) % 3] = 0;
                YYY[(first_panel_index + 2) % 3] = display_panel_height;
            }
        } else if (i == 54 || i == key[3]) {// -4向下翻页
            if (YYY[first_panel_index] <= Y_min) {
                mode = MODE_BOTTOM_MOST;//显示提示框，读完了
                repaint();
                return;
            } else if (YYY[first_panel_index] > -display_panel_height) {
                YYY[first_panel_index] = -display_panel_height;
                YYY[(first_panel_index + 1) % 3] = 0;
                YYY[(first_panel_index + 2) % 3] = display_panel_height;
            } else {//if (coordinate_Y > -height)
                YYY[first_panel_index] = -2 * display_panel_height;
                YYY[(first_panel_index + 1) % 3] = -display_panel_height;
                YYY[(first_panel_index + 2) % 3] = 0;
                new Thread(new Runnable() {

                    public void run() {
                        refreshNext();
                    }
                }).start();
            }
        } else if (i == key[6]) {// -7返回
            if (ebook.scene == Scene.READ_TXT) {//阅读
                text.destroy();//清理垃圾
            }
            not_full_screen = true;
            ebook.back();
            return;
        } else if (i == 48) {//数字键0切换全屏
            not_full_screen = !not_full_screen;
//            new Thread(new Runnable() {
//
//                public void run() {
//                    int j = 0;
//                    while (j < 500) {
//                        tkey(54);
//                        j++;
//                        try {
//                            Thread.sleep(50);
//                        } catch (InterruptedException ex) {
//                            ex.printStackTrace();
//                        }
//                    }
//
//                }
//            }).start();
        } else if (i == 55) {
//            int j = 0;
//            i = text.FFF();
//            first_panel_index = 0;
//            while (j < 3) {
//                text_panel.setPanel(text_panel_image[j]);
//                text_panel.refreshPanel(i, rows);
//                j++;
//                i += rows;
//            }
//            YYY[0] = -display_panel_height;
//            YYY[1] = 0;
//            YYY[2] = display_panel_height;
        }
        createBgImage();
        repaint();
    }

    private void dkey(int i) {//文件管理模式的按键响应函数
        if (i == 50 || i == key[0]) {// -1向上
            i = list.last();
        } else if (i == 56 || i == key[1]) {// -2向下
            i = list.next();
        } else if (i == 52 || i == key[2]) {// -3
            i = list.skipLast();
        } else if (i == 54 || i == key[3]) {// -4
            i = list.skipNext();
        } else if (i == 53 || i == key[4]) {// -5
            ebook.keyAction();
            return;//这里一定要返回，否则下面会generateBgImage();repaint();这会出错的，因为文件还没初始化，还没读取
        } else if (i == key[6]) {// -7
            if (ebook.scene == Scene.EXPLORE_FILES) {//在浏览文件夹
                new Thread(new Runnable() {

                    public void run() {
                        ebook.changeDirectory("..");
                    }
                }).start();
            } else {
                ebook.quit();
            }
            return;
        } else if (i == 55) {//左移文件名
            if (list.getNumberOfRows() == 0) {
                return;
            }
            directory_panel.setLeftBorder(-5);
            directory_panel.DisplayFullFileName(list.getWindowSelectedIndex(), getSelectedItem());
            i = -1;//防止下面又refreshPanel
        } else if (i == 57) {//右移文件名
            if (list.getNumberOfRows() == 0) {
                return;
            }
            directory_panel.setLeftBorder(5);
            directory_panel.DisplayFullFileName(list.getWindowSelectedIndex(), getSelectedItem());
            i = -1;//防止下面又refreshPanel
        } else if (i == 48) {//切换显示隐藏
            new Thread(new Runnable() {

                public void run() {
                    directory.switchHidden();
                    showDir(file_path, null);
                }
            }).start();
            return;
        } else //if (ebook.scene == Scene.EXPLORE_FILES) {
        if (i == 42) {//*单项选择
            i = list.setMultiSelect(list.getSelectedIndex());
            if (ebook.scene == Scene.EXPLORE_FILES && list.getSelectedIndex() == 0) {
                list.setMultiSelect(0);
            }
        } else if (i == 35) {//#全部取消选择
            i = list.setMultiSelect(-1);
        } else if (i == 49) {//1全部反向选择
            i = list.setMultiSelect(-2);
            if (ebook.scene == Scene.EXPLORE_FILES) {
                list.setMultiSelect(0);
            }
        }
        //  }
        if (i >= 0) {
            directory_panel.refreshPanel(i, list.getWindowNumberOfRows(), list.getWindowSelectedIndex(), ebook.scene == Scene.EXPLORE_FILES, list.multi_select);
            scroll_bar.refreshIterator(i);
        }
        createBgImage();
        repaint();
    }

    public void addBookMark() {
        text.addBookMark(headline, file_path, getScreenIndex());
    }

    //@TODO 恢复书签的时，三个面板的位置有待解决
    public void resumeBookMark(BookMarkNode pb) {
        reset();
        resetTextPanelPosition(-pb.begin_alias * cnw);
        file_path = pb.file_path;
        headline = getPureFileName(file_path);
        content = new String[frames + rows];//多加一帧用于备份首帧的内容
        tc = text;//tc控制指向text组件
        text.prepareFile(file_path, content, pb.begin, pb.bottom, pb.atop, pb.code);
        text_panel.setContent(content);
        int index[] = new int[3];
        index[0] = pb.begin;
        index[1] = text.next(1);
        index[2] = text.next(2);
        refreshTextPanel(index);
        //初始化上边框
        top_banner.createPanel(color[0], color[2], headline, false);
        createBgImage();
        repaint();
        int i = 0;
        i = 9;
    }

    public void refreshDir() {
        preparePanel(0, list.getNumberOfRows(), headline, "返回");
        createBgImage();
    }

    public void showDir(String d, String f) {
        reset();
        file_path = d;
        if ("/".equals(file_path)) {
            headline = "基于J2ME平台的文本编辑器";
            d = "";
        } else {
            headline = file_path;
            d = "返回";
        }
        content = new String[frames];
        int bottom = directory.readDirectory(file_path, content);

        int selectedIndex;
        if (f == null) {
            selectedIndex = 0;
        } else {
            selectedIndex = ebook.getStringIndex(content, f);
        }
        preparePanel(selectedIndex, bottom, headline, d);

        createBgImage();
        repaint();
        repaint();
    }

//    public void refreshFile() {
//        text_panel.refreshPanel(0, rows, 0, 1, 2, 6);
//        createBgImage();
//        repaint();
//    }
    private void refreshTextPanel(int index[]) {
        int i = 0, j = 0;
        while (i < 3) {
            if (index[i] >= 0) {
//                text_panel.setPanel(text_panel_image[j++]);
                text_panel.setPanel(text_panel_image[j], bgColor[j++]);
                text_panel.refreshPanel(index[i], rows);
                Y_min -= display_panel_height;
            }
            i++;
        }
    }

    private void refreshNext() {
        int i = tc.next();
        if (i >= 0) {
            //设置放置文件内容的容器
            text_panel.setPanel(text_panel_image[first_panel_index], bgColor[first_panel_index]);
//            text_panel.setPanel(text_panel_image[first_panel_index]);
            //刷新该容器里的内容
            text_panel.refreshPanel(i, rows);
            YYY[first_panel_index] += 3 * display_panel_height;
            //增加，保证first_panel_index始终指向的是下一个将被刷新的图片，即Y坐标小于0的那个图片
            first_panel_index = (first_panel_index + 1) % 3;
            //由于中间图片变成下一个了，所以它的坐标也变成下一个图片的坐标
        }
    }

    private void refreshLast() {
        int i = tc.last();
        if (i >= 0) {
            if (tc.hasTextJumped()) {//只对Text类有实际意义
                //@TODO 因为进行了freejump，所以内存中的索引全部无效了，要重新刷新三个面板
                int index[] = new int[3];
                index[0] = i;
                index[1] = text.next(1);
                index[2] = text.next(2);
                resetTextPanelPosition(-display_panel_height);
                refreshTextPanel(index);
            } else {
                first_panel_index = (first_panel_index + 2) % 3;
                //设置放置文件内容的容器
                text_panel.setPanel(text_panel_image[first_panel_index], bgColor[first_panel_index]);
//                text_panel.setPanel(text_panel_image[first_panel_index]);
                //刷新该容器里的内容
                text_panel.refreshPanel(i, rows);
                YYY[first_panel_index] -= 3 * display_panel_height;
            }
        }
    }

    //@TODO 跳转刷新页面有问题
    public void refreshFile(int percentage) {
        int index[] = new int[3];
        index[0] = text.jumpFile(percentage);
        int i = 1, j;
        int status = - 1;
        //获取三个指针,
        //先尝试读取当前帧的前后各一帧，如果后面没有帧了，就读取前面的两帧，如果前面没有帧了，就读取后面的两帧

        //@TODO 下面被修改过了
        while (i < 3) {
            if (status < 0) {
                j = text.last(1);
                if (j < 0) {
                    if (status == -2) {
                        break;
                    }
                    status = 1;
                } else {
                    index[i++] = j;
                    status = 2;
                }
            } else {
                j = text.next(status);
                if (j < 0) {
                    if (status == 1) {
                        break;
                    }
                    status = -2;
                } else {
                    index[i++] = j;
                    status = 2;
                }
            }
        }
        //对三个数进行排序
        if (index[1] > index[2]) {
            j = index[1];
            index[1] = index[2];
            index[2] = j;
        }
        if (index[0] > index[2]) {
            j = index[0];
            index[0] = index[1];
            index[1] = index[2];
            index[2] = j;
            status = 2;//当前页面的指针所在的位置
        } else if (index[0] > index[1]) {
            j = index[0];
            index[0] = index[1];
            index[1] = j;
            status = 1;//当前页面的指针所在的位置
        } else {
            status = 0;//当前页面的指针所在的位置
        }        //排序结束
        resetTextPanelPosition(-status * display_panel_height);
        refreshTextPanel(index);
        createBgImage();
        repaint();
    }

    public void showFile(String f, int code) {
        reset();
        resetTextPanelPosition(0);
        file_path = f;
        headline = getPureFileName(file_path);
        content = new String[frames + rows];//多加一帧用于备份首帧的内容
        tc = text;//tc控制指向text组件
        text.setCode(code);//指定编码
        text.prepareFile(f, content);
        text_panel.setContent(content);
        text.next(0);
        text_panel.setPanel(text_panel_image[0], bgColor[0]);
//        text_panel.setPanel(text_panel_image[0]);
        text_panel.refreshPanel(0, rows);
        //@TODO 这里对Y_min递减了，原来没有这一句
        Y_min -= display_panel_height;
        //初始化上边框
        top_banner.createPanel(color[0], color[2], headline, false);
        createBgImage();
        repaint();
//        new Thread(new Runnable() {
//
//            public void run() {

        int i, j = 1;
        while (j < 3) {
            i = text.next(j);
            if (i >= 0) {
                text_panel.setPanel(text_panel_image[j], bgColor[j]);
//                text_panel.setPanel(text_panel_image[j]);
                text_panel.refreshPanel(i, rows);
                Y_min -= display_panel_height;
            }
            j++;
        }
//            }
//        }).start();
    }

    //初始化各种情景的UI面板
    public void initCanvas(String[] s, String h) {
        reset();
        content = s;
        headline = h;
        preparePanel(0, s == null ? 0 : s.length, headline, "返回");
        createBgImage();
        repaint();
    }

    public void refreshColor() {
        directory_panel.setColor(color[0], color[1], color[2]);//主题色,背景色和前景色
        text_panel.setColor(color[0], color[1], color[2]);//主题色,背景色和前景色
        scroll_bar.setColor(color[1], color[0]);//前景色和背景色
    }

    public int[] postEdit() {
        return text.postEdit(getScreenIndex());
    }

    private void reset() {
        mode = MODE_DEFAULT;//默认
        content = null;
        file_path = null;
        headline = null;
    }

    private void resetTextPanelPosition(int i) {
        first_panel_index = 0;
//        i *= display_panel_height;
        YYY[0] = i;
        i += display_panel_height;
        YYY[1] = i;
        i += display_panel_height;
        YYY[2] = i;

        //@TODO 这里Y_min修改成display_panel_height，原来是0有错误
        Y_min = display_panel_height;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public int getScreenWidth() {
        return width;
    }

    public int getScreenHeight() {
        return height;
    }

    public int getRows() {
        return rows;
    }

    public int getCNW() {
        return cnw;
    }

    public int getCode() {
        return text.getCode();
    }

    public String getSelectedItem() {
        return content[list.getSelectedIndex()];
    }

    public String getScreenContent() {
        return text.getContent(getScreenIndex(), rows);
    }

    public String[] getContent() {
        return content;
    }

    public Image getBgImage() {
        return bgImage;
    }

    private int getScreenIndex() {
        return (int) Math.ceil((double) -YYY[first_panel_index] / cnw);
    }

    //截取文件名，如果有扩展名，也去掉
    private String getPureFileName(String filePath) {
        int bi = filePath.lastIndexOf('/') + 1, ei = filePath.indexOf('.', bi);
        if (bi > ei) {
            ei = filePath.length();
        }
        return filePath.substring(bi, ei);
    }

    private void createBgImage() {
        //清空面板
        graphics.setColor(0);
        graphics.fillRect(0, 0, width, height);

        int y = 0;//用来表示read_panel的Y坐标偏移量
        if (not_full_screen) {//如果不是全屏，则会有上边框
            y = top_banner.getPanel().getHeight();
        }

        //画出面板
        switch (ebook.scene) {
            case Scene.READ_TXT:
                //画出阅读面板
                for (int i = 0, j = height - 2 * y; i < 3; i++) {
                    if (YYY[i] < j) {
                        graphics.drawImage(text_panel_image[i], 0, y + YYY[i], Graphics.TOP | Graphics.LEFT);
                    }
                }
                break;
            default:
                //画出阅读面板
                graphics.drawImage(directory_panel.getPanel(), 0, y, Graphics.TOP | Graphics.LEFT);
                //浏览文件滚动条
                graphics.drawImage(scroll_bar.getPanel(), width, y, Graphics.TOP | Graphics.RIGHT);
        }

        //如果不是全屏，画出上下边框
        if (not_full_screen) {
            graphics.drawImage(top_banner.getPanel(), 0, 0, Graphics.TOP | Graphics.LEFT);
            graphics.drawImage(bottom_banner.getPanel(), 0, height, Graphics.BOTTOM | Graphics.LEFT);
        }

    }

    //@TODO 这里selectedIndex参数有问题 要分辨 开始索引 和 选择索引
    private void preparePanel(int selectedIndex, int bottom, String headline, String option) {
        //设置列表控制器
        list.setList(bottom);
        if (selectedIndex > 0) {
            selectedIndex = list.setWindowSelectedIndex(selectedIndex);//获得begin
        }
        //设置面板用到的content
        directory_panel.setContent(content);
        directory_panel.refreshPanel(selectedIndex, list.getWindowNumberOfRows(), list.getWindowSelectedIndex(), ebook.scene == Scene.EXPLORE_FILES, null);
        //初始化上下边框
        top_banner.createPanel(color[0], color[2], headline, false);
        bottom_banner.createPanel(color[0], color[2], "选项", option);
        //初始化滚动条
        scroll_bar.setIterator(selectedIndex, list.getWindowNumberOfRows(), bottom);
    }
}
