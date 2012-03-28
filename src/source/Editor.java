package source;

import javax.microedition.lcdui.*;
/*
 * com_type取值的含义
 * 0空
 * 1要选择编码
 * 2选择字符串
 * 3确定选择字符串
 * 4粘贴
 * 5确定粘贴项
 * 6复制
 * 7剪切
 * 8替换
 * 9删除
 */

class Editor implements CommandListener {

    private String textContent;//text框里的内容
    private TextBox text;//编辑器
    private List list;//列表，用于粘贴和替换，选择粘贴或替换项
    private Command com[];//命令配置数
    private int prefered_char_code;
    private int com_type;//命令类型
    private int editor_type;//编辑器的类型
    private int start_cursor, end_cursor;//编辑器中选择的字段的起点和终点
    //com_type常量
    private static final int NORMAL = 0;//一般模式
    private static final int TO_SELECT_CODE = 1;//将要选择字符编码
    private static final int SELECTING_STRING = 2;//正在选择字符串
    private static final int PASTING = 3;//正在粘贴
    //命令
    private Command com_copy;
    private Command com_cut;
    private Command com_paste;
    private Command com_save;
    private Command com_save_as;
    private Command com_part_save;
    private Command com_select;
    private Command com_replace;
    private Command com_delete;

    Editor() {
        setCommand();
        prefered_char_code = 1;
    }

    private void setCommand() {
        com_copy = new Command("复制", Command.OK, 3);// copy
        com_cut = new Command("剪切", Command.OK, 3);//cut
        com_paste = new Command("粘贴", Command.OK, 3);//paste
        com_save = new Command("保存", Command.OK, 3);//save
        com_save_as = new Command("另存为", Command.OK, 3);//save_as
        com_part_save = new Command("局部另存", Command.OK, 3);//part_save
        com_select = new Command("选择", Command.OK, 3);//select
        com_replace = new Command("替换", Command.OK, 3);//replace
        com_delete = new Command("删除", Command.OK, 3);//delete
    }

    public void destroy() {
        com_copy = null;
        com_cut = null;
        com_paste = null;
        com_save = null;
        com_save_as = null;
        com_part_save = null;
        com_select = null;
        com_replace = null;
        com_delete = null;
        textContent = null;
        com = null;
        list = null;
        text = null;
    }

    public String[] getTextContents() {
        String file_name = text.getString();
        return new String[]{textContent == null ? "" : textContent, file_name};
    }

    public String getTextContent() {
        textContent = null;
        textContent = text.getString();
        return textContent;
    }

    public void setTextContent(String s) {
        text.setString(s);
    }

    public int getSelectedCharCode() {
        int char_code;
        //1 unicode
        //2 utf-8
        //3 gb2312
        if (list == null) {
            char_code = 1;
        } else {
            char_code = list.getSelectedIndex() + 1;
            list = null;
        }
        return char_code;
    }

    public void setPreferedCharCode(int c) {
        prefered_char_code = c;
    }

    private void createTextBox(String title, String content, int length, Command[] command) {
        if (text == null) {
            text = new TextBox(title, content, length, TextField.ANY);
            text.setCommandListener(this);
        }
        changeCommand(command);
    }

