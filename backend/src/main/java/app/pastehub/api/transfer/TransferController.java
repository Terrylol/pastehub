package app.pastehub.api.transfer;

import java.time.Instant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferService transferService;

    TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("/text")
    public ResponseEntity<CreateTransferResponse> create(@Valid @RequestBody CreateTextTransferRequest request,
            @RequestHeader(value = HttpHeaders.ORIGIN, required = false) String origin) {
        TransferService.CreatedTransfer created = transferService.create(request.content());
        String pickupUrl = origin == null || origin.isBlank()
                ? ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/t/{id}").buildAndExpand(created.id()).toUriString()
                : origin.replaceAll("/+$", "") + "/t/" + created.id();
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(new CreateTransferResponse(created.id(), created.pickupCode(), pickupUrl, created.expiresAt(), created.deleteToken()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RetrieveTransferResponse> retrieve(@PathVariable String id) {
        TransferService.RetrievedTransfer transfer = transferService.retrieve(id);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore().cachePrivate())
                .body(new RetrieveTransferResponse(transfer.content(), transfer.expiresAt()));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ResolveCodeResponse> resolveCode(@PathVariable String code) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore().cachePrivate())
                .body(new ResolveCodeResponse(transferService.resolveCode(code)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        transferService.delete(id, bearerToken(authorization));
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) return null;
        return authorization.substring("Bearer ".length());
    }

    record CreateTextTransferRequest(@NotBlank @Size(max = 10_000) String content) { }
    record CreateTransferResponse(String id, String code, String pickupUrl, Instant expiresAt, String deleteToken) { }
    record RetrieveTransferResponse(String content, Instant expiresAt) { }
    record ResolveCodeResponse(String id) { }
}
