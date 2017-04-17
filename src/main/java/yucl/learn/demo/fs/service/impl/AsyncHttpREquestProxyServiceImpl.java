package yucl.learn.demo.fs.service.impl;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import yucl.learn.demo.fs.cfg.AppProperties;
import yucl.learn.demo.fs.service.AsyncHttpRequestProxyService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by YuChunlei on 2017/4/17.
 */
@Service
public class AsyncHttpREquestProxyServiceImpl implements AsyncHttpRequestProxyService {
    private static final Logger logger = LoggerFactory.getLogger(AsyncHttpREquestProxyServiceImpl.class);

    @Autowired
    private AppProperties appProperties;

    private ConnectingIOReactor connectingIOReactor;

    private PoolingNHttpClientConnectionManager poolingNHttpClientConnectionManager;

    private CloseableHttpAsyncClient httpAsyncClient;

    public AsyncHttpREquestProxyServiceImpl() {
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
    public DeferredResult<ResponseEntity> handleChunkedRequest(HttpServletRequest request){
        final DeferredResult<ResponseEntity> deferredResult = new DeferredResult<>();
        try {
            Part part = request.getPart("file");
            ExtInputStreamBody _file = new ExtInputStreamBody(part.getInputStream(),
                    ContentType.create(part.getContentType()), part.getName(), part.getSize());
            HttpEntity reqEntity = MultipartEntityBuilder.create().addPart("file", _file).build();
            HttpPost httpPost = new HttpPost(appProperties.getFileUploadUri());
            httpPost.setEntity(reqEntity);
            doHttpRequest(httpPost, deferredResult);
        } catch (ServletException  | IOException e) {
            logger.error(e.getMessage(),e);
            deferredResult.setResult(new ResponseEntity<>(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR));
        }
        return deferredResult;
    }

    @Override
    public DeferredResult<ResponseEntity> handleMultipartFormRequest(HttpServletRequest request, MultipartFile multipartFile) {
        final DeferredResult<ResponseEntity> deferredResult = new DeferredResult<>();
        FileItem fileItem = ((CommonsMultipartFile) multipartFile).getFileItem();
        DiskFileItem diskFileItem = (DiskFileItem) fileItem;
        FileBody _file = new FileBody(new File(diskFileItem.getStoreLocation().getAbsolutePath()), ContentType.parse(fileItem.getContentType()), fileItem.getName());
        HttpEntity reqEntity = MultipartEntityBuilder.create().addPart("file", _file).build();
        HttpPost httpPost = new HttpPost(appProperties.getFileUploadUri());
        httpPost.setEntity(reqEntity);
        doHttpRequest(httpPost, deferredResult);
        return deferredResult;

    }

    private void doHttpRequest(HttpPost httpPost, DeferredResult<ResponseEntity> deferredResult) {
        httpAsyncClient.execute(httpPost, new FutureCallback<HttpResponse>() {
            @Override
            public void completed(HttpResponse result) {
                try {
                    String responseBody = EntityUtils.toString(result.getEntity());
                    deferredResult.setResult(new ResponseEntity<>(responseBody, HttpStatus.valueOf(result.getStatusLine().getStatusCode())));
                } catch (IOException e) {
                    logger.error(e.getMessage(),e);
                    deferredResult.setResult(new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
                }
            }

            @Override
            public void failed(Exception ex) {
                logger.error(ex.getMessage(),ex);
                deferredResult.setResult(new ResponseEntity<>(ex.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR));
            }

            @Override
            public void cancelled() {
                logger.error(httpPost.toString() + "cancelled");
            }
        });
    }



    @Override
    public StreamingResponseBody handleDownload(String fileId, HttpServletRequest request, HttpServletResponse response) {
        return outputStream -> {
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
                    IOUtils.copy(inputStream, outputStream);
                    inputStream.close();
                }

            } catch (IOException | InterruptedException | ExecutionException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                logger.error("downloadFile failed", e);

            }

        };

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
