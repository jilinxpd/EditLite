package source;

import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;

public class EditLite extends MIDlet implements CommandListener {

//记录当前主场景的类
//（浏览文件夹，浏览记事列表，浏览历史记录列表，浏览剪贴板列表，浏览书签列表，浏览搜索结果列表）
    class Scenario {//内部类

        String[] menuItems;//菜单列表
        String headline;//标题
        String[] content;//内容
        int scene;//场景
    }
    private int level, select, whichOK;
    private String currFile, currDirName;
    private Form form;
    private FileManager fm;
    private BookMark bm;
    private Editor editor;
    private ViewCanvas viewer;
    private Batch batch;
    private Scenario ms;//主场景记录
    private JumpBox jb;
    Menu menu;//菜单
    ClipBoard cb;//剪贴板
    public int scene;//当前场景标识
    public static EditLite ek;//全局变量ek
    //命令常量
    public static Command com_ok = new Command("确定", Command.OK, 2);//ok
    public static Command com_cancel = new Command("取消", Command.CANCEL, 1);//cancel
    public static Command com_next = new Command("下一步", Command.CANCEL, 1);//next
    public static Command com_back = new Command("上一步", Command.CANCEL, 1);//quit

    public void startApp() {
        if (ek == null) {
            currDirName = "/";
            ek = this;
            UserData ud = new UserData();
            ud.loadSettings();//加载个人设置
            viewer = new ViewCanvas();//创建显示层
            viewer.postCreateReadCanvas(ud.color, ud.key, ud.images);
            ms = new Scenario();//主场景记录
            menu = new Menu(viewer.getCNW(), viewer.getScreenHeight(), ud.color, ud.key, viewer.getBgImage());//创建菜单
            quit();//当前主场景为显示文件夹主场景
            fm = new FileManager();//创建文件管理器
            cb = new ClipBoard();//创建剪贴板
        }
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean c) {
        notifyDestroyed();
    }

    public void changeDirectory(String fileName) {
        if (currDirName.equals("/")) {
            if (fileName.equals("..")) {
                return;
            }
            currDirName = fileName;
            currFile = null;
        } else if (fileName.equals("..")) {
            int i = currDirName.lastIndexOf('/', currDirName.length() - 2);
            if (i != -1) {
                currFile = currDirName.substring(i + 1);
                currDirName = currDirName.substring(0, i + 1);
            } else {
                currDirName = "/";
                currFile = null;
            }
        } else {
            currDirName = currDirName + fileName;
            currFile = null;
        }
        showCurrentDirectory(currFile);
    }

    public void createForm(int which_ok) {
        form = new Form("保存");
        form.append("是否保存?");
        registControl(form);
        this.whichOK = which_ok;
    }

    //向全局控制器(消息泵)注册，以便受全局控制器的控制
    public void registControl(Displayable d) {
        d.addCommand(com_ok);//ok
        d.addCommand(com_cancel);//cancel
        d.setCommandListener(this);
        Display.getDisplay(this).setCurrent(d);
    }

    public void changeDisplay(Displayable d) {
        if (d == null) {
            d = viewer;
        }
        Display.getDisplay(this).setCurrent(d);
    }

