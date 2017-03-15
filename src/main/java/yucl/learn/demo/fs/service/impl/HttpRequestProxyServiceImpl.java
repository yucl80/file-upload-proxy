package yucl.learn.demo.fs.service.impl;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import yucl.learn.demo.fs.cfg.AppProperties;
import yucl.learn.demo.fs.service.HttpRequestProxyService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.StandardOpenOption;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by YuChunlei on 2017/3/15.
 */
@Service
public class HttpRequestProxyServiceImpl implements HttpRequestProxyService {
    private static final Logger logger = LoggerFactory.getLogger(HttpRequestProxyServiceImpl.class);

    @Autowired
    private AppProperties appProperties;

    @Override
    public void handleChunkedRequest(final HttpServletRequest request, final HttpServletResponse response) {
        try {
            Part fileItem = request.getPart("file");
            URL myURL = new URL(appProperties.getFileUploadUri());
            HttpURLConnection httpURLConnection = (HttpURLConnection) myURL.openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("charset", request.getCharacterEncoding());
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if (!headerName.equalsIgnoreCase("content-type"))
                    httpURLConnection.setRequestProperty(headerName, request.getHeader(headerName));
            }
            httpURLConnection.setRequestProperty("Content-Disposition", "attachment; filename=" + fileItem.getName());
            httpURLConnection.setRequestProperty("Content-Type", fileItem.getContentType());
            httpURLConnection.setRequestProperty("Content-Length", Long.toString(fileItem.getSize()));
            IOUtils.copyLarge(fileItem.getInputStream(), httpURLConnection.getOutputStream());

            response.setStatus(httpURLConnection.getResponseCode());
            Map<String, List<String>> headerFields = httpURLConnection.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
                for (String fieldVal : entry.getValue()) {
                    //if (!fieldVal.equalsIgnoreCase("content-length"))
                    //response.addHeader(entry.getKey(), fieldVal);
                    System.out.println(entry.getKey() + ":" + fieldVal);
                }
            }
            IOUtils.copy(httpURLConnection.getInputStream(), response.getOutputStream());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } catch (ServletException e) {
            logger.error(e.getMessage(), e);
        }
    }


    public void handleMultipartFormRequest(final HttpServletRequest request, final HttpServletResponse response, MultipartFile multipartFile) {
        CommonsMultipartFile commonsMultipartFile = (CommonsMultipartFile) multipartFile;
        FileItem fileItem = commonsMultipartFile.getFileItem();
        DiskFileItem diskFileItem = (DiskFileItem) fileItem;
        File file = new File(diskFileItem.getStoreLocation().getAbsolutePath());
        try {
            URL myURL = new URL(appProperties.getFileUploadUri());
            HttpURLConnection httpURLConnection = (HttpURLConnection) myURL.openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("charset", request.getCharacterEncoding());
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if (!headerName.equalsIgnoreCase("content-type"))
                    httpURLConnection.setRequestProperty(headerName, request.getHeader(headerName));
            }
            httpURLConnection.setRequestProperty("Content-Disposition", "attachment; filename=" + fileItem.getName());
            httpURLConnection.setRequestProperty("Content-Type", fileItem.getContentType());
            httpURLConnection.setRequestProperty("Content-Length", Long.toString(fileItem.getSize()));
            try (FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
                final WritableByteChannel outputChannel = Channels.newChannel(httpURLConnection.getOutputStream());
                fileChannel.transferTo(0, file.length(), outputChannel);
                long size = file.length();
                long transferred = fileChannel.transferTo(0, file.length(), outputChannel);
                while (transferred != size) {
                    transferred += fileChannel.transferTo(transferred, size - transferred, outputChannel);
                }
                response.setStatus(httpURLConnection.getResponseCode());
                Map<String, List<String>> headerFields = httpURLConnection.getHeaderFields();
                for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
                    for (String fieldVal : entry.getValue()) {
                        //if (!fieldVal.equalsIgnoreCase("content-length"))
                        //response.addHeader(entry.getKey(), fieldVal);
                        System.out.println(entry.getKey() + ":" + fieldVal);
                    }
                }
                IOUtils.copy(httpURLConnection.getInputStream(), response.getOutputStream());


            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void handleDownload(HttpServletRequest request, HttpServletResponse response, String fileId) {
        try {
            URL myURL = new URL(appProperties.getFileDownloadUri() + "/" + fileId);
            HttpURLConnection httpURLConnection = (HttpURLConnection) myURL.openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("charset", request.getCharacterEncoding());
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setDoInput(true);
            response.setStatus(httpURLConnection.getResponseCode());
            Map<String, List<String>> headerFields = httpURLConnection.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
                for (String fieldVal : entry.getValue()) {
                    response.addHeader(entry.getKey(), fieldVal);
                }
            }
            OutputStream outputStream = response.getOutputStream();
            IOUtils.copyLarge(httpURLConnection.getInputStream(), outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }


    public void handleMultipartFormRequestX(final HttpServletRequest request, final HttpServletResponse response, MultipartFile multipartFile) {
        CommonsMultipartFile commonsMultipartFile = (CommonsMultipartFile) multipartFile;
        FileItem fileItem = commonsMultipartFile.getFileItem();
        DiskFileItem diskFileItem = (DiskFileItem) fileItem;
        File file = new File(diskFileItem.getStoreLocation().getAbsolutePath());
        try {
            String boundary = UUID.randomUUID().toString();
            URL myURL = new URL(appProperties.getFileUploadUri());
            HttpURLConnection httpURLConnection = (HttpURLConnection) myURL.openConnection();
            httpURLConnection.setConnectTimeout(1000);
            httpURLConnection.setReadTimeout(30000);
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("charset", "UTF-8");
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            String crlf = "\r\n";
            String twoHyphens = "--";
            OutputStream outputStream = httpURLConnection.getOutputStream();
            outputStream.write((twoHyphens + boundary + crlf).getBytes("UTF-8"));
            outputStream.write(("Content-Type:" + fileItem.getContentType() + crlf).getBytes("UTF-8"));
            outputStream.write(("Content-Disposition: form-data; name=\"" + fileItem.getFieldName() + "\";filename=\"" +
                    fileItem.getName() + "\"" + crlf).getBytes("UTF-8"));
            outputStream.write(crlf.getBytes("UTF-8"));

            try (FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
                final WritableByteChannel outputChannel = Channels.newChannel(outputStream);
                fileChannel.transferTo(0, file.length(), outputChannel);
                long size = file.length();
                long transferred = fileChannel.transferTo(0, file.length(), outputChannel);
                while (transferred != size) {
                    transferred += fileChannel.transferTo(transferred, size - transferred, outputChannel);
                }
            }
            outputStream.write(crlf.getBytes("UTF-8"));
            outputStream.write((twoHyphens + boundary +
                    twoHyphens + crlf).getBytes("UTF-8"));
            outputStream.flush();
            outputStream.close();

            response.setStatus(httpURLConnection.getResponseCode());
            Map<String, List<String>> headerFields = httpURLConnection.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
                for (String fieldVal : entry.getValue()) {
                    //if (!fieldVal.equalsIgnoreCase("content-length"))
                    //response.addHeader(entry.getKey(), fieldVal);
                    System.out.println(entry.getKey() + ":" + fieldVal);
                }
            }
            IOUtils.copy(httpURLConnection.getInputStream(), response.getOutputStream());


        } catch (IOException e) {
            logger.error("upload file failed ", e);

        }
    }

}
