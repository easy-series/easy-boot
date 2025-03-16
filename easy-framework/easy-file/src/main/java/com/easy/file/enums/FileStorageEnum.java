package com.easy.file.enums;

import cn.hutool.core.util.ArrayUtil;

import com.easy.file.client.FileClient;
import com.easy.file.client.FileClientConfig;
import com.easy.file.client.s3.S3FileClient;
import com.easy.file.client.s3.S3FileClientConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文件存储器枚举
 *
 * @author 芋道源码
 */
@AllArgsConstructor
@Getter
public enum FileStorageEnum {

//    DB(1, DBFileClientConfig.class, DBFileClient.class),
//
//    LOCAL(10, LocalFileClientConfig.class, LocalFileClient.class),
//    FTP(11, FtpFileClientConfig.class, FtpFileClient.class),
//    SFTP(12, SftpFileClientConfig.class, SftpFileClient.class),

    S3(20, S3FileClientConfig.class, S3FileClient.class),
    ;

    /**
     * 存储器
     */
    private final Integer storage;

    /**
     * 配置类
     */
    private final Class<? extends FileClientConfig> configClass;
    /**
     * 客户端类
     */
    private final Class<? extends FileClient> clientClass;

    public static FileStorageEnum getByStorage(Integer storage) {
        return ArrayUtil.firstMatch(o -> o.getStorage().equals(storage), values());
    }

}
