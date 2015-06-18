package edu.webframework.annotations;

public class HttpMethodType {

    public static final String GET = "GET";
    public static final String POST = "POST";

    public static boolean isGET(String methodType) {
        return GET.equals(methodType);
    }

    public static boolean isPOST(String methodType) {
        return POST.equals(methodType);
    }

}
