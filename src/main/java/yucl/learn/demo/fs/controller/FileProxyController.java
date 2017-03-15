package yucl.learn.demo.fs.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yucl.learn.demo.fs.service.HttpRequestProxyService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;


/**
 * @author chunlei.yu
 */
@RestController
public class FileProxyController {
    @Autowired
    private HttpRequestProxyService httpRequestProxyService;


    @RequestMapping(value = "/files", method = RequestMethod.POST, headers = "content-type!=multipart/form-data")
    public void uploadChunked(Principal principal, final HttpServletRequest request, final HttpServletResponse response) {
        httpRequestProxyService.handleChunkedRequest(request, response);
    }


    @RequestMapping(value = "/files", method = RequestMethod.POST, headers = "content-type=multipart/form-data")
    public void uploadMultipart2(Principal principal, final HttpServletRequest request, final HttpServletResponse response,
                                 @RequestParam(value = "file") MultipartFile multipartFile) {
        httpRequestProxyService.handleMultipartFormRequest(request, response, multipartFile);
    }


    @RequestMapping(value = "/newFile/{schema}", method = RequestMethod.POST)
    public void createNewFile(Principal principal, final HttpServletRequest request, final HttpServletResponse response,
                              @RequestParam(value = "fileName", required = true) String fileName,
                              @RequestParam(value = "fileLength", required = true, defaultValue = "0") long fileLength,
                              @RequestParam(value = "fileType", required = true) String contentType) {


    }

    @RequestMapping(value = "/uploadPartFile", method = RequestMethod.POST, headers = "content-type!=multipart/form-data")
    public void uploadChunked(final HttpServletRequest request, final HttpServletResponse response) {
        httpRequestProxyService.handleChunkedRequest(request, response);
    }


    @RequestMapping(value = "/uploadPartFile", method = RequestMethod.POST, headers = "content-type=multipart/form-data")
    public void uploadMultipart(Principal principal, final HttpServletRequest request, final HttpServletResponse response,
                                @RequestParam(value = "file") MultipartFile multipartFile) {
        httpRequestProxyService.handleChunkedRequest(request, response);
    }

    @RequestMapping(value = "/files/{fileId}", method = RequestMethod.GET)
    public void downloadFile(@PathVariable("fileId") String fileId, final HttpServletRequest request, HttpServletResponse response) {
        httpRequestProxyService.handleDownload(request, response, fileId);
    }


}
