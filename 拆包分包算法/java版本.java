package com.fanjun.messagecenter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test {
    public static void main(String[] args) {

        //开始符
        final String START_TAG = "&1&1";
        //结束符
        final String END_TAG = "&2&2";

        //socket收到的源数据
        String sourceData = "&2123123123123" + START_TAG + "123是你吗" + END_TAG + START_TAG + "你好你好" + END_TAG + START_TAG + "打趴" + END_TAG + END_TAG + "123";
        //缓存的粘包数据
        String cutData = START_TAG + "老的粘包" + "&2";

        //新数据拼接老的黏包，注意拼接数据，老的在前面
        String newSourceData = cutData + sourceData;
        //查找新数据源需要立即分发的数据，一组START_TAG + END_TAG构成一个分发数据，但要注意处理中间值为空的情况，在极端情况可能出现无效数据，注意处理异常
        List<String> handData = findHandData(newSourceData, START_TAG, END_TAG);
        //整理出最新的粘包数据
        cutData = sortCutData(newSourceData, START_TAG, END_TAG);

        System.out.println("新的粘包数据：" + cutData);
        System.out.println("需要分发的数据：" + handData.toString());

    }

    /**
     * 查找符合开始和结束标识的字符串
     * 如“startTag你好endTagstartTag朋友endTag”，得到“你好、朋友”
     *
     * @param sourceData
     * @param startTag
     * @param endTag
     * @return
     */
    public static List<String> findHandData(String sourceData, String startTag, String endTag) {
        List<String> handData = new ArrayList<>();
        String regex = startTag + ".*?" + endTag;
        String tempSourceData = sourceData;
        String data = null;
        while (true) {
            if (data != null) {
                tempSourceData = tempSourceData.substring(tempSourceData.indexOf(data) + data.length(), tempSourceData.length());
            }
            data = filerStartEnd(tempSourceData, regex);
            if (data != null) {
                //如果存在多个startTag开始符异常数据时，以最接近endTag结束符号的startTag开始符号为准
                if (data.indexOf(startTag) < data.lastIndexOf(startTag)) {
                    String tempData = data.substring(data.lastIndexOf(startTag), data.length());
                    handData.add(tempData);
                } else {
                    handData.add(data);
                }

            } else {
                break;
            }
        }
        return handData;
    }

    /**
     * 递归取出符合首位符号规则的字符串
     *
     * @param sourceData
     * @param regex
     * @return
     */
    public static String filerStartEnd(String sourceData, String regex) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(sourceData);
        if (m.find()) {
            return m.group();
        }
        return null;
    }

    /**
     * 整理粘包数据
     *
     * @param sourceData
     * @param startTag
     * @param endTag
     * @return
     */
    public static String sortCutData(String sourceData, String startTag, String endTag) {
        String newSourceData = sourceData;
        int startTagLastPosition = newSourceData.lastIndexOf(startTag);
        int endTagLastPosition = newSourceData.lastIndexOf(endTag);
        if (startTagLastPosition >= 0) {
            //如果存在startTag标识，但不存在endTag标识，则将startTag标识以后的作为粘包数据
            if (endTagLastPosition < 0) {
                newSourceData = newSourceData.substring(startTagLastPosition, newSourceData.length());
            } else {
                //如果存在startTag/endTag标识，但startTag在endTag后，则以startTag标识以后的作为粘包数据
                if (endTagLastPosition < startTagLastPosition) {
                    newSourceData = newSourceData.substring(startTagLastPosition, newSourceData.length());
                } else {
                    //如果存在startTag/endTag标识，但startTag在endTag前，则后续数据可能为不完整的粘包数据
                    newSourceData = newSourceData.substring(endTagLastPosition, newSourceData.length());
                }
            }
        }
        return newSourceData;
    }
}
