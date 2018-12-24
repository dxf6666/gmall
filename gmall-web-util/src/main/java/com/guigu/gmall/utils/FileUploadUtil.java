package com.guigu.gmall.utils;



 import com.guigu.gmall.constant.FastDFSURL;
 import org.csource.common.MyException;
 import org.csource.fastdfs.ClientGlobal;
 import org.csource.fastdfs.StorageClient;
 import org.csource.fastdfs.TrackerClient;
 import org.csource.fastdfs.TrackerServer;
 import org.springframework.web.multipart.MultipartFile;

 import java.io.IOException;

public class FileUploadUtil {
    public static String uploadImage(MultipartFile file) { //MultipartFile单词：多文件
        // 加载fdfs的配置文件
        String path = FileUploadUtil.class.getClassLoader().getResource("tracker.conf").getPath();
        try {
            ClientGlobal.init(path);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }

        // 创建一个tracker的连接
        TrackerClient trackerClient = new TrackerClient();
        TrackerServer connection = null;
        try {
            connection = trackerClient.getConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 从tracker中返回一个可用的storage
        StorageClient storageClient = new StorageClient(connection, null);

        // 上传文件
        String[] gifs = new String[0];//上传的数据的byte数组，需要上传的数据的路径
        try {
            // 获得文件的扩展名
            String originalFilename = file.getOriginalFilename(); //获取文件名
            String[] split = originalFilename.split("\\.");//【注意】这里点. 必须使用转义
            String extName = split[(split.length - 1)];  // 使用 . 进行分割，分割成好多个字段，获取最后一个，那就是扩展名
            gifs = storageClient.upload_file(file.getBytes(), extName, null);
                                            // byte[]数据      文件扩展名
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }

        // 返回上传结果，浏览器通过访问该上传的结果，访问到存储在fastDFS中的文件
        String imgUrl = FastDFSURL.LinuxURL;
        for (String gif : gifs) {
            imgUrl = imgUrl +"/"+gif;
        }
        return imgUrl;
    }
}
