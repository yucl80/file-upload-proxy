package yucl.learn.demo.fs.service;

import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by YuChunlei on 2017/3/15.
 */
public interface HttpRequestProxyService {
    void handleChunkedRequest(HttpServletRequest request, HttpServletResponse response);

    void handleMultipartFormRequest(HttpServletRequest request, HttpServletResponse response, MultipartFile multipartFile);

    void handleDownload(HttpServletRequest request, HttpServletResponse response, String fileId);
}
