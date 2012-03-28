package source;

import javax.microedition.lcdui.*;
import javax.microedition.rms.*;

class UserData {

    int font_size;
    int color[], key[];// color[]分别表示mnc(主题颜色),bgc(背景颜色),ftc(字体颜色)
    Image images[];
    private int config_data[];
    private RecordStore rs;
    private SetKey sk;
    static UserData ud;

    UserData() {
        try {
            images = new Image[4];
            images[0] = Image.createImage("/i/d");
            images[1] = Image.createImage("/i/t");
            images[2] = Image.createImage("/i/u");
            images[3] = Image.createImage("/i/r");
        } catch (Exception x) {
        }
        color = new int[]{0x28d2fa, 0xf0f0fa, 0, 0xcccccc};
        key = new int[7];
        ud = this;
    }

    void loadSettings() {
        try {
            rs = RecordStore.openRecordStore("s", true);
            config_data = new int[12];

            if (rs.getNumRecords() == 0) {// 第一次运行程序
                sk = new SetKey(key);
                EditLite.ek.changeDisplay(sk);
                while (sk.undone) {//忙等待
                    config_data[0] = 0;
                }
                sk = null;
//                key[0] = -1;
//                key[1] = -2;
//                key[2] = -3;
//                key[3] = -4;
//                key[4] = -5;
//                key[5] = -6;
//                key[6] = -7;
                new Thread(new Runnable() {

                    public void run() {
                        reset();
                        saveSettings(false);
                    }
                }).start();
            } else {// 以后
                byte[] data = rs.getRecord(1);
                int i = -1;
                while (++i < 12) {
                    if ((config_data[i] = data[i]) < 0) {
                        config_data[i] += 256;
                    }
                }
                data = null;
                data = rs.getRecord(3);//键值
                for (i = 0; i < 7; i++) {
                    key[i] = 0 - (int) data[i];
                }
                rs.closeRecordStore();
                rs = null;
                refresh(true);
                config_data = null;
            }
        } catch (Exception x) {
            x.printStackTrace();
        }
    }



    void saveSettings(boolean b) {
        try {
            byte[] data = new byte[12];
            int i = -1;
            while (++i < 12) {
                data[i] = (byte) config_data[i];
            }
            config_data = null;
            if (b) {
                rs = null;
                rs = RecordStore.openRecordStore("s", false);
                rs.setRecord(1, data, 0, 12);
            } else {
                rs.addRecord(data, 0, 12);// 1基本设置
                rs.addRecord(null, 0, 0);// 2密码
                i = -1;
                while (++i < 7) {
                    data[i] = (byte) (0 - key[i]);
                }
                rs.addRecord(data, 0, 7);//3按键
            }
            rs.closeRecordStore();
            rs = null;
        } catch (Exception x) {
        }
    }

    void reset() {
        config_data[0] = 40;
        config_data[1] = 210;
        config_data[2] = 250;
        config_data[3] = 240;
        config_data[4] = 240;
        config_data[5] = 250;
        config_data[6] = 0;
        config_data[7] = 0;
        config_data[8] = 0;
        config_data[9] = 0;// 自动退出,历史记录,默认编码的开关
        config_data[10] = 2;// 自动退出时间
        config_data[11] = 0;//默认编码unicode
    }

    void refresh(boolean b) {// 刷新设置
        color[0] = (config_data[0] << 16) | (config_data[1] << 8) | config_data[2];// 主题色(边栏背景色)
        color[1] = (config_data[3] << 16) | (config_data[4] << 8) | config_data[5];// 背景色(页面背景色)
        color[2] = (config_data[6] << 16) | (config_data[7] << 8) | config_data[8];// 字体色(前景色)
    }
}
