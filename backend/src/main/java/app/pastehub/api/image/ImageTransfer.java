package app.pastehub.api.image;
import java.time.Instant;
import jakarta.persistence.*;
@Entity @Table(name = "image_transfers") class ImageTransfer {
 @Id String id; @Column(name="pickup_code",length=6,unique=true) String pickupCode; @Column(name="object_key",nullable=false,unique=true) String objectKey; @Column(name="mime_type",nullable=false) String mimeType; @Column(name="size_bytes",nullable=false) long sizeBytes; @Column(name="upload_token_hash",nullable=false) String uploadTokenHash; @Column(name="delete_token_hash") String deleteTokenHash; @Column(nullable=false) String state; @Column(name="created_at",nullable=false) Instant createdAt; @Column(name="expires_at",nullable=false) Instant expiresAt; @Column(name="deleted_at") Instant deletedAt;
 protected ImageTransfer() {} ImageTransfer(String id,String key,String mime,long size,String uploadHash,Instant now){this.id=id;objectKey=key;mimeType=mime;sizeBytes=size;uploadTokenHash=uploadHash;state="PENDING";createdAt=now;expiresAt=now.plusSeconds(300);}
 boolean live(Instant now){return "READY".equals(state)&&deletedAt==null&&expiresAt.isAfter(now);} void ready(String code,String hash,Instant now){pickupCode=code;deleteTokenHash=hash;state="READY";expiresAt=now.plusSeconds(600);} void deleted(Instant now){deletedAt=now;state="DELETED";}
}