    public void keyAction() {
        level = menu.getMenuLevel();
        select = menu.getSelectedIndex(level);
        switch (scene) {//根据情景分类
            case Scene.EXPLORE_FILES://文件夹浏览模式
                if (level == -1) {//默认，直接按下确定
                    new Thread(new Runnable() {

                        public void run() {
                            startBrowse(0);
                        }
                    }).start();
                } else if (level == 0) {//一级菜单
                    if (select == 0) {// "浏览"
                        menu.switchMenu(false);
                        menu.initialMenu(new String[]{"默认", "其他"}, 3, 2);
                        menu.initialImage(0);
                    } else if (select == 1) {// 附件
                        menu.switchMenu(false);
                        menu.initialMenu(new String[]{"书签", "剪贴板"}, 3, 0);
                        menu.initialImage(0);
                    } else if (select == 2) {// "新建"
                        menu.switchMenu(false);
                        menu.initialMenu(new String[]{"文本", "文件夹"}, 3, 0);
                        menu.initialImage(0);
                    } else {// "退出"
                        destroyApp(false);
                    }
                } else if (level == 1) {//二级菜单
                    select = menu.getSelectedIndex(0);
                    if (select == 0) {//上级选的是浏览
                        select = menu.getSelectedIndex(1);
                        if (select == 0) {//默认
                            new Thread(new Runnable() {

                                public void run() {
                                    startBrowse(0);
                                    changeDisplay(viewer);
                                }
                            }).start();
                        } else {//其他
                            menu.switchMenu(false);
                            menu.initialMenu(new String[]{"Unicode", "UTF-8", "GB2312"/*, "十六进制"*/}, 4, 0);
                            menu.initialImage(0);
                        }
                    } else if (select == 1) {//上级选的是附件
                        select = menu.getSelectedIndex(1);
                        if (select == 0) {// 书签
                            new Thread(new Runnable() {

                                public void run() {
                                    if (bm == null) {
                                        bm = new BookMark();
                                    }
                                    bm.initBookMark();
                                    initScenario(new String[]{"浏览", "删除"}, bm.getAll(), "书签管理", Scene.EXPLORE_BOOKMARKS, true);
                                }
                            }).start();
                        } else {// 剪贴板
                            new Thread(new Runnable() {

                                public void run() {
                                    initScenario(new String[]{"编辑", "删除"}, cb.getAll(), "剪贴板", Scene.EXPLORE_CLIPBOARD, true);
                                }
                            }).start();
                        }
                    } else {//上级选的是新建
                        select = menu.getSelectedIndex(1);
                        editor = null;
                        editor = new Editor();
                        if (select == 0) {// makefile
                            editor.initEditor(Function.MAKE_TXT, null);//新建文本
                        } else {// makedir
                            editor.initEditor(Function.MAKE_DIRECTORY, null);//新建文件夹
                        }
                    }
                } else {//三级菜单
                    //如果在根目录下，不能执行任何操作
                    changeDisplay(viewer);
                    if (currDirName.equals("/")) {
                        alert("禁止管理根目录");
                        return;
                    }
                    //第一级菜单选的是“浏览”
                    select = menu.getSelectedIndex(2);
                    if (select == 3) {
                        //十六进制查看
                    } else {
                        //unicode,utf-8,gb2312查看
                        new Thread(new Runnable() {

                            public void run() {
                                startBrowse(select + 1);
                            }
                        }).start();
                    }
                }
                break;
            case Scene.READ_TXT://文本阅读模式
                if (select == 0) { // 添加书签
                    changeDisplay(viewer);
                    new Thread(new Runnable() {

                        public void run() {
                            viewer.addBookMark();
                            warn(ViewCanvas.MODE_OK);
                        }
                    }).start();
                } else if (select == 1) {// 编辑
                    editor = null;
                    editor = new Editor();
                    editor.initEditor(Function.SAVE_EDITED_TXT, viewer.getScreenContent());//编辑文本
                    editor.setPreferedCharCode(viewer.getCode());
                } //                else if (select == 2) {//查找
                //                    changeDisplay(viewer);
                //                    viewer.refreshFile();
                //                }
                else {//跳转
//                    viewer.refreshFile(50);
//                    changeDisplay(viewer);
                    jb = null;
                    jb = new JumpBox();
                    jb.initJumpBox();
                    whichOK = Function.JUMP;
                }
                break;
            case Scene.EXPLORE_BOOKMARKS://书签
                if (level >= 0) {
                    changeDisplay(viewer);
                }
                if (viewer.list.getNumberOfRows() == 0) {
                    return;
                }
//                System.out.println(select);
                if (select == 0) {// 浏览
                    new Thread(new Runnable() {

                        public void run() {
                            startReviewText();
                        }
                    }).start();
                } else if (select == 1) {// 删除
                    new Thread(new Runnable() {

                        public void run() {
                            if (viewer.list.multi_select == null) {
                                bm.delete(viewer.list.getSelectedIndex());
                            } else {//批量删除
                                batch = new Batch() {

                                    private int k = 0;

                                    public void process(String s, int i) {
                                        bm.delete(i - k++);
                                    }

                                    public void destroy() {
                                    }
                                };
                                batchProcess();//执行批任务
                            }
                            viewer.initCanvas(bm.getAll(), "书签管理");
                            changeDisplay(viewer);
                        }
                    }).start();
                }
                break;
            case Scene.EXPLORE_CLIPBOARD://剪贴板
                if (select == 0) {// edit
                    editor = null;
                    editor = new Editor();
                    editor.initEditor(Function.EDIT_CLIPBOARD, cb.getContent(viewer.list.getSelectedIndex()));//编辑剪贴板
                } else if (select == 1) {// delete
                    if (viewer.list.multi_select == null) {
                        cb.delContent(viewer.list.getSelectedIndex());
                    } else {
                        batch = new Batch() {

                            public void process(String s, int i) {
                                cb.delContent(i);
                            }

                            public void destroy() {
                            }
                        };
                        batchProcess();//执行批任务
                    }
                    changeDisplay(viewer);
                }
                break;
        }
    }

