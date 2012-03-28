/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package source;

import java.io.*;
import javax.microedition.io.*;
import javax.microedition.io.file.*;
import javax.microedition.lcdui.*;
import javax.microedition.rms.*;

public class Text implements TextController {

    private boolean loading;
    private long file_size;
    //begin是标记被画布使用的那三段帧的起始地址,top标记缓冲区中的第一帧的起始地址，bottom标记最近加入的帧的结束地址
    //a_top是top之前已读取了的字节总数，b_top比a_top多top下面的一帧的字节数
    private int width, frames, begin, code, number_of_rows;
    private int since_LastASCIndex;//用于gb2312,统计距离已知的最近的一个ASC字符的字节数
    private boolean autoCode;
    private byte gb[];
    private PFM pfm;
    private InputStream fis;
    private FileConnection fc;
    private Font font = Font.getFont(0, 0, 0);//获取系统标准字体
    private boolean text_jumped;

    public Text(int width, int number_of_rows, int frames) {
        this.width = width;
        this.number_of_rows = number_of_rows;
        this.frames = frames;
    }

    public void limitedJump(int bottom_limit) {
        try {
            while (notOver() && pfm.getBottom() < bottom_limit) {
                readText();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void freeJump(int leap_total, int leap_base) {
        //read_bytes_amount目前已读取的字节总数
        //fix_bytes_amount由于修正而多读取的字节数
        //limit每次小跳的界限，到达这个界限则说明跳转成功
        int read_bytes_amount = 0, fix_bytes_amount = 0, leap = 0, limit = leap_base, limit_delta, nbottom;
        limit_delta = leap_total / 2;//小跳界限的增量
        try {
            while (true) {
                pfm.reset(limit, true);//先不把fix_bytes_amount统计在内，因为这对比较大小不产生影响
                limit += limit_delta;
                limit_delta /= 2;
                nbottom = 0;
                while (nbottom < 30 && notOver()) {
                    readText();
                    nbottom++;//递增
                }
                read_bytes_amount = pfm.getByteCountX(nbottom);//这才是当前已经读取的所有字节数
                //下面判断是否已经跳转成功，即当前缓冲区包含要跳转到的那个字节
                if (read_bytes_amount >= limit) {
                    break;
                }
                //如果没有达到预定的数量，就要直接跳到预定的字节处,这样可以节省时间
                leap = limit - read_bytes_amount;
                if (code == 1) {//unicode
                    fix_bytes_amount += skipUnicode(leap);//因为修正而多读取的字节
                } else if (code == 2) {//utf8
                    fix_bytes_amount += skipUTF8(leap);
                } else if (code == 3) {//gb2312
                    fix_bytes_amount += skipGB2312(leap);
                }
            }
            limit = leap_base + leap_total;
//            System.out.println("read_bytes_amount  " + read_bytes_amount);
//            System.out.println("limit  " + limit);
            if (read_bytes_amount < limit) {
                //达到预定的字节数，说明下面的字节也不多了，可以接着读，不会浪费很多时间
                pfm.reset(read_bytes_amount + fix_bytes_amount, false);//加上以前没统计的fix_bytes_amount
                nbottom = 0;
                while (read_bytes_amount < limit) {
                    readText();
                    nbottom++;
                    read_bytes_amount = pfm.getByteCountX(nbottom);//这才是当前已经读取的所有字节数
                }
            }
        } catch (Exception x) {
            x.printStackTrace();
        }
        text_jumped = true;
    }

    public void reloadFile(int leap) {
        try {
            if (fis != null) {
                fis.close();
                fis = null;
            }
            fis = fc.openInputStream();
            pfm.reset(leap, true);
//            fis.skip(leap);
            while (leap-- > 0) {//跳跃
                fis.read();
            }
            pfm.setTempContent("");
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    //用于跳转,必须保证percentage是介于0~100
    public int jumpFile(int percentage) {
        int leap = (int) (file_size * percentage / 100);//本次要跳转到的地方
        begin = pfm.findFrameByByte(leap);
//        text_jumped = true;
        //@TODO 这里将true改成false
        text_jumped = false;

        return begin;
    }

    //用于从书签读取
    //@TODO 这里也要考虑三个帧的问题,并且还有三个页面的排版问题
    public void prepareFile(String file_path, String content[], int begin, int bottom_limit, int leap, int code) {
        setCode(code);
        prepareFile(file_path, content, leap);
        limitedJump(bottom_limit);
        this.begin = begin;
    }

    //用于一般模式
    public void prepareFile(String file_path, String content[]) {
        prepareFile(file_path, content, 0);
    }

    public int[] postEdit(int alias) {
        try {
            int i = begin + alias, k = i + number_of_rows, et[] = new int[]{pfm.getByteCountX(i / number_of_rows), 0, 0}, a[] = new int[2];
            StringBuffer s = new StringBuffer();
            String str[] = new String[2];
            do {
                s.append(pfm.getContentX(i++));
            } while (i % number_of_rows != 0);
            str[0] = s.toString();
            while (i != k) {
                s.append(pfm.getContentX(i++));
            }
            str[1] = s.toString();
            s = null;
//            System.out.println(str[0]);
//            System.out.println("************************");
//            System.out.println(str[1]);
            if (code == 1) {//unicode
                a[0] = 2 * str[0].length();
                a[1] = 2 * str[1].length();
            } else if (code == 2) {//utf-8
                try {
                    ByteArrayOutputStream baos;
                    DataOutputStream dos;
                    k = 0;
                    while (k < 2) {
                        baos = new ByteArrayOutputStream();
                        dos = new DataOutputStream(baos);
                        dos.writeUTF(str[k]);
                        a[k++] = baos.size() - 2;
                        dos.close();
                        baos.close();
                        baos = null;
                        dos = null;
                    }
                } catch (Exception x) {
                }
            } else {//gb2312
                char c[];
                int j, m, n;
                k = 0;
                while (k < 2) {
                    c = str[k].toCharArray();
                    j = 0;
                    m = c.length;
                    n = 0;
                    while (j < m) {
                        if (c[j++] < 128) {
                            n++;
                        }
                    }
                    c = null;
                    a[k++] = 2 * m - n;
                }
            }
            et[0] -= a[0];//front
            et[1] = a[1];//skip
            et[2] = (int) file_size - et[0] - et[1];//rest
            a = null;
            str = null;
            return et;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void addBookMark(String headline, String file_path, int index) {
        byte[] data = null;
        try {
            RecordStore rs = RecordStore.openRecordStore("b", true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(pfm.getSkipByteCount());//atop
            dos.writeInt(begin - pfm.getTop());//begin - top 头偏移量
            dos.writeInt(index);//当前屏幕首行相对于begin的偏移量
            dos.writeInt(pfm.getBottom() - pfm.getTop());//nbottom - top 尾偏移量
            dos.writeInt(autoCode ? 0 : code);//打开文件时使用的code
            dos.writeUTF("[" + headline + "]" + pfm.getContentX(begin + index));//简介
            dos.writeUTF(file_path);//file_path文件地址
            data = baos.toByteArray();
            rs.addRecord(data, 0, data.length);
            rs.closeRecordStore();
            rs = null;
        } catch (Exception x) {
        }
        data = null;
    }

    //预读取用到的函数
    public int next(int i) {
        return pfm.findFrameByIndex(begin + i * number_of_rows);
    }

    //向下获取即将要显示的帧的起始地址，如果缓冲区中没有所须的内容，则从手机存储中读取，这个操作对面板类是透明的
    public int next() {
        int index = pfm.findFrameByIndex(begin + 3 * number_of_rows);//只有3个画布面板容器，循环使用
        if (index >= 0) {
            begin += number_of_rows;
        }
        return index;//第3个窗口起始地址
    }

    public int last(int i) {//i是正数
        i = begin - i * number_of_rows;
        if (i < pfm.getTop()) {
            i = -1;
        } else {
            begin = i;
        }
        return i;
    }

    //向上获取即将要显示的帧的起始地址，如果缓冲区中没有所须的内容，则从手机存储中读取，这个操作对面板类是透明的
    public int last() {
        int index = pfm.findFrameByIndex(begin - number_of_rows);
        if (index >= 0) {
            begin = index;
        }
        return index;
    }

//    public int FFF() {
//
//        begin = pfm.getTop();
//        return begin;
//    }
    public String getContent(int begin_alias, int rows) {
        StringBuffer s = new StringBuffer();
        for (int i = begin + begin_alias, j = rows + i; i < j; i++) {
            s.append(pfm.getContentX(i));
        }
        return s.toString();
    }

    public void destroy() {
        close();
        pfm.destroy();
        pfm = null;
    }

    public void setCode(int b) {
        if (b >= 0 && b < 4) {
            code = b;
        } else {
            code = 0;
        }
    }

    public int getCode() {
        return code;
    }

    public boolean hasTextJumped() {
        boolean t = text_jumped;
        text_jumped = false;
        return t;
    }

    long getFileSize() {
        return file_size;
    }

    private boolean notOver() {
        return null != fis;
    }

    private void close() {
        try {
            if (fis != null) {
                fis.close();
                fis = null;
            }
            if (fc != null) {
                fc.close();
                fc = null;
            }
            gb = null;
            loading = true;
        } catch (Exception x) {
        }
    }

    private void prepareFile(String file_path, String content[], int leap) {
        try {
            fc = (FileConnection) Connector.open("file:///" + file_path);
            fis = fc.openInputStream();
            pfm = new PFM(content, number_of_rows, frames, this);
            int t = autoSetCode() + leap;
//            fis.skip(leap);
            while (leap-- > 0) {//跳跃
                fis.read();
            }
            pfm.reset(t, true);
            pfm.setTempContent("");
            file_size = fc.fileSize();
            begin = 0;
            loading = true;
            if (code == 3) {
                new Thread(new Runnable() {

                    public void run() {
                        try {
                            gb = null;
                            gb = new byte[15386];
                            getClass().getResourceAsStream("/c/g").read(gb);
                            loading = false;
                        } catch (Exception x) {
                        }
                    }
                }).start();
            }
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    private int autoSetCode() throws IOException {
        int a, b;
        if (code == 0) {//没有指定编码
            a = fis.read();
            b = fis.read();
            if (a == 239 && b == 187 && fis.read() == 191) {
                code = 2;//utf-8
                a = 3;//临时保存byte_count[0]的初始值
            } else if (a != 255 || b != 254) {
                code = 3;//gb2312
                a = 0;//临时保存byte_count[0]的初始值
                fis.close();
                fis = null;
                fis = fc.openInputStream();
            } else {
                code = 1;//unicode
                a = 2;//临时保存byte_count[0]的初始值
            }
            autoCode = true;
        } else {//已经指定了编码
            a = 0;//临时保存byte_count[0]的初始值
            autoCode = false;
        }
        return a;
    }

    private void readText() {
        try {
            String ccs = pfm.getTempContent();
            //m是已经读取的行数，n是当前处理的行的物理长度（像素为单位），w是当前处理的英文单词的物理长度，k是非英文单词的字符宽度
            //g是此次读取的有效字符所占的字节数，r是当前处理的英文单词所占的字节总数
            //a是当前处理的行所占的字节总数，b是此次读操作(此次函数调用)已读取的总字节数
            int i = 0, j = 0, m = 0, n = font.stringWidth(ccs), a = 0, b = 0, d = width, w = 0, k = 0, g = 0, r = 0;
            char c;//刚刚读取的有效字符
            //s是英文单词缓冲，ct是当前一整行的缓冲
            StringBuffer s = new StringBuffer(), ct = new StringBuffer(ccs);
            ccs = null;

            //开始解析
            if (code == 3) {//gb2312
                while (loading) {
                    i = 0;
                }
                while (m < number_of_rows) {
                    if ((i = fis.read()) == -1) {
                        break;
                    }
                    if (i < 128) {//如果小于128，说明是单字节的ASC码
                        j = 0;
                        g = 1;
                        since_LastASCIndex = 0;
                    } else {//否则是双字节的中文字符，要再读取一个字节
                        i -= 161;
                        if (i > 14) {
                            i -= 6;
                        }
                        j = fis.read() - 160;
                        j = (j + i * 95) * 2;
                        if (j < 15385 && j >= 0) {
                            if ((i = gb[j]) < 0) {
                                i += 256;
                            }
                            if ((j = gb[j + 1]) < 0) {
                                j += 256;
                            }
                        } else {
                            i = 0;
                            j = 48;
                        }
                        g = 2;
                        since_LastASCIndex += 2;
                    }
                    c = (char) (i | (j << 8));
                    if (((c <= 122 && c >= 97) || (c <= 90 && c >= 65)) && w < d) {
                        s.append(c);
                        w = font.stringWidth(s.toString());
                        r += g;
                    } else {
                        k = font.charWidth(c);
                        n += (w + k);
                        a += (r + g);
                        if (n > d) {
                            if (n < d + k) {
                                r = w = 0;
                                ct.append(s);
                                s = null;
                                s = new StringBuffer();
                            }
                            b += a;
                            n = w + k;
                            a = r + g;
                            b -= a;
                            pfm.setCurrentContent(ct.toString());
                            m++;
                            ct = null;
                            ct = new StringBuffer();
                        }
                        if (w != 0) {
                            r = w = 0;
                            ct.append(s);
                            s = null;
                            s = new StringBuffer();
                        }
                        ct.append(c);
                        if (c == 10 && m < number_of_rows) {
                            b += a;
                            a = n = 0;
                            pfm.setCurrentContent(ct.toString());
                            m++;
                            ct = null;
                            ct = new StringBuffer();
                        }
                    }
                }
            } else if (code == 2) {//utf-8
                while (m < number_of_rows) {
                    if ((i = fis.read()) == -1) {
                        break;
                    }
                    j = i & 224;
                    if (j < 128) {
                        j = i;
                        g = 1;
                    } else if (j == 192) {
                        i &= 63;
                        j = fis.read() & 63;
                        j |= (i << 6);
                        g = 2;
                    } else {
                        i &= 15;
                        j = fis.read() & 63;
                        j |= (i << 6);
                        i = fis.read() & 63;
                        j = i | (j << 6);
                        g = 3;
                    }
                    c = (char) j;
                    if (((c <= 122 && c >= 97) || (c <= 90 && c >= 65)) && w < d) {
                        s.append(c);
                        w = font.stringWidth(s.toString());
                        r += g;
                    } else {
                        k = font.charWidth(c);
                        n += (w + k);
                        a += (r + g);
                        if (n > d) {
                            if (n < d + k) {
                                r = w = 0;
                                ct.append(s);
                                s = null;
                                s = new StringBuffer();
                            }
                            b += a;
                            n = w + k;
                            a = r + g;
                            b -= a;
                            pfm.setCurrentContent(ct.toString());
                            m++;
                            ct = null;
                            ct = new StringBuffer();
                        }
                        if (w != 0) {
                            r = w = 0;
                            ct.append(s);
                            s = null;
                            s = new StringBuffer();
                        }
                        ct.append(c);
                        if (c == 10 && m < number_of_rows) {
                            b += a;
                            a = n = 0;
                            pfm.setCurrentContent(ct.toString());
                            m++;
                            ct = null;
                            ct = new StringBuffer();
                        }
                    }
                }
            } else {//unicode
                g = 2;
                while (m < number_of_rows) {
                    if ((i = fis.read()) == -1) {
                        break;
                    }
                    j = fis.read();
                    c = (char) (i | (j << 8));
                    if (((c <= 122 && c >= 97) || (c <= 90 && c >= 65)) && w < d) {
                        s.append(c);
                        w = font.stringWidth(s.toString());
                        r += g;
                    } else {
                        k = font.charWidth(c);
                        n += (w + k);
                        a += (r + g);
                        if (n > d) {
                            if (n < d + k) {
                                r = w = 0;
                                ct.append(s);
                                s = null;
                                s = new StringBuffer();
                            }
                            b += a;
                            n = w + k;
                            a = r + g;
                            b -= a;
                            pfm.setCurrentContent(ct.toString());
                            m++;
                            ct = null;
                            ct = new StringBuffer();
                        }
                        if (w != 0) {
                            r = w = 0;
                            ct.append(s);
                            s = null;
                            s = new StringBuffer();
                        }
                        ct.append(c);
                        if (c == 10 && m < number_of_rows) {
                            b += a;
                            a = n = 0;
                            pfm.setCurrentContent(ct.toString());
                            m++;
                            ct = null;
                            ct = new StringBuffer();
                        }
                    }
                }
            }//解析结束
            //如果文本读取完毕，需要处理余下的不完整一帧，并且关闭输入流
            if (i == -1) {
                n += w;
                a += r;
                if (n > d) {
                    b += a;
                    a = r;
                    b -= a;
                    pfm.setCurrentContent(ct.toString());
                    m++;
                    ct = null;
                    ct = new StringBuffer();
                }
                ct.append(s);
                if (m < number_of_rows) {
                    pfm.setCurrentContent(ct.toString());
                    m++;
                    ct = null;
                    ct = new StringBuffer();
                    while (m < number_of_rows) {//如果一帧没有满，将空白的行补上
                        pfm.setCurrentContent("");
                        m++;
                    }
                    b += a;
                    a = 0;
                }
                if (ct.length() > 0) {
                    pfm.setTempContent(ct.toString());
                } else {
                    fis.close();//关闭输入流
                    fis = null;
                }
            } else {
                pfm.setTempContent(ct.toString());
            }
            //完成此次的帧
            pfm.completeFrame(b, a);
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    private int skipUTF8(int leap) throws IOException {
        //修正因跳转引起的字节非法组合
        int i;
        int j;
        int g = 0;
//        fis.skip(leap); //跳跃
        while (leap-- > 0) {//跳跃
            fis.read();
        }
        //修正因跳转引起的字节非法组合
        while (true) {
            i = fis.read();
            j = i & 224;
            if (j < 128) {
                //0xxxxxxx
                g++;
                break;
            } else if (j == 192) {
                //110xxxxx
                fis.read();
                g += 2;
                break;
            } else if (j > 192) {
                //111xxxxx
                fis.read();
                fis.read();
                g += 3;
                break;
            } else {
                //10xxxxxx
                g++;
            }
        }
        return g;
    }

    private int skipGB2312(int leap) throws IOException {
        int i, g = 0;
        while (leap-- > 0) {//跳跃
            if (fis.read() < 128) {
                since_LastASCIndex = 0;
            } else {
                since_LastASCIndex++;
            }
        }
        //修正因跳转引起的字节非法组合
        i = fis.read();
        if (i < 128) {//如果小于128，说明是单字节的ASC码
            since_LastASCIndex = 0;
            g = 1;
        } else if (((since_LastASCIndex) & 1) == 0) {//说明加上刚刚读取的那个字节
            //，距离上次的ASC码字符就有奇数个字节了
            //，于是还差一个字节就构成偶数个了，可以组成中文字符
            fis.read();
            since_LastASCIndex += 2;
            g = 2;
        } else {//说明加上刚刚读取的那个字节，距离上次的ASC码字符有偶数个字节,不用再读取了
            since_LastASCIndex++;
            g = 1;
        }
        return g;
    }

    private int skipUnicode(int leap) throws IOException {
        int g;
        if ((leap & 1) == 1) {//只能跳偶数个字节
            leap++;
            g = 1;
        } else {
            g = 0;
        }
        //@TODO 系统提供的skip函数不可靠
//        fis.skip(leap);//跳跃
        while (leap-- > 0) {//跳跃
            fis.read();
        }
        return g;
    }
}
