/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package source;

/**
 *virtual page files manager虚拟页面管理器
 * @author xpd
 */
public class PFM {

    //frame_byte_count[i]是到第i帧为止(包含第i帧)已读取的字节总数,group_byte_count[i]是第i组之前有多少个字节
    private String content[];
    private String temp_content;
    //frame_byte_count是一个循环数组，group_byte_count是一个递增数组
    private int group_byte_count[], frame_byte_count[], frames, number_of_rows, volumes, a_top, b_top, top, bottom, current, peak;
    private Text text;
    private boolean full;

    public PFM(String[] c, int n, int f, Text t) {
        content = c;
        number_of_rows = n;
        frames = f;
        text = t;
        volumes = frames + number_of_rows;
        top = 0;
        frame_byte_count = new int[30];
        group_byte_count = new int[30];
    }

    void destroy() {
        content = null;
        frame_byte_count = null;
        group_byte_count = null;
    }

    void reset(int i, boolean reset) {
        if (reset) {//重置存储游标
            current = 0;
        } else {//继续按原位置向下存储
            top = current;// 将top重置
            peak = current;//将peak重置
        }
        bottom = top;
        a_top = i;
        b_top = i;
        frame_byte_count[0] = i;
        group_byte_count[0] = i;
        full = false;
    }

    void setCurrentContent(String s) {
        content[current++] = s;
        if (current == frames) {
            full = true;
        } else if (current == volumes) {
            current = 0;
        }
    }

    void setTempContent(String s) {
        temp_content = null;
        temp_content = s;
    }

    String getTempContent() {
        return temp_content;
    }

    String getContentX(int i) {
        return content[i % volumes];
    }

    int getByteCountX(int i) {
        return frame_byte_count[i % 30];
    }

    int getSkipByteCount() {
        return a_top;
    }

    int getTop() {
        return top;
    }

    int getBottom() {
        return bottom;
    }

    //对外隐藏存储空间的大小，当索引不在实际的索引空间中时，要读取那些内容，然后定位正确的帧，返回该帧的索引
    int findFrameByIndex(int index) {
        if (index < peak) {//如果要求的内容不在缓冲区中，并且不在可索引的区域
            int t = (top < number_of_rows) ? a_top : group_byte_count[0];
            if (t > 0) {//如果不是文件开始，则要重新开始读取文件
                //@TODO 实现无索引的跳转 跳转估计成功了 可是没有正确定位到想要的帧
                index = findFrameByByte(t - 1);
            }
        } else if (index < top) {//如果要求的内容不在缓冲区中,但在可索引的区域
            int t = index / frames;
            top = t * frames;
            text.reloadFile(group_byte_count[t]);//先跳过group_byte_count[t]个字节
            text.limitedJump(index + number_of_rows);//然后读取到包含index的那个帧为止
        } else if (index >= bottom) {//如果要求的内容不在缓冲区中，并且不在可索引的区域
            text.limitedJump(index + number_of_rows);
            if (index >= bottom) {
                index = -1;
            }
        }
        return index;
    }


    //@TODO 跳转有问题 试了几个unicode 60% 100% 都有问题
    //对外隐藏存储空间的大小，当字节号不在实际的字节空间中时，要读取那些内容，然后定位正确的帧，返回该帧的索引
    int findFrameByByte(int leap) {
        int i, j, t;
        if (leap < a_top) {
            i = 0;
            j = (top - number_of_rows) / frames;
            while (i <= j) {
                if (leap <= group_byte_count[i]) {
                    break;
                }
                i++;
            }
            if (i > 0) {//说明在group_byte_count中找到了，可以根据记录快速跳转
                t = group_byte_count[i - 1];
            } else {//说明在group_byte_count[0]之前，所以要从头开始跳转
                t = 0;
            }
            top = 0;
            text.reloadFile(t);
            text.freeJump(leap - t, t);
        } else if (leap > (t = getByteCountX(bottom / number_of_rows))) {
            top = 0;
//            System.out.println(t);
//            System.out.println(leap - t);
//            System.out.println(leap );
            text.freeJump(leap - t, t);
        }
        //二分查找
        i = 0;
        j = (bottom - top) / number_of_rows - 1;//这里上界从29改为现在的这个变量表达式
        int m = 0, b = (top / number_of_rows) % 30, a = j;
        while (i <= j) {
            m = (i + j) / 2;
            t = frame_byte_count[(m + b) % 30];
            if (t == leap) {
                break;
            } else if (t < leap) {
                i = m + 1;
            } else {
                j = m - 1;
            }
        }
        t = m * number_of_rows + top;//将要返回的当前帧的索引
        //@TODO 已完成 保证当前帧的前后两帧也在窗口中
        if (m == 0) {//如果当前帧是第一个帧
            if (a_top > 0) {//判断是否是文本开头，如果不是，则将top向前退一帧，让当前帧的前面一帧也在窗口中
                top -= number_of_rows;
                bottom -= number_of_rows;
                if (top < peak) {
                    top += volumes;
                    bottom += volumes;
                    t += volumes;
                    peak = top;
                }
            }
        } else if (m == a) {//如果当前帧是最后一帧
            text.limitedJump(bottom + number_of_rows);
        }
        return t;
    }

    void completeFrame(int b, int a) {
        bottom += number_of_rows;//一帧完成后，bottom要增加一帧的大小
        int i = (bottom / number_of_rows) % 30;
        if (bottom >= top + frames) {
            a_top = b_top;//frames缓冲区之前有多少个字节
            b_top = frame_byte_count[i];//frame_byte_count[r]将在下一次读取时被覆盖，所以要保存
            if (bottom > top + frames) {//如果缓冲区窗口大小(bottom - top)大于frames,则增加top以减小窗口大小
                top += number_of_rows;
            }
            //之所以不能随便更改top 是因为top要用来标记当前处于哪个group_byte_count
            if (full) {//一个完整的块缓冲区被填满
                int j = top / frames, k = group_byte_count.length;
                if (j > k) {
                    increaseBlockByteCount(k);
                }
                group_byte_count[j] = a_top;//当前缓冲区之前有多少个字节
                full = false;
            }
        }
        frame_byte_count[(i + 29) % 30] += b;
        frame_byte_count[i] = (frame_byte_count[(i + 29) % 30] + a);
    }

    private void increaseBlockByteCount(int k) {
        int temp[] = new int[k * 2];
        System.arraycopy(group_byte_count, 0, temp, 0, k);
        group_byte_count = null;
        group_byte_count = temp;
        System.gc();
    }
}
