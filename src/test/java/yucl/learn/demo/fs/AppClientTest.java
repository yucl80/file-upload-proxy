package yucl.learn.demo.fs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author chunlei.yu
 *
 */
@RestController
public class AppClientTest {
	private static final Logger logger = LoggerFactory.getLogger(AppClientTest.class);

	@RequestMapping(value = "/proxy/files", method = RequestMethod.POST)
	public String uploadSingleFile(Principal principal, @RequestParam(value = "file") MultipartFile file) {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		try {
			String module = "10";
			HttpPost httppost = new HttpPost("http://localhost:8090/files");
			ExtInputStreamBody _file = new ExtInputStreamBody(file.getInputStream(),
					ContentType.create(file.getContentType()), file.getOriginalFilename(), file.getSize());
			HttpEntity reqEntity = MultipartEntityBuilder.create()
					.addPart("module", new StringBody(module, ContentType.create("text/plain", Consts.UTF_8)))
					.addPart("contentType",
							new StringBody(file.getContentType(), ContentType.create("text/plain", Consts.UTF_8)))
					.addPart("fileName",
							new StringBody(file.getOriginalFilename(), ContentType.create("text/plain", Consts.UTF_8)))
					.addPart("file", _file).build();
			httppost.setEntity(reqEntity);
			CloseableHttpResponse response = httpclient.execute(httppost);
			if (response.getStatusLine().getStatusCode() == 200) {
				String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
				ObjectMapper objectMapper = new ObjectMapper();
				Map map = objectMapper.readValue(responseBody, Map.class);
				String fileId = (String) map.get("fileId");
				return responseBody;
			}
			response.close();
		} catch (IOException e) {
			logger.error("upload file failed ", e);

		} finally {
			try {
				httpclient.close();
			} catch (IOException e) {

			}
		}
		return null;

	}

	@RequestMapping(value = "/proxy/files/{fileId}", method = RequestMethod.GET)
	public void downloadFile(@PathVariable("fileId") String fileId, HttpServletResponse response) throws IOException {
		try {
			CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault();
			httpclient.start();
			HttpGet request = new HttpGet("http://localhost:8090/files/" + fileId);
			Future<HttpResponse> future = httpclient.execute(request, null);
			HttpResponse httpResponse = future.get();
			if (httpResponse.getStatusLine().getStatusCode() == 200) {
				for (Header header : httpResponse.getAllHeaders()) {
					response.addHeader(header.getName(), header.getValue());
				}
				final InputStream inputStream = httpResponse.getEntity().getContent();
				IOUtils.copy(inputStream, response.getOutputStream());
				inputStream.close();
			} else {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			}
		} catch (IOException | InterruptedException | ExecutionException e) {
			logger.error("downloadFile failed", e);
		}

	}

    /**
     * @EnableAutoConfiguration(exclude = {MultipartAutoConfiguration.class})
     * @param request
     * @throws IOException
     * @throws FileUploadException
     */
	@RequestMapping("/upload")
	public void upload(HttpServletRequest request) throws IOException, FileUploadException {
		boolean isMultipart = ServletFileUpload.isMultipartContent(request);
		if (!isMultipart) {
			// Inform user about invalid request
		}

		// Create a new file upload handler
		ServletFileUpload upload = new ServletFileUpload();

		// Parse the request
		FileItemIterator iter = upload.getItemIterator(request);
		while (iter.hasNext()) {
			FileItemStream item = iter.next();

			String name = item.getFieldName();
			InputStream stream = item.openStream();
			if (item.isFormField()) {
				System.out.println("Form field " + name + " with value "+ Streams.asString(stream) + " detected.");
			} else {
				System.out.println("File field " + name + " with file name " + item.getName() + " detected.");
				// Process the input stream

			}
		}
	}

	@RequestMapping(value = "/proxy/test", method = RequestMethod.POST)
	public String test(Principal principal, @RequestParam(value = "contentType") String contentType,
			@RequestParam(value = "fileName", required = false) String fileName,
			@RequestParam(value = "file") MultipartFile file) {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		try {
			HttpPost httppost = new HttpPost("http://localhost:8080/files");
			ExtInputStreamBody _file = new ExtInputStreamBody(file.getInputStream(),
					ContentType.create(file.getContentType()), file.getOriginalFilename(), file.getSize());
			HttpEntity reqEntity = MultipartEntityBuilder.create().addPart("file", _file).build();
			httppost.setEntity(reqEntity);
			CloseableHttpResponse response = httpclient.execute(httppost);
			if (response.getStatusLine().getStatusCode() == 200) {
				String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
				ObjectMapper objectMapper = new ObjectMapper();
				Map map = objectMapper.readValue(responseBody, Map.class);
				String fileId = (String) map.get("fileId");
				return responseBody;
			}
			response.close();
		} catch (IOException e) {
			logger.error("upload file failed ", e);

		} finally {
			try {
				httpclient.close();
			} catch (IOException e) {

			}
		}
		return null;
	}

}
