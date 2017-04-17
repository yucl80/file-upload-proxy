package yucl.learn.demo.fs.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by YuChunlei on 2017/3/15.
 */
public interface AsyncHttpRequestProxyService {
    DeferredResult<ResponseEntity>  handleChunkedRequest(HttpServletRequest request) ;

    DeferredResult<ResponseEntity> handleMultipartFormRequest(HttpServletRequest request, MultipartFile multipartFile) ;

    StreamingResponseBody  handleDownload( String fileId, HttpServletRequest request, HttpServletResponse response);
}
