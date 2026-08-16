package app.pastehub.api.image;
import java.time.Instant; import jakarta.validation.Valid; import jakarta.validation.constraints.*; import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/transfers/image") public class ImageTransferController {
 final ImageTransferService service; ImageTransferController(ImageTransferService service){this.service=service;}
 @PostMapping("/init") ResponseEntity<?> init(@Valid @RequestBody InitRequest r){var x=service.init(r.mimeType(),r.sizeBytes());return ResponseEntity.status(201).body(new InitResponse(x.id(),x.uploadUrl(),x.uploadToken()));}
 @PostMapping("/complete") ResponseEntity<?> complete(@Valid @RequestBody CompleteRequest r,@RequestHeader(value="Origin",required=false) String origin){var x=service.complete(r.id(),r.uploadToken());String base=origin==null?"":origin.replaceAll("/+$","");return ResponseEntity.ok(new CreatedResponse(x.id(),x.code(),base+"/t/"+x.id(),x.expiresAt(),x.deleteToken()));}
 @GetMapping("/{id}") ResponseEntity<?> get(@PathVariable String id){var x=service.get(id);return ResponseEntity.ok(new ImageResponse(x.imageUrl(),x.expiresAt(),x.mimeType()));}
 @GetMapping("/code/{code}") ResponseEntity<?> code(@PathVariable String code){return ResponseEntity.ok(java.util.Map.of("id",service.code(code)));}
 @DeleteMapping("/{id}") ResponseEntity<Void> delete(@PathVariable String id,@RequestHeader(value="Authorization",required=false) String auth){service.delete(id,auth!=null&&auth.startsWith("Bearer ")?auth.substring(7):null);return ResponseEntity.noContent().build();}
 record InitRequest(@NotBlank String mimeType,@Min(1) @Max(10485760) long sizeBytes){} record CompleteRequest(@NotBlank String id,@NotBlank String uploadToken){} record InitResponse(String id,String uploadUrl,String uploadToken){} record CreatedResponse(String id,String code,String pickupUrl,Instant expiresAt,String deleteToken){} record ImageResponse(String imageUrl,Instant expiresAt,String mimeType){}
}
