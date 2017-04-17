package yucl.learn.demo.fs.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Created by YuChunlei on 2017/4/17.
 */
public interface ResumableUploadService {
    Map resumableUploadHandle(String fileUID, InputStream inputStream, long position, long contentLength, long fileSize) throws IOException;
}
