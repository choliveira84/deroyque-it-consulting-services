package com.deroyque.activation.infrastructure.web;

import com.deroyque.activation.application.port.in.CreateActivationUseCase;
import com.deroyque.activation.application.port.in.GetActivationUseCase;
import com.deroyque.activation.application.port.in.ListActivationsUseCase;
import com.deroyque.activation.domain.model.ActivationRequest;
import com.deroyque.activation.domain.model.ActivationStatus;
import com.deroyque.activation.infrastructure.web.dto.ActivationResponseDto;
import com.deroyque.activation.infrastructure.web.dto.CreateActivationRequestDto;
import com.deroyque.activation.infrastructure.web.dto.PaginatedResponseDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/requests")
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
    public ResponseEntity<ActivationResponseDto> getActivationRequestById(@PathVariable UUID id) {
        ActivationRequest request = getActivationUseCase.getById(id);
        return ResponseEntity.ok(ActivationResponseDto.fromDomain(request));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponseDto<ActivationResponseDto>> listActivationRequests(
            @RequestParam(required = false) String customerId,
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
