package yucl.learn.demo.fs.service.impl;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.params.HttpParams;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by YuChunlei on 2017/3/15.
 */


/**
 * tomcat web.xml
 * <multipart-config>
 * <!-- 50MB max -->
 * <max-file-size>52428800</max-file-size>
 * <max-request-size>52428800</max-request-size>
 * <file-size-threshold>0</file-size-threshold>
 * </multipart-config>
 */
//@Service
public class HttpRequestProxyServiceXImpl implements HttpRequestProxyService {
    private static final Logger logger = LoggerFactory.getLogger(HttpRequestProxyServiceXImpl.class);

    @Autowired
    private AppProperties appProperties;

    private ConnectingIOReactor connectingIOReactor;

    private PoolingNHttpClientConnectionManager poolingNHttpClientConnectionManager;

    private CloseableHttpAsyncClient httpAsyncClient;

    public HttpRequestProxyServiceXImpl() {
        try {
            connectingIOReactor = new DefaultConnectingIOReactor();
            poolingNHttpClientConnectionManager = new PoolingNHttpClientConnectionManager(connectingIOReactor);
            poolingNHttpClientConnectionManager.setMaxTotal(500);
            httpAsyncClient = HttpAsyncClients.custom()
                    .setMaxConnTotal(100)
                    .setMaxConnPerRoute(100)
                    .setConnectionManager(poolingNHttpClientConnectionManager)
                    .build();
            httpAsyncClient.start();
        } catch (IOReactorException e) {
            logger.error("init httpAsynClient failed", e);
        }
    }

    @Override
    public void handleChunkedRequest(final HttpServletRequest request, final HttpServletResponse response) {
        // CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            Part part = request.getPart("file");
            HttpPost httpPost = new HttpPost(appProperties.getFileUploadUri());
            ExtInputStreamBody _file = new ExtInputStreamBody(part.getInputStream(),
                    ContentType.create(part.getContentType()), part.getName(), part.getSize());
            HttpEntity reqEntity = MultipartEntityBuilder.create().addPart("file", _file).build();
            httpPost.setEntity(reqEntity);
            Future<HttpResponse> future = httpAsyncClient.execute(httpPost,null);
            HttpResponse serverResponse = future.get();
            response.setStatus(serverResponse.getStatusLine().getStatusCode());
            IOUtils.copy(serverResponse.getEntity().getContent(), response.getOutputStream());
            response.getOutputStream().flush();
            response.getOutputStream().close();
        } catch (IOException | ServletException|InterruptedException |ExecutionException e) {
            logger.error("upload file failed ", e);
        }
    }

    public void handleMultipartFormRequest(final HttpServletRequest request, final HttpServletResponse response, MultipartFile multipartFile) {

        try {
            try( CloseableHttpClient httpclient = HttpClients.createDefault()) {
                HttpPost httpPost = new HttpPost(appProperties.getFileUploadUri());
             /*ExtInputStreamBody _file = new ExtInputStreamBody(multipartFile.getInputStream(),
                    ContentType.create(multipartFile.getContentType()), multipartFile.getOriginalFilename(), multipartFile.getSize());*/

                FileItem fileItem = ((CommonsMultipartFile) multipartFile).getFileItem();
                DiskFileItem diskFileItem = (DiskFileItem) fileItem;
                FileBody _file = new FileBody(new File(diskFileItem.getStoreLocation().getAbsolutePath()), ContentType.parse(fileItem.getContentType()), fileItem.getName());

                HttpEntity reqEntity = MultipartEntityBuilder.create().addPart("file", _file).build();
                httpPost.setEntity(reqEntity);
                HttpResponse serverResponse = httpclient.execute(httpPost);
                response.setStatus(serverResponse.getStatusLine().getStatusCode());
                for (Header header : serverResponse.getAllHeaders()) {
                    System.out.println(header.getName() + ":" + header.getValue());
                    response.setHeader(header.getName(),header.getValue());
                }
                IOUtils.copy(serverResponse.getEntity().getContent(), response.getOutputStream());
                response.getOutputStream().flush();
                response.getOutputStream().close();
            }
        } catch (IOException e) {
            logger.error("upload file failed ", e);

        }
    }

    public void handleDownload(final HttpServletRequest request, final HttpServletResponse response, final String fileId) {
        try {
            HttpGet httpGet = new HttpGet(appProperties.getFileDownloadUri() + "/" + fileId);
            Future<HttpResponse> future = httpAsyncClient.execute(httpGet, null);
            HttpResponse httpResponse = future.get();
            response.setStatus(httpResponse.getStatusLine().getStatusCode());
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                for (Header header : httpResponse.getAllHeaders()) {
                    response.addHeader(header.getName(), header.getValue());
                }
                final InputStream inputStream = httpResponse.getEntity().getContent();
                IOUtils.copy(inputStream, response.getOutputStream());
                inputStream.close();
            }

        } catch (IOException | InterruptedException | ExecutionException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            logger.error("downloadFile failed", e);

        }
    }

}

class ExtInputStreamBody extends InputStreamBody {
    private long len;

    public ExtInputStreamBody(InputStream in, ContentType contentType, String filename, long len) {
        super(in, contentType, filename);
        this.len = len;
    }

    public ExtInputStreamBody(InputStream in, String filename, long len) {
        super(in, filename);
        this.len = len;

    }

    /**
     * @param in
     * @param contentType
     */
    public ExtInputStreamBody(InputStream in, ContentType contentType) {
        super(in, contentType);

    }

    @Override
    public InputStream getInputStream() {
        return super.getInputStream();
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        super.writeTo(out);
    }

    @Override
    public String getTransferEncoding() {
        return super.getTransferEncoding();
    }

    @Override
    public long getContentLength() {
        return this.len;
    }

    @Override
    public String getFilename() {
        return super.getFilename();
    }

}