    //全局控制器
    public void commandAction(Command c, Displayable d) {
        if (c == com_ok) {//确定
            switch (whichOK) {
                case Function.SAVE_EDITED_TXT://保存编辑文本
                    new Thread(new Runnable() {

                        public void run() {
                            warn(ViewCanvas.MODE_WAITING);
                            form = null;
                            String s = editor.getTextContent();
                            editor.destroy();
                            editor = null;
                            int[] et = viewer.postEdit();
                            fm.saveEditedFile(s, currDirName, currFile, currFile, et[0], et[1], et[2], viewer.getCode(), viewer.getCode(), true);
                            viewer.keyPressed(UserData.ud.key[6]);//退出到文件浏览
//                            quit();
                            et = null;
                            s = null;
                        }
                    }).start();
                    break;
                case Function.SAVEAS_EDITED_TXT://另存编辑文本
                    new Thread(new Runnable() {

                        public void run() {
                            warn(ViewCanvas.MODE_WAITING);
                            form = null;
                            String[] str = editor.getTextContents();
                            int[] et = viewer.postEdit();
                            fm.saveEditedFile(str[0], currDirName, currFile, str[1] + ".txt", et[0], et[1], et[2], editor.getSelectedCharCode(), viewer.getCode(), false);
                            warn(ViewCanvas.MODE_DEFAULT);
                            str = null;
                            et = null;
                        }
                    }).start();
                    break;
                case Function.PARTSAVE_EDITED_TXT://局部另存文本
                case Function.MAKE_TXT://新建文本
                    new Thread(new Runnable() {

                        public void run() {
                            warn(ViewCanvas.MODE_WAITING);
                            String s[] = editor.getTextContents();
                            fm.saveNewFile(s[0], currDirName, s[1] + ".txt", editor.getSelectedCharCode());
                            editor.destroy();
                            editor = null;
                            if (scene == Scene.EXPLORE_FILES) {
                                showCurrentDirectory(null);
                            } else {
                                warn(ViewCanvas.MODE_DEFAULT);
                            }
                        }
                    }).start();
                    break;
                case Function.MAKE_DIRECTORY://新建文件夹
                    new Thread(new Runnable() {

                        public void run() {
                            fm.makeDir(editor.getTextContent(), currDirName + editor.getTextContent());
                            editor.destroy();
                            editor = null;
                            whichOK = 0;
                            showCurrentDirectory(null);
                        }
                    }).start();
                    break;
                case Function.EDIT_CLIPBOARD://保存剪贴板
                    changeDisplay(viewer);
                    cb.setContent(editor.getTextContent(), viewer.list.getSelectedIndex());
                    break;
                case Function.JUMP://跳转
                    new Thread(new Runnable() {

                        public void run() {
                            changeDisplay(viewer);
                            viewer.refreshFile(jb.getPercentage());
                            jb = null;
                        }
                    }).start();
                    break;
            }
        } else if (c == com_cancel) {//取消
            changeDisplay(viewer);//改变当前显示
            // 下面的主要是清理工作
            switch (whichOK) {
                case Function.SAVEAS_EDITED_TXT:
                case Function.MAKE_TXT:
                case Function.MAKE_DIRECTORY:
                case Function.EDIT_CLIPBOARD:
                    editor.destroy();
                    editor = null;
                    break;
            }
        }
    }

    public void batchProcess(boolean[] marks, String[] sources, Batch batch) {
        int i = 0, j = marks.length;
        for (; i < j; i++) {
            if (marks[i]) {
                batch.process(sources[i], i);
            }
        }
    }

    public void batchProcess() {
        batchProcess(viewer.list.multi_select, viewer.getContent(), batch);
        batch.destroy();
        batch = null;
        viewer.list.setMultiSelect(-1);//清空多选标记
    }

