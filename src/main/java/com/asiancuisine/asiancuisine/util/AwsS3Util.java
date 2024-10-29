package com.asiancuisine.asiancuisine.util;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

@Component
@Slf4j
public class AwsS3Util {

    @Autowired
    AmazonS3 amazonS3Client;


    public String uploadFile(MultipartFile file, String awsBucketName) throws IOException {

        String originalFileName = file.getOriginalFilename();
        String extensionName = originalFileName.substring(originalFileName.lastIndexOf('.'));
        String keyName = UUID.randomUUID().toString()+extensionName;
        //ContentType
        ObjectMetadata metadata = new ObjectMetadata();
        if(extensionName.contains("PNG") || extensionName.contains("png")){
            metadata.setContentType("image/png");
        }else{
            metadata.setContentType("image/jpeg");
        }

        File tempFile = File.createTempFile("temp", originalFileName);
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(file.getBytes());
        }
        PutObjectRequest putObjectRequest = new PutObjectRequest(awsBucketName, keyName,tempFile).withMetadata(metadata);;
        putObjectRequest.setCannedAcl(CannedAccessControlList.PublicRead);
        amazonS3Client.putObject(putObjectRequest);
        log.info(amazonS3Client.getUrl(awsBucketName,keyName).toString());
        return amazonS3Client.getUrl(awsBucketName,keyName).toString();
    }

    public File downloadFile(String keyName, String awsBucketName) {
        File file = new File(keyName);
        amazonS3Client.getObject(new GetObjectRequest(awsBucketName, keyName), file);
        return file;
    }

}
