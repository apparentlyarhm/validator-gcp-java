package com.arhum.validator.provider.impl;

import com.arhum.validator.exception.BaseException;
import com.arhum.validator.exception.InternalServerException;
import com.arhum.validator.exception.NotFoundException;
import com.arhum.validator.provider.contract.GcsProvider;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.*;
import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.util.concurrent.TimeUnit;

@Service
public class GcsProviderImpl implements GcsProvider {
    private static final Logger logger = LoggerFactory.getLogger(GcsProviderImpl.class);

    @Value("${google.storage.bucket}")
    private String bucketName;

    @Autowired
    private Storage storage;

    @Override
    public Blob uploadFile(String blobName, MultipartFile file, String contentType) throws BaseException {
        BlobId blobId = BlobId.of(bucketName, blobName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).build();

        try (WriteChannel writer = storage.writer(blobInfo)) {
            try (InputStream inputStream = file.getInputStream()) {
                ByteStreams.copy(inputStream, Channels.newOutputStream(writer));
            }
            // After successful write, get the final blob object with all metadata
            Blob blob = storage.get(blobId);

            if (blob == null) {
                // This is a rare edge case but good to handle
                logger.info("Upload seemed successful, but the final object could not be retrieved.");
                throw new InternalServerException("Something went wrong", 500);
            }
            return blob;
        } catch (IOException e) {
            logger.error("Failed to upload file '{}' to GCS bucket '{}'.", blobName, bucketName, e);
            throw new InternalServerException("Something went wrong", 500);
        }
    }

    @Override
    public Blob getBlob(String blobName) throws BaseException {
        try {
            Blob blob = storage.get(bucketName, blobName);
            if (blob == null || !blob.exists()) {
                throw new NotFoundException("File not found: " + blobName, 40000);
            }
            return blob;
        } catch (StorageException e) {
            logger.error("Error accessing blob '{}' in GCS bucket '{}'.", blobName, bucketName, e);
            throw new InternalServerException("Could not access storage.", 500);
        }
    }


    @Override
    public URL generateV4SignedUrl(String blobName) throws BaseException {
        // First, ensure the blob exists before trying to sign it.
        getBlob(blobName);

        long expiryInMinutes = 5;
        try {
            BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, blobName)).build();
            return storage.signUrl(
                    blobInfo,
                    expiryInMinutes,
                    TimeUnit.MINUTES,
                    Storage.SignUrlOption.withV4Signature()
            );
        } catch (StorageException e) {
            logger.error("Error generating signed URL for blob '{}' in GCS bucket '{}'.", blobName, bucketName, e);
            throw new InternalServerException("Could not generate download link.", 500);
        }
    }
}

