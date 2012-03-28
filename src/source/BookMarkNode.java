/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package source;

/**
 *
 * @author Administrator
 * 用于
 */
public class BookMarkNode {

    public int atop;
    public int begin;
    public int begin_alias;
    public int bottom;
    public int code;
    public String file_path;

    BookMarkNode(int atop, int begin, int begin_alias, int bottom, int code, String file_path) {
        this.atop = atop;
        this.begin = begin;
        this.begin_alias = begin_alias;
        this.bottom = bottom;
        this.code = code;
        this.file_path = file_path;
    }
}
