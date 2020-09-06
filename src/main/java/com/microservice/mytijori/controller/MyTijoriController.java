package com.microservice.mytijori.controller;

import com.microservice.mytijori.service.AWSS3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value = "/mytijori")
@Tag(name = "mytijori", description = "MyTijori API")
public class MyTijoriController {

    @Autowired
    private AWSS3Service service;

    @Operation(summary = "Creates the file for the specified user and category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid file supplied", content = @Content),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)})
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadFile(@Parameter(description = "Unique mobile number") @RequestParam(value = "mobileNumber") final String mobileNumber,
                                             @Parameter(description = "File Category") @RequestParam(value = "category") final String category,
                                             @Parameter(description = "File sub-category") @RequestParam(value = "subCategory", required = false) final String subCategory,
                                             @Parameter(description = "Upload the file to be created") @RequestPart(value = "file") final MultipartFile multipartFile) {
        service.uploadFile(mobileNumber, category, subCategory, multipartFile);
        final String response = multipartFile.getOriginalFilename() + " uploaded successfully.";
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Download the file for the specified user and category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", content = {@
                    Content(mediaType = "application/octet-stream")}),
            @ApiResponse(responseCode = "400", description = "Invalid fileName supplied", content = @Content),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)})
    @GetMapping(value = "/download")
    public ResponseEntity<ByteArrayResource> downloadFile(@Parameter(description = "Unique mobile number") @RequestParam(value = "mobileNumber") final String mobileNumber,
                                                          @Parameter(description = "File Category") @RequestParam(value = "category") final String category,
                                                          @Parameter(description = "File sub-category") @RequestParam(value = "subCategory", required = false) final String subCategory) {
        final byte[] data = service.downloadFile(mobileNumber, category, subCategory);
        final ByteArrayResource resource = new ByteArrayResource(data);
        return ResponseEntity
                .ok()
                .contentLength(data.length)
                .header("Content-type", "application/octet-stream")
                .header("Content-disposition", "attachment; filename=\"" + category + "-" + subCategory + "\"")
                .body(resource);
    }

    @Operation(summary = "Deletes the file for the specified user and category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deleted", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid request supplied", content = @Content),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content)})
    @DeleteMapping(value = "/upload")
    public ResponseEntity<String> deleteFile(@Parameter(description = "Unique mobile number") @RequestParam(value = "mobileNumber") final String mobileNumber,
                                             @Parameter(description = "File Category") @RequestParam(value = "category") final String category,
                                             @Parameter(description = "File sub-category") @RequestParam(value = "subCategory", required = false) final String subCategory) {
        service.deleteFile(mobileNumber, category, subCategory);
        return new ResponseEntity<>("Deleted successfully", HttpStatus.CREATED);
    }
}
