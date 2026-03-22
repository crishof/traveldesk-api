package com.crishof.traveldeskapi.controller;

import com.crishof.traveldeskapi.dto.ThemeRequest;
import com.crishof.traveldeskapi.dto.ThemeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/theme")
@RequiredArgsConstructor
@Slf4j
public class ThemeController {

    //  ===============
    //  GET THEME
    //  ===============

    @Operation(summary = "Get theme settings")
    @ApiResponse(responseCode = "200", description = "Theme settings retrieved successfully")
    @GetMapping
    public ResponseEntity<ThemeResponse> getTheme() {
        log.info("Get theme request received");
        return ResponseEntity.ok().build();
    }

    //  ===============
    //  UPDATE THEME
    //  ===============

    @Operation(summary = "Update theme settings")
    @ApiResponse(responseCode = "200", description = "Theme settings updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PutMapping
    public ResponseEntity<ThemeResponse> updateTheme(@Valid @RequestBody ThemeRequest request) {
        log.info("Update theme request received for mode={}", request.mode());
        return ResponseEntity.ok().build();
    }
}
