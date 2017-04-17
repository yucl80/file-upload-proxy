package yucl.learn.demo.fs.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;
import yucl.learn.demo.fs.service.AsyncHttpRequestProxyService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;

/**
 * Created by YuChunlei on 2017/4/17.
 */
@RestController
@RequestMapping("/async")
public class AsyncFileProxyController {
    private static final Logger log = LoggerFactory.getLogger(AsyncFileProxyController.class);

    @Autowired
    private AsyncHttpRequestProxyService asyncHttpRequestProxyService;

    @RequestMapping(value = "/uploadPartFile", method = RequestMethod.POST, headers = "content-type!=multipart/form-data")
    public DeferredResult uploadChunked(HttpServletRequest request) {
        return  asyncHttpRequestProxyService.handleChunkedRequest(request);
    }


    @RequestMapping(value = "/uploadPartFile", method = RequestMethod.POST, headers = "content-type=multipart/form-data")
    public DeferredResult uploadMultipart(Principal principal, final HttpServletRequest request, final HttpServletResponse response,
                                @RequestParam(value = "file") MultipartFile multipartFile) {
        return  asyncHttpRequestProxyService.handleMultipartFormRequest(request,multipartFile);
    }

    @RequestMapping(value = "/files/{fileId}", method = RequestMethod.GET)
    public void downloadFile(@PathVariable("fileId") String fileId, final HttpServletRequest request, HttpServletResponse response) {
        asyncHttpRequestProxyService.handleDownload(fileId,request,response);
    }






}
