package com.asiancuisine.asiancuisine.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/image")
@Api(tags = "recipe Api")
@Slf4j
public class RecipeImageController {
    @ApiOperation("get recipe images")
    @GetMapping("/{imageName}")
    public ResponseEntity<Resource> getImage(
            @ApiParam(value = "image name",required = true,example = "MapoTofu")@PathVariable String imageName) {
        try {

            imageName += ".jpg";
            // Define the base directory for images
            Path baseDir = Paths.get("/home/lz238/images").toAbsolutePath().normalize();

            // Resolve the requested image path and normalize it
            Path imagePath = baseDir.resolve(imageName).normalize();

            log.info("Looking for image at path: " + imagePath.toString());

            // Check if the resolved path starts with the base directory to prevent traversal
            if (!imagePath.startsWith(baseDir)) {
                log.warn("Attempted path traversal: " + imageName);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            // Load the image as a resource
            Resource resource = new UrlResource(imagePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                log.warn("Requested image not found or not readable: " + imageName);
                return ResponseEntity.notFound().build();
            }

            // Determine content type
            String contentType = Files.probeContentType(imagePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // Return the image as a response
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Error retrieving image: " + imageName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
