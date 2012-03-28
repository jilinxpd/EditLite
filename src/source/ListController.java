/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package source;

/**
 *
 * @author Administrator
 */
public class ListController {

    //selectedIndex是以1开始计数的
    private int number_of_rows, begin, selectedIndex, bottom;
    boolean multi_select[];

    ListController(int number_of_rows) {
        this.number_of_rows = number_of_rows;
    }

    public void setList(int bottom) {
        begin = 0;
        selectedIndex = bottom > 0 ? 1 : 0;
        this.bottom = bottom;
        multi_select = null;
    }

    public int setMultiSelect(int index) {// 返回值是从0开始计数的,注意selectedIndex是从1开始计数的
        if (multi_select == null) {
            multi_select = new boolean[bottom];
        }
        if (index < -1) {//说明要反向选择全部
            for (index = 0; index < bottom; index++) {
                multi_select[index] = !multi_select[index];
            }
        } else if (index < 0) {//index==-1说明要取消选择全部
            multi_select = null;
        } else {//index==0说明要反向选择单个
            multi_select[index] = !multi_select[index];
        }
        return begin;
    }

    public int getWindowSelectedIndex() {
        return selectedIndex - 1;
    }

    public int getSelectedIndex() {// 返回值是从0开始计数的,注意selectedIndex是从1开始计数的
        return begin + selectedIndex - 1;
    }

    public int setWindowSelectedIndex(int index) {
        if (index + number_of_rows > bottom) {
            begin = bottom > number_of_rows ? bottom - number_of_rows : 0;
            selectedIndex = index - begin + 1;
        } else {
            selectedIndex = index % number_of_rows + 1;
            begin = index - selectedIndex + 1;
        }
        return begin;
    }

    public int getNumberOfRows() {
        return bottom;
    }

    public int getWindowNumberOfRows() {
        return bottom > number_of_rows ? number_of_rows : bottom;
    }

    public int next() {
        if (selectedIndex < number_of_rows && selectedIndex < bottom) {
            selectedIndex++;
        } else if (begin + number_of_rows < bottom) {
            begin++;
        } else if (selectedIndex > 0) {
            begin = 0;
            selectedIndex = 1;
        } else {//index==0
            return -1;
        }
        return begin;
    }

    public int last() {
        if (selectedIndex > 1) {
            selectedIndex--;
        } else if (begin > 0) {
            begin--;
        } else if (selectedIndex > 0) {
            selectedIndex = bottom > number_of_rows ? number_of_rows : bottom;
            begin = bottom - selectedIndex;
        } else {//index==0
            return -1;
        }
        return begin;
    }

    public int skipLast() {
        if (selectedIndex > 1) {
            selectedIndex = 1;
        } else if (begin > 0) {
            begin -= number_of_rows;
            if (begin < 0) {
                begin = 0;
            }
        } else if (selectedIndex > 0) {
            selectedIndex = bottom > number_of_rows ? number_of_rows : bottom;
            begin = bottom - selectedIndex;
        } else {//index==0
            return -1;
        }
        return begin;
    }

    public int skipNext() {
        int min = bottom > number_of_rows ? number_of_rows : bottom;
        if (selectedIndex < min) {
            selectedIndex = min;
        } else if (selectedIndex > 0) {
            begin += number_of_rows;
            if (begin >= bottom) {
                begin = 0;
                selectedIndex = 1;
            } else if (begin + number_of_rows > bottom) {
                begin = bottom - number_of_rows;
            }
        } else {//index==0
            return -1;
        }
        return begin;
    }
}
