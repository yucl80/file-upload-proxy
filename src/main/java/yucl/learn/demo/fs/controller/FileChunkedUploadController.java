package yucl.learn.demo.fs.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yucl.learn.demo.fs.service.ResumableUploadService;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author chunlei.yu
 */
@RestController
public class FileChunkedUploadController {
    private static final Logger logger = LoggerFactory.getLogger(FileChunkedUploadController.class);
    private static final Pattern contentRangePattern = Pattern.compile("bytes ([0-9]+)-([0-9]+)/([0-9]+)");
    private ResumableUploadService resumableUploadService;

    @Autowired
    public void setResumableUploadService(ResumableUploadService resumableUploadService) {
        this.resumableUploadService = resumableUploadService;
    }

    @RequestMapping(value = "/uploadMultipart", method = RequestMethod.POST, headers = "content-type=multipart/form-data")
    public ResponseEntity uploadMultipart(Principal principal,
                                          @RequestHeader(value = "Content-Range", required = true) String contentRange,
                                          @RequestParam(value = "fileName", required = false) String fileName,
                                          @RequestParam(value = "fileType", required = true) String contentType,
                                          @RequestParam(value = "fileUID", required = true) String fileUID,
                                          @RequestParam(value = "file") MultipartFile multipartFile)
            throws IOException, InterruptedException, ExecutionException {
        Matcher contentRangeMatcher = contentRangePattern.matcher(contentRange);
        if (contentRangeMatcher.find()) {
            long position = Long.parseLong(contentRangeMatcher.group(1));
            long contentLength = multipartFile.getSize();
            long fileSize = Long.parseLong(contentRangeMatcher.group(3));
            Map resultMap = resumableUploadService.resumableUploadHandle(fileUID, multipartFile.getInputStream(), position, contentLength, fileSize);
            return new ResponseEntity<>(resultMap, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

    }
}

