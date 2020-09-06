package com.microservice.mytijori.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;

@Service
public class AWSS3Service {

	private static final Logger LOGGER = LoggerFactory.getLogger(AWSS3Service.class);

	@Autowired
	private AmazonS3 amazonS3;
	@Value("${aws.s3.bucket}")
	private String bucketName;

	// @Async annotation ensures that the method is executed in a different background thread
	// but not consume the main thread.
	@Async
	public void uploadFile(final String mobileNumber, final String category, final String subCategory, final MultipartFile multipartFile) {
		LOGGER.info("File upload in progress.");
		try {
			final File file = convertMultiPartFileToFile(multipartFile);
			uploadFileToS3Bucket(bucketName, mobileNumber, category, subCategory, file);
			LOGGER.info("File upload is completed.");
			file.delete();	// To remove the file locally created in the project folder.
		} catch (final AmazonServiceException ex) {
			LOGGER.info("File upload is failed.");
			LOGGER.error("Error= {} while uploading file.", ex.getMessage());
		}
	}

	// @Async annotation ensures that the method is executed in a different background thread
	// but not consume the main thread.
	@Async
	public byte[] downloadFile(final String mobileNumber, final String category, final String subCategory) {
		byte[] content = null;
		final S3Object s3Object = amazonS3.getObject(bucketName, mobileNumber + "/" + category + "/" + subCategory + "/" + category+ "-" + subCategory);
		final S3ObjectInputStream stream = s3Object.getObjectContent();
		try {
			content = IOUtils.toByteArray(stream);
			LOGGER.info("File downloaded successfully.");
			s3Object.close();
		} catch(final IOException ex) {
			LOGGER.info("IO Error Message= " + ex.getMessage());
		}
		return content;
	}

	// @Async annotation ensures that the method is executed in a different background thread
	// but not consume the main thread.
	@Async
	public void deleteFile(final String mobileNumber, final String category, final String subCategory) {
		final DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(bucketName, mobileNumber + "/" + category + "/" + subCategory + "/" + category+ "-" + subCategory);
		amazonS3.deleteObject(deleteObjectRequest);
		LOGGER.info("File deleted successfully.");
	}

	@Async
	public List<String> getFiles(final String mobileNumber) {
		ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName).withPrefix(mobileNumber);
		ListObjectsV2Result listing = amazonS3.listObjectsV2(req);
		List<String> resourcePaths = new ArrayList<>();
		for (S3ObjectSummary summary: listing.getObjectSummaries()) {
			if(!summary.getKey().endsWith("/"))
			resourcePaths.add(summary.getKey());
		}
		return resourcePaths;
	}

	private File convertMultiPartFileToFile(final MultipartFile multipartFile) {
		final File file = new File(multipartFile.getOriginalFilename());
		try (final FileOutputStream outputStream = new FileOutputStream(file)) {
			outputStream.write(multipartFile.getBytes());
		} catch (final IOException ex) {
			LOGGER.error("Error converting the multi-part file to file= ", ex.getMessage());
		}
		return file;
	}

	private void uploadFileToS3Bucket(final String bucketName, final String mobileNumber, final String category, final String subCategory, final File file) {
		final PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, mobileNumber + "/" + category + "/" + subCategory + "/" + category+ "-" + subCategory, file);
		amazonS3.putObject(putObjectRequest);
	}
}
