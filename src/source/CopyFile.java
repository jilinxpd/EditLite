/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package source;

import java.io.*;
import javax.microedition.io.*;
import javax.microedition.io.file.*;

class CopyFile {

    private String sfp, ddn, dfn, text;
    private FileConnection sfc, dfc;
    private int front, skip, rest, off_w;
    private int box;
    private int target_char_code, original_char_code;
    private byte data[][], dic0[], dic1[];
    private boolean del, rename, convert;
    private final Object semaphore = new Object();

    public void set(String s_file_path, String d_file_name, String d_dir_name, String text_content, int tc, int oc, int o, int s, int l, boolean to_ren, boolean to_del) {
        sfp = s_file_path;
        dfn = d_file_name;
        ddn = d_dir_name;
        target_char_code = tc;
        original_char_code = oc;
        front = o;
        skip = s;
        rest = l;
        rename = to_ren;
        del = to_del;
        convert = (tc != oc);
        if (text_content != null && text_content.equals("")) {
            text = null;
        } else {
            text = text_content;
        }
        off_w = 0;
    }

    //@TODO 已完成 源文件目标文件重命名问题
    public void startCopy() {
        try {
            //@TODO 保存功能还未测试
            if (!convert && rest > 0 && front >= rest + 10240) {//如果开始部分大于结尾部分一定
                //的数量（这里是10240字节），则将结尾部分复制到一个临时文件中暂存，
                //然后将中间部分添加到开始部分的后面，最后将结尾部分复制回来
                int front_b = front, skip_b = skip;
                String text_b = text;
                front = 0;
                skip = skip_b + front_b;
                text = null;
                openFile(sfp, ddn, "_", true);//临时文件作为目标文件
                copyProcess();
                sfc.close();
                dfc.close();
                sfc = null;
                dfc = null;
                off_w = front_b;
                front = 0;
                skip = 0;
//                text = text_b;
                rename = false;
                openFile(ddn + "_", "", sfp, false);//临时文件作为源文件
            } else {
                if (convert) {
                    if (original_char_code == 1) {//unicode
                        front -= 2;
                    } else if (original_char_code == 2) {//utf8
                        front -= 3;
                    }
                }
                openFile(sfp, ddn, dfn, true);
            }
            copyProcess();
            if (del) {
                sfc.delete();
            }
            if (sfp != null) {
                sfc.close();
                sfc = null;
            }
            if (rename) {
                dfc.rename(dfn);
            }
            dfc.close();
            dfc = null;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.gc();
    }

//    public void TEST() {
//        try {
//            front = (int) sfc.fileSize();
//            rest = 0;
//            skip = 0;
//            text = null;
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
//    }
    private void openFile(String sfp, String ddn, String dfn, boolean create_df) throws Exception {
        if (sfp != null) {
            sfc = (FileConnection) Connector.open("file:///" + sfp);
            if (front + skip + rest == 0) {//如果是复制文件，就获取文件的大小
                front = (int) sfc.fileSize();
            }
        }
        dfc = (FileConnection) Connector.open("file:///" + ddn + dfn);
        if (create_df) {
            StringBuffer sb = new StringBuffer(), st;
            while (dfc.exists()) {
                dfc.close();
                dfc = null;
                sb.append("复件_");
                st = new StringBuffer("file:///");
                st.append(ddn);
                st.append(sb);
                st.append(dfn);
                dfc = (FileConnection) Connector.open(st.toString());
                st = null;
            }
            sb = null;
            dfc.create();
        }
    }

    private void copyProcess() throws Exception {
        if (front > 0 || rest > 0) {
            data = new byte[10][];
            for (box = 0; box < 10; box++) {
                data[box] = new byte[2052];//比2048多4个字节，用于防止前一个缓冲
                //区出现零头字节，可以把它们放在下一个缓冲区的头4个字节中
            }
            data[0][0] = 4;//置初值
            new Thread(new Runnable() {

                private int a, b, p, c;
                private boolean xy;
                private InputStream is;

                public void run() {
                    try {
                        is = sfc.openInputStream();
                        if (convert) {
                            //去掉BOM
                            if (original_char_code == 1) {//unicode
                                is.read();
                                is.read();
                            } else if (original_char_code == 2) {//utf8
                                is.read();
                                is.read();
                                is.read();
                            }
                        }
                        a = front;
                        b = 2048;
                        p = 0;
                        xy = (skip > 0 || rest > 0);
                        while (true) {
                            while (a > 0) {
                                if (a < 2048) {
                                    b = a;
                                }
                                a -= 2048;
                                synchronized (semaphore) {
                                    while (box < 1) {
                                        semaphore.wait();
                                    }
                                    semaphore.notify();
                                }
                                c = 0;
                                while (c < b) {//这里必须用一个循环来读取字节，
                                    //然后判断是否读取了指定数量的字节，因为read函数
                                    //要不停的缓冲，当缓冲区中的字节不够时，它会从
                                    //硬盘读取，这时有可能data没有填完read函数就返回了
                                    c += is.read(data[p], 4 + c, b - c);
                                }
                                synchronized (semaphore) {
                                    box--;
                                    semaphore.notify();
                                }
                                p = (p + 1) % 10;
                            }
                            if (xy) {
                                xy = false;
                                //@TODO这里改不改呢 系统提供的skip在数量太大时会出现问题
                                is.skip(skip);
                                a = rest;
                                b = 2048;
                            } else {
                                break;
                            }
                        }
                        is.close();
                        is = null;
                    } catch (Exception x) {
                        x.printStackTrace();
                    }
                }
            }).start();
        }
        int a, b, q, c, t;
        boolean xy;
        OutputStream os;
        byte temp[], temp1[], temp2[];
        try {
            os = dfc.openOutputStream(off_w);
            if (convert) {
                if (target_char_code == 3) {//gb2312
                    beginU2GConvert();
                } else {
                    if (original_char_code == 3) {//gb2312
                        beginG2UConvert();
                    }
                    if (target_char_code == 2) {//utf8
                        //BOM
                        os.write(0XEF);
                        os.write(0XBB);
                        os.write(0XBF);
                    } else if (target_char_code == 1) {//unicode
                        //BOM
                        os.write(0XFF);
                        os.write(0XFE);
                    }
                }
            }
            a = front;
            b = 2048;
            c = 4;
            q = 0;
            xy = (text != null || rest > 0);
            while (true) {
                while (a > 0) {
                    if (a < 2048) {
                        b = a;
                    } else {
                        b = 2048;
                    }
                    a -= 2048;
                    synchronized (semaphore) {
                        while (box > 9) {
                            semaphore.wait();
                        }
                        semaphore.notify();
                    }
                    temp = data[q];
                    if (convert) {
                        if (original_char_code != 1) {//不是unicode
                            c = temp[0];//当前缓冲区的开始字节
                            temp[0] = 0;//未处理前，当前缓冲区零头初始为0个字节
                            if (original_char_code == 2) {//utf8
                                temp = UTF82Unicode(temp, c, b + 4);
                            } else {//if (original_char_code == 3) {//gb2312
                                temp = GB231122Unicode(temp, c, b + 4);
                            }
                            c = 0;//当前缓冲区的开始字节
                            b = temp.length;//当前缓冲区的有效字节长度
                            //现在data[q][0]里面存的是当前缓冲区的零头的字节数
                            t = 4 - data[q][0];//下一个缓冲区的开始字节
                            temp2 = data[(q + 1) % 10];//下一个缓冲区
                            temp2[0] = (byte) t;//修改下一个缓冲区的开始字节位置
                            if (t < 4) {//如果有零头字节
                                temp1 = data[q];
                                while (t < 4) {//复制到下一个缓冲区中，并修改下一个缓冲区的开始字节位置
                                    temp2[t] = temp1[t + 2048];
                                    t++;
                                }
                            }
                        } else {
                            c = 4;
                        }
                        if (target_char_code != 1) {//不是unicode
                            if (target_char_code == 2) {//utf8
                                temp = Unicode2UTF8(temp, c, b + c);
                            } else {//if (target_char_code == 3) {//gb2312
                                temp = Unicode2GB2312(temp, c, b + c);
                            }
                            c = 0;//当前缓冲区的开始字节
                            b = temp.length;//当前缓冲区的有效字节长度
                        }
                    }
                    os.write(temp, c, b);
                    synchronized (semaphore) {
                        box++;
                        semaphore.notify();
                    }
                    q = (q + 1) % 10;
                }
                if (xy) {
                    xy = false;
                    if (text != null) {
                        insert(os, text, target_char_code);
                    }
                    a = rest;
                    b = 2048;
                } else {
                    break;
                }
            }
            if (convert) {
                if (original_char_code == 3 || target_char_code == 3) {//gb2312
                    endConvert();
                }
            }
            os.flush();
            os.close();
            os = null;
            data = null;
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    private void insert(OutputStream os, String textContent, int char_code) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        byte dat[] = null;
        if (char_code == 1) {//unicode
            dos.writeChars(textContent);
            textContent = null;
            dat = baos.toByteArray();
            invertUnicode(dat);
            os.write(dat);
        } else if (char_code == 2) {//utf-8
            dos.writeUTF(textContent);
            textContent = null;
            dat = baos.toByteArray();
            os.write(dat, 2, dat.length - 2);
        } else {//gb2312
            dos.writeChars(textContent);
            textContent = null;
            byte[] b = baos.toByteArray();
            invertUnicode(b);
            if (dic1 == null) {//还没有载入字库
                beginU2GConvert();
                dat = Unicode2GB2312(b, 0, b.length);
                endConvert();
            } else {
                dat = Unicode2GB2312(b, 0, b.length);
            }
            b = null;
            os.write(dat);
        }
        dat = null;
        dos.close();
        baos.close();
        dos = null;
        baos = null;
    }

    private void invertUnicode(byte data[]) {
        int i, j;
        byte t;
        for (i = 0, j = data.length; i < j; i += 2) {
            t = data[i];
            data[i] = data[i + 1];
            data[i + 1] = t;
        }
    }

    private void beginU2GConvert() throws Exception {
        dic0 = new byte[14890];
        dic1 = new byte[14890];
        getClass().getResourceAsStream("/c/u0").read(dic0);
        getClass().getResourceAsStream("/c/u1").read(dic1);
    }

    //其中data为unicode反序 其中i为起始 j为结束 包含i但不包含j
    private byte[] Unicode2GB2312(byte data[], int i, int j) throws Exception {
        int l, h, m = 0, n1 = 0, n2 = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        while (i < j) {
            if (data[i + 1] == 0 && data[i] < 0x80) {//原则上不能直接用byte型的与int型的相比较
                //必须补全前面的字节转化为int后才可以比较 但是这里强制转化的结果跟提升字节的结果是一样的
                //所以不用麻烦提升字节了
                dos.writeByte(data[i]);// 西文字符
            } else {//汉字
                l = 0;
                h = 7444;
                while (l <= h) {//二分查找
                    m = (l + h) / 2;
                    n1 = ((dic0[2 * m] & 0xff) << 8) + (dic0[2 * m + 1] & 0xff);//一定要记住+号的优先级大于<<  所以要加括号
                    n2 = ((data[i + 1] & 0xff) << 8) + (data[i] & 0xff);
                    if (n2 > n1) {
                        l = m + 1;
                    } else if (n2 < n1) {
                        h = m - 1;
                    } else {
                        break;
                    }
                }
                if (n2 == n1) {
                    dos.writeByte(dic1[2 * m]);
                    dos.writeByte(dic1[2 * m + 1]);// 成功将unicode映射为gb2312
                } else {
                    dos.writeByte(0x1B);// 不能正常映射就写入0x1b
                }
            }
            i += 2;
        }
        dos.flush();
        baos.flush();
        return baos.toByteArray();
    }

    private void endConvert() {
        dic0 = null;
        dic1 = null;
        System.gc();
    }

    //其中data为unicode反序 其中i为起始 j为结束 包含i但不包含j
    private byte[] Unicode2UTF8(byte data[], int i, int j) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        while (i < j) {
            if (data[i + 1] == 0 && data[i] < 0x80) {
                dos.writeByte(data[i]);
            } else if ((data[i + 1] & 0xFF) < 0x08) {
                dos.writeByte(0xC0 | ((data[i + 1] & 0x07) << 2) | ((data[i] & 0xC0) >> 6));
                dos.writeByte(0x80 | (data[i] & 0x3F));
            } else if ((data[i + 1] & 0xFF) <= 0xFF) {
                dos.writeByte(0xE0 | ((data[i + 1] & 0xF0) >> 4));
                dos.writeByte(0x80 | ((data[i + 1] & 0x0F) << 2) | ((data[i] & 0xC0) >> 6));
                dos.writeByte(0x80 | (data[i] & 0x3F));
            }
            i += 2;
        }
        dos.flush();
        baos.flush();
        return baos.toByteArray();
    }

    //其中返回值为unicode反序 其中i为起始 j为结束 包含i但不包含j
    private byte[] UTF82Unicode(byte data[], int i, int j) throws Exception {
        int n;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        while (i < j) {
            n = data[i] & 0xE0;
            if (n < 0x80) {
                n = data[i] & 0xFF;
                i++;
            } else if (n == 0xC0) {
                if (i + 1 >= j) {//如果不够
                    data[0] = (byte) (j - i);//保存零头字节的个数
                    break;
                }
                n = ((data[i] & 0x1F) << 6) | (data[i + 1] & 0x3F);
                i += 2;
            } else {
                if (i + 2 >= j) {//如果不够
                    data[0] = (byte) (j - i);//保存零头字节的个数
                    break;
                }
                n = ((data[i] & 0x0F) << 12) | ((data[i + 1] & 0x3F) << 6) | (data[i + 2] & 0x3F);
                i += 3;
            }
            dos.writeByte(n);
            dos.writeByte(n >> 8);
        }
        dos.flush();
        baos.flush();
        return baos.toByteArray();
    }

    private void beginG2UConvert() throws Exception {
        dic0 = new byte[15386];
        getClass().getResourceAsStream("/c/g").read(dic0);
    }

    //其中返回值为unicode反序 其中i为起始 j为结束 包含i但不包含j
    private byte[] GB231122Unicode(byte data[], int i, int j) throws Exception {
        int n, m, t;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        while (i < j) {
            n = data[i] & 0xFF;
            if (n < 0x80) {//如果小于128，说明是单字节的ASC码
                m = 0;
                i++;
            } else {//否则是双字节的中文字符，要再读取一个字节
                if (i + 1 >= j) {//如果不够
                    data[0] = (byte) (j - i);//保存零头字节的个数
                    break;
                }
                n -= 161;
                if (n > 14) {
                    n -= 6;
                }
                m = (data[i + 1] & 0xFF) - 160;
                t = (m + n * 95) * 2;
                if (t < 15385 && t >= 0) {
                    if ((n = dic0[t]) < 0) {//注意dic0字典中的字是unicode反序的
                        n += 256;
                    }
                    if ((m = dic0[t + 1]) < 0) {
                        m += 256;
                    }
                } else {
                    n = 48;
                    m = 0;
                }
                i += 2;
            }
            dos.writeByte(n);
            dos.writeByte(m);
        }
        dos.flush();
        baos.flush();
        return baos.toByteArray();
    }
}
