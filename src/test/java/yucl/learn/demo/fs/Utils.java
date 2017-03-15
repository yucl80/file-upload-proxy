package yucl.learn.demo.fs;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

/**
 * @author chunlei.yu
 *
 */
public class Utils {

	public static void printRequestHeader(HttpServletRequest request) {
		Enumeration<String> names = request.getHeaderNames();
		while (names.hasMoreElements()) {
			String n = names.nextElement();
			System.out.println(n + ":" + request.getHeader(n));

		}
	}

	/**
	 * @param args
	 */
	// public static void main(String[] args) {
	// Path path = Paths.get("E:/tmp", "a.txt");
	// try {
	// FileStore store = Files.getFileStore(path);
	// if (!store.supportsFileAttributeView(UserDefinedFileAttributeView.class))
	// {
	// System.out.println("The user defined attributes are not supported on: " +
	// store);
	// } else {
	// System.out.println("The user defined attributes are supported on: " +
	// store);
	// }
	// UserDefinedFileAttributeView udfav = Files.getFileAttributeView(path,
	// UserDefinedFileAttributeView.class);
	// int written = udfav.write("file.description",
	// Charset.defaultCharset().encode("test"));
	//
	// int size = udfav.size("file.x");
	// ByteBuffer bb = ByteBuffer.allocateDirect(size);
	// udfav.read("file.description", bb);
	// bb.flip();
	// System.out.println(Charset.defaultCharset().decode(bb).toString());
	// } catch (IOException e) {
	// System.err.println(e);
	// }
	//
	// }

}
