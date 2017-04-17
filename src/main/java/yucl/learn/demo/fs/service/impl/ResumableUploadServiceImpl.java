package yucl.learn.demo.fs.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import yucl.learn.demo.fs.service.ResumableUploadService;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * Created by YuChunlei on 2017/4/17.
 */
@Service
public class ResumableUploadServiceImpl implements ResumableUploadService {
    private static final Logger logger = LoggerFactory.getLogger(ResumableUploadServiceImpl.class);
    private FileSystem fileSystem = FileSystems.getDefault();
    private String baseDir = ".";

    public synchronized void createNewFile(String fileUID, long fileSize) {
        try {
            Path tempFilePath = getTempFilePath(fileUID);
            File file = tempFilePath.toFile();
            if (!file.exists()) {
                RandomAccessFile targetFile = new RandomAccessFile(file, "rw");
                targetFile.setLength(fileSize);
                targetFile.close();
            }
            File trackFile = getTrackFilePath(fileUID).toFile();
            if (!trackFile.exists()) {
                trackFile.createNewFile();
            }
        } catch (IOException e) {
            logger.error("create file " + fileUID + " failed", e);
        }

    }

    public Map<String, String> resumableUploadHandle(String fileId, InputStream inputStream, long position, long partSize, long fileSize) throws IOException {
        createNewFile(fileId, fileSize);
        Path tempFilePath = getTempFilePath(fileId);
        File trackFile = getTrackFilePath(fileId).toFile();
        Map<String, String> resultMap = new HashMap<>();
        try (final ReadableByteChannel inputChannel = Channels.newChannel(inputStream);
             final FileChannel outputChannel = FileChannel.open(tempFilePath, StandardOpenOption.WRITE)) {
            long size = outputChannel.transferFrom(inputChannel, position, partSize);
            try (FileOutputStream fileOutputStream = new FileOutputStream(trackFile, true)) {
                fileOutputStream.write(new StringBuilder().append(String.valueOf(position)).append(" ").append(String.valueOf(position + size - 1)).append("\n").toString().getBytes(StandardCharsets.UTF_8));
                fileOutputStream.flush();
            }
            resultMap.put("ContentRange", position + "-" + (position + size));
            resultMap.put("state", "false");
        }
        if (isUploadComplete(trackFile, fileSize)) {
            tempFilePath.toFile().renameTo(getFilePath(fileId).toFile());
            trackFile.delete();
            resultMap.put("state", "true");
            /* todo call business function when file upload completed */
        }
        return resultMap;
    }

    private boolean isUploadComplete(File trackFile, long fileSize) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(trackFile))) {
            String line = null;
            List<TrackRecord> trackList = new ArrayList<>();
            while ((line = bufferedReader.readLine()) != null) {
                String[] data = line.split(" ");
                if (data.length == 2) {
                    trackList.add(new TrackRecord(Long.parseLong(data[0]), Long.parseLong(data[1])));
                }
            }
            trackList.sort(new Comparator<TrackRecord>() {
                @Override
                public int compare(TrackRecord o1, TrackRecord o2) {
                    if (o1.begin > o2.begin)
                        return 1;
                    if (o1.begin < o2.begin)
                        return -1;
                    return 0;
                }
            });
            if (trackList.get(0).begin != 0) {
                return false;
            }
            if (trackList.get(trackList.size() - 1).end + 1 != fileSize) {
                return false;
            }
            for (int i = 0; i < trackList.size() - 1; i++) {
                if (trackList.get(i).end + 1 != trackList.get(i + 1).begin) {
                    return false;
                }
            }
            return true;
        }
    }

    public Path getTempFilePath(String fileUID) {
        return fileSystem.getPath(baseDir + "/" + fileUID + ".tmp");
    }

    public Path getTrackFilePath(String fileUID) {
        return fileSystem.getPath(baseDir + "/" + fileUID + ".track");
    }

    public Path getFilePath(String fileUID) {
        return fileSystem.getPath(baseDir + "/" + fileUID);
    }


}

class TrackRecord {
    public long begin;

    public long end;

    public TrackRecord(long begin, long end) {
        this.begin = begin;
        this.end = end;
    }

}

