package edu.webframework;

import java.io.Serializable;
import java.util.List;

public class TestBean implements Serializable {

    private String strValue;
    private Integer intValue;
    private Float floatValue;
    private Double doubleValue;
    private List<String> listStr;
    private List<Integer> listInt;

    private List<TestBean> moreBeans;

    public String getStrValue() {
        return strValue;
    }

    public void setStrValue(String strValue) {
        this.strValue = strValue;
    }

    public Integer getIntValue() {
        return intValue;
    }

    public void setIntValue(Integer intValue) {
        this.intValue = intValue;
    }

    public Float getFloatValue() {
        return floatValue;
    }

    public void setFloatValue(Float floatValue) {
        this.floatValue = floatValue;
    }

    public Double getDoubleValue() {
        return doubleValue;
    }

    public void setDoubleValue(Double doubleValue) {
        this.doubleValue = doubleValue;
    }

    public List<String> getListStr() {
        return listStr;
    }

    public void setListStr(List<String> listStr) {
        this.listStr = listStr;
    }

    public List<Integer> getListInt() {
        return listInt;
    }

    public void setListInt(List<Integer> listInt) {
        this.listInt = listInt;
    }

    public List<TestBean> getMoreBeans() {
        return moreBeans;
    }

    public void setMoreBeans(List<TestBean> moreBeans) {
        this.moreBeans = moreBeans;
    }

}
