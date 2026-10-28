package com.deroyque.activation.infrastructure.web;

import com.deroyque.activation.application.port.in.CreateActivationUseCase;
import com.deroyque.activation.application.port.in.GetActivationUseCase;
import com.deroyque.activation.application.port.in.ListActivationsUseCase;
import com.deroyque.activation.domain.model.ActivationRequest;
import com.deroyque.activation.domain.model.ActivationStatus;
import com.deroyque.activation.infrastructure.web.dto.ActivationResponseDto;
import com.deroyque.activation.infrastructure.web.dto.CreateActivationRequestDto;
import com.deroyque.activation.infrastructure.web.dto.PaginatedResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/requests")
@Tag(name = "Activation Requests", description = "Operations for initiating, querying, and listing product activations")
@SecurityRequirement(name = "bearerAuth")
public class ActivationController {

    private final CreateActivationUseCase createActivationUseCase;
    private final GetActivationUseCase getActivationUseCase;
    private final ListActivationsUseCase listActivationsUseCase;

    public ActivationController(CreateActivationUseCase createActivationUseCase,
                                GetActivationUseCase getActivationUseCase,
                                ListActivationsUseCase listActivationsUseCase) {
        this.createActivationUseCase = createActivationUseCase;
        this.getActivationUseCase = getActivationUseCase;
        this.listActivationsUseCase = listActivationsUseCase;
    }

    @PostMapping
    @Operation(
            summary = "Create or replay an activation request",
            description = "Asynchronously initiates product activation. Enforces idempotency via externalId: " +
                    "new requests return 201 Created with Location header; identical replays return 200 OK with the existing record; " +
                    "divergent replays return 409 Conflict."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Activation request accepted for asynchronous processing",
                    headers = {
                            @Header(name = "Location", description = "URI of the created activation request", schema = @Schema(type = "string")),
                            @Header(name = "X-Correlation-Id", description = "Tracing correlation identifier", schema = @Schema(type = "string"))
                    },
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ActivationResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "200",
                    description = "Idempotent replay of an existing activation request with matching payload",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ActivationResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request payload or validation errors",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(responseCode = "401", description = "Missing or invalid OAuth 2.0 Bearer JWT token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient scope (requires 'activations:write')", content = @Content),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict: externalId already exists with divergent productCode or customerId",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<ActivationResponseDto> createActivationRequest(
            @Valid @RequestBody CreateActivationRequestDto requestDto) {

        CreateActivationUseCase.CreateResult result = createActivationUseCase.createActivation(
                requestDto.externalId(),
                requestDto.customerId(),
                requestDto.productCode()
        );

        ActivationResponseDto responseBody = ActivationResponseDto.fromDomain(result.request());

        if (result.isNew()) {
            URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(result.request().getId())
                    .toUri();
            return ResponseEntity.created(location).body(responseBody);
        } else {
            return ResponseEntity.ok(responseBody);
        }
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Retrieve an activation request by ID",
            description = "Returns current lifecycle status (PENDING, PROCESSING, COMPLETED, FAILED) and failure reasons if any."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Activation request details found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ActivationResponseDto.class))
            ),
            @ApiResponse(responseCode = "401", description = "Missing or invalid OAuth 2.0 Bearer JWT token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient scope (requires 'activations:read')", content = @Content),
            @ApiResponse(
                    responseCode = "404",
                    description = "Activation request not found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<ActivationResponseDto> getActivationRequestById(
            @Parameter(description = "Unique activation request UUID", required = true)
            @PathVariable UUID id) {
        ActivationRequest request = getActivationUseCase.getById(id);
        return ResponseEntity.ok(ActivationResponseDto.fromDomain(request));
    }

    @GetMapping
    @Operation(
            summary = "List activation requests with filtering and pagination",
            description = "Returns a paginated list of activation requests with optional filtering by customerId and status."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Paginated list of activation requests",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PaginatedResponseDto.class))
            ),
            @ApiResponse(responseCode = "401", description = "Missing or invalid OAuth 2.0 Bearer JWT token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient scope (requires 'activations:read')", content = @Content)
    })
    public ResponseEntity<PaginatedResponseDto<ActivationResponseDto>> listActivationRequests(
            @Parameter(description = "Filter by customer identifier", required = false)
            @RequestParam(required = false) String customerId,
            @Parameter(description = "Filter by activation lifecycle status", required = false)
            @RequestParam(required = false) ActivationStatus status,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<ActivationRequest> page = listActivationsUseCase.list(customerId, status, pageable);

        PaginatedResponseDto<ActivationResponseDto> response = new PaginatedResponseDto<>(
                page.getContent().stream().map(ActivationResponseDto::fromDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );

        return ResponseEntity.ok(response);
    }
}