    public void initEditor(int m, String s) {//初始化编辑框,m是编辑框类型，s是编辑框内容
        switch (editor_type = m) {
            case Function.SAVE_EDITED_TXT://编辑文本
            case Function.SAVEAS_EDITED_TXT://另存编辑文本
            case Function.PARTSAVE_EDITED_TXT://局部另存文本
                createTextBox("编辑", s, 1024, new Command[]{com_save, com_save_as, com_part_save, com_select, com_paste, EditLite.com_cancel});//save,save_as,part_save,select,paste,cancel
                editor_type = Function.SAVE_EDITED_TXT;//重置
                break;
            case Function.MAKE_TXT:// 新建文本
            case Function.MAKE_DIRECTORY:// 新建文件夹
                createTextBox("命名为", s, 64, new Command[]{com_save, EditLite.com_cancel});//save,cancel
                break;
            case Function.EDIT_CLIPBOARD://编辑剪贴板
                createTextBox("编辑", s, 1024, new Command[]{com_save, EditLite.com_cancel});//save,cancel
        }
        EditLite.ek.changeDisplay(text);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == EditLite.com_next) {// 下一步next
            if (com_type == TO_SELECT_CODE) {//选择文本编码
                list = new List("请选择编码", 1);
                list.append("Unicode", null);
                list.append("UTF-8", null);
                list.append("GB2312", null);
                list.setSelectedIndex(prefered_char_code - 1, true);
                list.addCommand(com_save);//save
                list.addCommand(EditLite.com_back);//quit
                list.setCommandListener(this);
                EditLite.ek.changeDisplay(list);
            } else if (com_type == SELECTING_STRING) {//已经定位了字段起点，下一步定位终点
                start_cursor = getCursor();
                text.setTitle("请定位终点");
                changeCommand(new Command[]{EditLite.com_ok, EditLite.com_back});//ok,quit
            } else {//com_type == 3已经选择了粘贴项，下一步定位粘贴点
                text.setTitle("请移动光标,定位粘贴点");
                changeCommand(new Command[]{EditLite.com_ok, EditLite.com_back});//ok,quit
                EditLite.ek.changeDisplay(text);
            }
        } else if (c == com_copy) {//复制copy
            resumeBasicMode();
            EditLite.ek.alert("已存储到剪贴板");
            EditLite.ek.cb.setContent(text.getString().substring(start_cursor, end_cursor), -1);//存储
        } else if (c == com_cut) {//剪切cut
            resumeBasicMode();
            EditLite.ek.alert("已存储到剪贴板");
            EditLite.ek.cb.setContent(text.getString().substring(start_cursor, end_cursor), -1);//存储
            text.delete(start_cursor, end_cursor - start_cursor);//删除
        } else if (c == com_replace) {//替换replace
            //首先显示粘贴项列表,用于选择
            list = new List("请选择", 3);
            list.addCommand(EditLite.com_ok);//ok
            list.addCommand(EditLite.com_cancel);//cancel
            list.setCommandListener(this);
            EditLite.ek.cb.listContent(list);
            EditLite.ek.changeDisplay(list);
        } else if (c == com_delete) {//删除delete
            resumeBasicMode();
            text.delete(start_cursor, end_cursor - start_cursor);//删除
        } else if (c == com_paste) {//粘贴paste
            //首先显示粘贴项列表,用于选择
            list = new List("请选择", 3);
            list.addCommand(EditLite.com_next);//next
            list.addCommand(EditLite.com_cancel);//cancel
            list.setCommandListener(this);
            EditLite.ek.cb.listContent(list);
            EditLite.ek.changeDisplay(list);
            com_type = PASTING;
        } else if (c == com_select) {//选择select
            text.setTitle("请移动光标,定位起始点");
            changeCommand(new Command[]{EditLite.com_next, EditLite.com_cancel});//next,cancel
            start_cursor = end_cursor = 0;//重置字段起点和终点
            com_type = SELECTING_STRING;
        } else if (c == com_save) {//保存save
            EditLite.ek.createForm(editor_type);//弹出提示框
        } else if (c == EditLite.com_ok) {//确定ok
            if (com_type == SELECTING_STRING) {//已经选择好字段
                if (list == null) {
                    end_cursor = getCursor();
                    changeCommand(new Command[]{com_copy, com_cut, com_replace, com_delete, EditLite.com_cancel});
                    text.setTitle("已选择字段");
                } else {//正在进行替换操作replace
                    //返回到一般编辑模式
                    resumeBasicMode();
                    text.delete(start_cursor, end_cursor - start_cursor);//删除
                    text.insert(list.getString(list.getSelectedIndex()), start_cursor);//粘贴
                    list = null;
                }
            } else {// com_type == 3粘贴到文本框中
                //返回到一般编辑模式
                resumeBasicMode();
                text.insert(list.getString(list.getSelectedIndex()), getCursor());//插入
                list = null;
            }
        } else if (c == EditLite.com_back) {//上一步back
            if (com_type == SELECTING_STRING) {//处于选择模式,应该是由选择终点返回到选择起点
                text.setTitle("请移动光标,定位起始点");
                changeCommand(new Command[]{EditLite.com_next, EditLite.com_cancel});//next,cancel
                start_cursor = 0;//重置字段起点
            } else if (com_type == PASTING) {//粘贴模式
                EditLite.ek.changeDisplay(list);//返回选择粘贴项列表
            } else if (list == null) {//正在命名文件，返回到编辑文本内容窗口
                initEditor(editor_type, null);//恢复编辑框中的命令
                setTextContent(textContent);//恢复编辑框中的内容
                com_type = NORMAL;//一般编辑模式
            } else {//正在选择文本编码，返回到命名文件窗口
                EditLite.ek.changeDisplay(text);
                list = null;
            }
        } else if (c == EditLite.com_cancel) {//取消cancel
            if (com_type == NORMAL) {//一般编辑模式
                //退出编辑模式
                EditLite.ek.changeDisplay(null);//显示viewer
                text = null;
                textContent = null;
            } else if (com_type == SELECTING_STRING) {//选择模式
                if (list == null) {
                    //返回到一般编辑模式
                    resumeBasicMode();
                } else {//正在进行替换操作replace
                    EditLite.ek.changeDisplay(text);//显示text
                    list = null;
                }
            } else if (com_type == PASTING) {//粘贴模式
                //返回到一般编辑模式
                resumeBasicMode();
                EditLite.ek.changeDisplay(text);//显示text
                list = null;
            }
        } else {//局部另存或另存为
            textContent = text.getString();
            text.setString(null);
            changeCommand(new Command[]{EditLite.com_cancel, EditLite.com_back, EditLite.com_next});//cancel,quit,next
            text.setTitle("文件命名为");
            if (c == com_part_save) {//编辑文本
                editor_type = Function.PARTSAVE_EDITED_TXT;//局部另存编辑文本 part_save
            } else {//editor_type==2编辑文本
                editor_type = Function.SAVEAS_EDITED_TXT;//编辑文本另存为save_as
            }
            com_type = TO_SELECT_CODE;//下一步需要选择编码
        }
    }

    private int getCursor() {
        int a = text.getCaretPosition();
        if (start_cursor > a) {// 交换a,cursor
            a += start_cursor;
            start_cursor = a - start_cursor;
            a -= start_cursor;
        }
        return a;
    }

    private void resumeBasicMode() {
        text.setTitle("编辑");
        changeCommand(null);//删除当前编辑框的命令
        initEditor(editor_type, null);//恢复编辑框的命令
        com_type = NORMAL;//复位
    }

    private void changeCommand(Command[] co) {//co是新的命令序号列表
        int n, i;
        n = com == null ? 0 : com.length;
        for (i = 0; i < n; i++) {
            text.removeCommand(com[i]);
        }
        com = null;
        com = co;
        n = com == null ? 0 : com.length;
        for (i = 0; i < n; i++) {
            text.addCommand(com[i]);
        }
    }
}