    /*
     * 是公共函数，所以放在这个类里面，便于调用;
     * 在一个字符串数组中查找一个字符串，返回它的位置
     * @param content
     * 要在这里面查找字符串
     * @param s
     * 要查找的字符串
     * @return 目标字符串的位置
     */
    public int getStringIndex(String[] content, String s) {
        for (int i = 0, j = content.length; i < j && content[i] != null; i++) {
            if (content[i].equalsIgnoreCase(s)) {
                return i;
            }
        }
        return -1;
    }

    /*
     * 是公共函数，所以放在这个类里面，便于调用;
     * 将一个字符串数组连接成一个字符串
     * @param content
     * 要连接的字符串数组
     * @return 连接后的字符串
     */
    public String getCatchedString(String[] content) {
        StringBuffer s = new StringBuffer();
        for (int i = 0, j = content.length; i < j && content[i] != null; i++) {
            s.append(content[i]);
        }
        return s.toString();
    }

    private void showCurrentDirectory(String fileName) {
        viewer.showDir(currDirName, fileName);
        Display.getDisplay(this).setCurrent(viewer);
    }

    //初始化情景
    private void initScenario(String[] cmd, String[] content, String headline, int scene, boolean record) {
        this.scene = scene;
        menu.resetMenuLevel();
        menu.initialMenu(cmd, 4, 0);
        viewer.initCanvas(content, headline);
        Display.getDisplay(this).setCurrent(viewer);
        if (record) {
            //记录主场景
            ms.menuItems = null;
            ms.content = null;
            ms.headline = null;
            ms.menuItems = cmd;
            ms.content = content;
            ms.headline = headline;
            ms.scene = scene;
        }
    }

    private void startBrowse(int code) {
        menu.resetMenuLevel();
        currFile = viewer.getSelectedItem();
        if (currFile.endsWith("/") || currFile.equals("..")) {
            changeDirectory(currFile);
        } else {
            startReadText(currDirName + currFile, code);
        }
    }

    //推荐使用多线程
    private void startReadText(String filePath, int code) {
        menu.initialMenu(new String[]{"添加书签", "编辑", /*"查找",*/ "跳转"}, 4, 0);
        scene = Scene.READ_TXT;
        viewer.showFile(filePath, code);
    }

    //推荐使用多线程
    private void startReviewText() {
        int i = viewer.list.getSelectedIndex();
        String file_path = bm.getFilePath(i);
        if (!fm.doesFileExist(file_path)) {
            return;
        }
        scene = Scene.READ_TXT;
        menu.resetMenuLevel();
        menu.initialMenu(new String[]{"添加书签", "编辑", "跳转"}, 4, 0);
        viewer.resumeBookMark(bm.getBookMark(i));
    }

    public void warn(int mode) {
        viewer.setMode(mode);
        Display.getDisplay(this).setCurrent(viewer);
        viewer.repaint();
    }

    public void alert(String s) {
        Alert a = new Alert("提示", s, null, AlertType.INFO);
        a.setTimeout(600);
        Display.getDisplay(this).setCurrent(a);
        try {
            Thread.sleep(400);
        } catch (Exception ex) {
        }
    }

    //退出当前主场景到显示文件夹的主场景
    public void quit() {
        scene = Scene.EXPLORE_FILES;
        new Thread(new Runnable() {

            public void run() {
                showCurrentDirectory(currFile);
            }
        }).start();
        //清理工作
        if (ms.scene == Scene.EXPLORE_BOOKMARKS) {
            bm.destroy();
            bm = null;
        }
        //初始化新的主场景
        menu.resetMenuLevel();
        menu.initialMenu(new String[]{"打开", "工具", "新建", "退出"}, 3, 7);
        //记录主场景
        ms.content = null;
        ms.headline = null;
        ms.menuItems = null;
        ms.scene = Scene.EXPLORE_FILES;
        //不用记录其他的字段了，因为这个Scene.EXPLORE_FILES返回时...
    }

    //返回到当前主场景界面，根据ms变量来恢复主场景
    public void back() {
        if (ms.scene == Scene.EXPLORE_FILES) {
            quit();
        } else {
            initScenario(ms.menuItems, ms.content, ms.headline, ms.scene, false);
        }
    }
}



