package yucl.learn.demo.fs;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.CountingInputStream;
import org.springframework.ui.Model;
import sun.reflect.generics.visitor.Visitor;

import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by yuchunlei on 2017/3/8.
 */
public class ApiTest {
    private static final Pattern contentRangePattern = Pattern.compile("bytes ([0-9]+)-([0-9]+)/([0-9]+)");


    public static void main(String[] args){
        String fn = "aa.mov";
        int idx = fn.lastIndexOf("/") ;
        if(idx != -1){
            System.out.println( fn.substring(idx+1));
        }
         System.out.println(idx);
        System.out.println( fn);


    }

    @SuppressWarnings(value = { "resource" })
    public void load(InputStream is) throws Exception {
        CountingInputStream countingIs = new CountingInputStream(is);
        HashingInputStream hashingIs = new HashingInputStream(Hashing.md5(), countingIs);
        hashingIs.hash().toString();
    }

    private static String extractFileName(String contentDisp) {
        String[] items = contentDisp.split(";");
        for (String s : items) {
            String ss = s.trim() ;
            if ( ss.startsWith("filename")) {
                System.out.println(ss);
                return ss.substring(ss.indexOf("=") + 1);
            }
        }
        return "";
    }
}
