package com.arhum.validator.provider.contract;

import com.arhum.validator.exception.BaseException;
import com.google.cloud.storage.Blob;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;

public interface GcsProvider {
    /**
     * Uploads a file to the configured storage bucket.
     *
     * @param blobName    The full path and name for the object in the bucket.
     * @param file        The file to upload.
     * @param contentType The MIME type of the file.
     * @return The uploaded Blob object, containing metadata.
     */
    Blob uploadFile(String blobName, MultipartFile file, String contentType) throws BaseException;

    /**
     * Retrieves a blob object from the storage bucket.
     *
     * @param blobName The full path and name of the object.
     * @return The Blob object.
     */
    Blob getBlob(String blobName) throws BaseException;

    /**
     * Generates a temporary, signed URL to allow downloading a private object.
     *
     * @param blobName The full path and name of the object.
     * @return A signed URL valid for a short duration.
     */
    URL generateV4SignedUrl(String blobName) throws BaseException;
}
